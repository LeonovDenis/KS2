package ru.pelengator.API;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jfree.data.json.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.tasks.*;

/**
 * Базовый класс детектора, полученного от драйвера
 */
public class Detector {

    /**
     * Класс для асинхронного уведомления всех слушателей о новой картинке.
     */
    private static final class ImageNotification implements Runnable {

        /**
         * Детектор.
         */
        private final Detector detector;

        /**
         * Полученная картинка.
         */
        private final BufferedImage image;

        /**
         * Уведомление.
         *
         * @param detector с которого получена картинка
         * @param image    полученная картинка
         */
        public ImageNotification(Detector detector, BufferedImage image) {
            this.detector = detector;
            this.image = image;
        }

        @Override
        public void run() {
            if (image != null) {
                DetectorEvent de = new DetectorEvent(DetectorEventType.NEW_IMAGE, detector, image);
                for (DetectorListener l : detector.getDetectorListeners()) {
                    try {
                        l.detectorImageObtained(de);
                    } catch (Exception e) {
                        LOG.error(String.format("Notify image acquired, exception when calling listener %s", l.getClass()), e);
                    }
                }
            }
        }
    }

    private final class NotificationThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format("Notificator-[%s]", getName()));
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Detector.class);

    /**
     * Список имен драйверов.
     */
    private static final List<String> DRIVERS_LIST = new ArrayList<String>();

    /**
     * Список драйверов.
     */
    private static final List<Class<?>> DRIVERS_CLASS_LIST = new ArrayList<Class<?>>();

    /**
     * Слушатели обнаружения.
     */
    private static final List<DetectorDiscoveryListener> DISCOVERY_LISTENERS = Collections.synchronizedList(new ArrayList<DetectorDiscoveryListener>());

    /**
     * Драйвер детектора.
     */
    private static volatile DetectorDriver driver = null;

    /**
     * Сервис обнаружения детектора.
     */
    private static volatile DetectorDiscoveryService discovery = null;

    /**
     * Должно ли быть вызвано удаление при получении сигнала TERM signal, если ShutdownHook включен.
     */
    private static boolean deallocOnTermSignal = false;

    /**
     * Автоматическое открытие включено?
     */
    private static boolean autoOpen = false;

    /**
     * Слушатели детектора.
     */
    private List<DetectorListener> listeners = new CopyOnWriteArrayList<DetectorListener>();

    /**
     * Список нестандартного разрешения, поддерживаемого детектором.
     */
    private List<Dimension> customSizes = new ArrayList<Dimension>();

    /**
     * Сигнал закрытия окна.
     */
    private DetectorShutdownHook hook = null;

    /**
     * Экземпляр детектора.
     */
    private DetectorDevice device = null;

    /**
     * Детектор открыт?
     */
    private AtomicBoolean open = new AtomicBoolean(false);

    /**
     * Детектор уже освобожден?
     */
    private AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Включен ли асинхронный  режим?
     */
    private volatile boolean asynchronous = false;

    /**
     * Текущий FPS
     */
    private volatile double fps = 0;

    /**
     * Асинхронный обновлятель изображения.
     */
    private volatile DetectorUpdater updater = null;

    /**
     * Трансформер изображения.
     */
    private volatile DetectorImageTransformer transformer = null;

    /**
     * Блокировка, которая запрещает доступ к детектору, когда он уже захвачен
     * процессом из API или потоком.
     */
    private DetectorLock lock = null;

    /**
     * Служба уведомления об изображении.
     */
    private ExecutorService notificator = null;

    /**
     * Конструктор.
     *
     * @param device устройство, которое будет использовано в качестве детектора
     * @throws IllegalArgumentException когда аргумент null
     */
    protected Detector(DetectorDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Detector device cannot be null");
        }
        this.device = device;
        this.lock = new DetectorLock(this);
    }

    /**
     * Асинхронно стартует новый поток для уведомления новых слушателей о новом изображении.
     */
    protected void notifyDetectorImageAcquired(BufferedImage image) {

        if (getDetectorListenersCount() > 0) {
            notificator.execute(new ImageNotification(this, image));
        }
    }

    /**
     * Открыть детектор в блокирующем (синхронном) режиме.
     *
     * @return True, если детектор открыт, иначе false
     * @throws DetectorException, если что-то пошло не так
     * @see #open(boolean, DetectorUpdater.DelayCalculator)
     */
    public boolean open() {
        return open(false);
    }

    /**
     * Открыть детектор в блокирующем (синхронном) или неблокирующем (асинхронном) режиме. Если
     * включен неблокирующий режим DefaultDelayCalculator используется для расчета задержки между
     * получение двух изображений.
     *
     * @param async true при неблокирующем режиме, false для блокирующего режима
     * @return True, если детектор открыт, иначе false
     * @throws DetectorException, если что-то пошло не так
     * @see #open(boolean, DetectorUpdater.DelayCalculator)
     */
    public boolean open(boolean async) {
        return open(async, new DetectorUpdater.DefaultDelayCalculator());
    }

    /**
     * Открыть детектор в блокирующем (синхронном) или неблокирующем (асинхронном) режиме.
     * Разница между этими двумя режимами заключается в механизме получения изображения.
     * <br>
     * <br>
     * В режиме блокировки, когда пользователь вызывает метод {@link  #getImage()}, устройство запрашивает новый
     * буфер изображения и пользователь должен ждать, пока он станет доступным.
     * <br>
     * <br>
     * В неблокирующем режиме в фоновом режиме работает специальный поток, который постоянно
     * получает новые изображения и кэширует их внутри для дальнейшего использования.
     * Этот кешированный экземпляр изображеения возвращается каждый раз, когда пользователь запрашивает
     * новое изображение. Из-за этого его можно использовать, когда время очень
     * важно, потому что всем пользователям, запрашивающим новое изображение, не нужно ждать ответа устройства.
     * При использовании данного режима пользователь должен учитывать тот факт, что в некоторых случаях
     * при двух последовательных запросах на получение нового изображения, запросы выполняются чаще,
     * чем их может обслужить детектор, будет возвращен то же самой Экземпляр изображения.
     * Пользователь должен использовать метод {@link #isImageNew()}, чтобы отличить возвращаемое изображение
     * от предыдущего.
     * <br>
     * <br>
     * Фоновый поток использует реализацию интерфейса DelayCalculator для расчета задержки
     * между двумя выборками изображений. Пользовательская реализация может быть указана как параметр этого
     * метода. Если включен неблокирующий режим и не указан DelayCalculator, то будет использоваться
     * DefaultDelayCalculator.
     *
     * @param async           true, для неблокирующего режима, false для блокирующего
     * @param delayCalculator отвечает за расчет задержки между получением двух изображений в
     *                        неблокирующий режим. В режиме блокировки игнорируется.
     * @return True, если детектор был открыт
     * @throws DetectorException, когда что-то пошло не так
     */

    public boolean open(boolean async, DetectorUpdater.DelayCalculator delayCalculator) {

        if (open.compareAndSet(false, true)) {

            assert lock != null;

            notificator = Executors.newSingleThreadExecutor(new NotificationThreadFactory());

            // блокируем детектор для других Java (only) processes

            lock.lock();

            // открываем детектор

            DetectorOpenTask task = new DetectorOpenTask(driver, device);
            try {
                task.open();
            } catch (InterruptedException e) {
                lock.unlock();
                open.set(false);
                LOG.debug("Thread has been interrupted in the middle of detector opening process!", e);
                return false;
            } catch (DetectorException e) {
                lock.unlock();
                open.set(false);
                LOG.debug("Detector exception when opening", e);
                throw e;
            }

            LOG.debug("Detector is now open {}", getName());

            // устанавливаем перехватчик выключения

            try {
                Runtime.getRuntime().addShutdownHook(hook = new DetectorShutdownHook(this));
            } catch (IllegalStateException e) {

                LOG.debug("Shutdown in progress, do not open device");
                LOG.trace(e.getMessage(), e);

                close();

                return false;
            }

            // устанавливаем неблокирующую конфигурацию

            if (asynchronous = async) {
                if (updater == null) {
                    updater = new DetectorUpdater(this, delayCalculator);
                }
                updater.start();
            }

            // уведомляем слушателей

            DetectorEvent de = new DetectorEvent(DetectorEventType.OPEN, this);
            Iterator<DetectorListener> dli = listeners.iterator();
            DetectorListener l = null;

            while (dli.hasNext()) {
                l = dli.next();
                try {
                    l.detectorOpen(de);
                } catch (Exception e) {
                    LOG.error(String.format("Notify detector open, exception when calling listener %s",
                            l.getClass()), e);
                }
            }

        } else {
            LOG.debug("detector is already open {}", getName());
        }

        return true;
    }

    /**
     * Закрытие детектора.
     *
     * @return True, если детектор был открыт, иначе false
     */
    public boolean close() {

        if (open.compareAndSet(true, false)) {

            LOG.debug("Closing detector {}", getName());

            assert lock != null;

            // close detector

            DetectorCloseTask task = new DetectorCloseTask(driver, device);
            try {
                task.close();
            } catch (InterruptedException e) {
                open.set(true);
                LOG.debug("Thread has been interrupted before detector was closed!", e);
                return false;
            } catch (DetectorException e) {
                open.set(true);
                throw e;
            }

            // остановка обновлятеля
            if (asynchronous) {
                updater.stop();
            }

            // убираем захватчика выключения
            removeShutdownHook();

            // разблокировать детектор, чтобы другие процессы Java могли начать его использовать
            lock.unlock();

            // уведомляем слушателей

            DetectorEvent de = new DetectorEvent(DetectorEventType.CLOSED, this);
            Iterator<DetectorListener> dli = listeners.iterator();
            DetectorListener l = null;

            while (dli.hasNext()) {
                l = dli.next();
                try {
                    l.detectorClosed(de);
                } catch (Exception e) {
                    LOG.error(String.format("Notify detector closed, exception when calling %s listener", l.getClass()), e);
                }
            }

            notificator.shutdown();
            while (!notificator.isTerminated()) {
                try {
                    notificator.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return false;
                }
            }

            LOG.debug("detector {} has been closed", getName());

        } else {
            LOG.debug("detector {} is already closed", getName());
        }

        return true;
    }

    /**
     * Возврат базового устройства детестора.
     * В зависимости от драйвера, используемого для обнаружения устройств, этот
     * метод может возвращать экземпляры другого класса.
     *
     * @return Базовый экземпляр устройства детектора
     */
    public DetectorDevice getDevice() {
        assert device != null;
        return device;
    }

    /**
     * Полностью освободить/удалить устройство.
     * После этой операции детектор больше не может использоваться и
     * требуется полная инициализация.
     */
    protected void dispose() {

        assert disposed != null;
        assert open != null;
        assert driver != null;
        assert device != null;
        assert listeners != null;

        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        open.set(false);

        LOG.info("Disposing detector {}", getName());

        DetectorDisposeTask task = new DetectorDisposeTask(driver, device);
        try {
            task.dispose();
        } catch (InterruptedException e) {
            LOG.error("Processor has been interrupted before detector was disposed!", e);
            return;
        }

        DetectorEvent de = new DetectorEvent(DetectorEventType.DISPOSED, this);
        Iterator<DetectorListener> dli = listeners.iterator();
        DetectorListener l = null;

        while (dli.hasNext()) {
            l = dli.next();
            try {
                l.detectorClosed(de);
                l.detectorDisposed(de);
            } catch (Exception e) {
                LOG.error(String.format("Notify detector disposed, exception when calling %s listener", l.getClass()), e);
            }
        }

        removeShutdownHook();

        LOG.debug("Detector disposed {}", getName());
    }

    private void removeShutdownHook() {

        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException e) {
                LOG.trace("Shutdown in progress, cannot remove hook");
            }
        }
    }

    /**
     * Изображение трансформируется с помощью преобразователя изображения.
     * Если преобразователь изображения не был установлен, этот метод
     * возвращает экземпляр, переданный в аргументе, без каких-либо модификаций.
     *
     * @param image изображение, которое нужно преобразовать
     * @return Преобразованное изображение (если установлено преобразование)
     */
    protected BufferedImage transform(BufferedImage image) {
        if (image != null) {
            DetectorImageTransformer tr = getImageTransformer();
            if (tr != null) {
                return tr.transform(image);
            }
        }
        return image;
    }

    /**
     * Детектор открыт?
     *
     * @return true, если открыт, иначе false
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Получить текущее разрешение детектора в пикселях.
     *
     * @return Разрешение детектора (размер изображения) в пикселях.
     */
    public Dimension getViewSize() {
        return device.getResolution();
    }

    /**
     * Вернуть список поддерживаемых разрешений.
     * Он может различаться в зависимости от источника данных.
     *
     * @return Массив поддерживаемых размеров
     */
    public Dimension[] getViewSizes() {
        return device.getResolutions();
    }

    /**
     * Установить пользовательское разрешение.
     * Если вы используете этот метод, убедитесь, что ваш детектор
     * может поддерживать это конкретное разрешение.
     *
     * @param sizes массив пользовательских разрешений, которые будут поддерживаться устройством.
     */
    public void setCustomViewSizes(Dimension... sizes) {
        assert customSizes != null;
        if (sizes == null) {
            customSizes.clear();
            return;
        }
        customSizes = Arrays.asList(sizes);
    }

    public Dimension[] getCustomViewSizes() {
        assert customSizes != null;
        return customSizes.toArray(new Dimension[customSizes.size()]);
    }

    /**
     * Установить новое разрешение.
     * Новое разрешение должно точно совпадать с размером по умолчанию или точно
     * такое же, как одно из пользовательских.
     *
     * @param size новое установленное разрешение.
     * @see Detector#setCustomViewSizes(Dimension[])
     * @see Detector#getViewSizes()
     */
    public void setViewSize(Dimension size) {

        if (size == null) {
            throw new IllegalArgumentException("Resolution cannot be null!");
        }

        if (open.get()) {
            throw new IllegalStateException("Cannot change resolution when detector is open, please close it first");
        }

        // проверяем отличается ли новое разрешение от старого

        Dimension current = getViewSize();
        if (current != null && current.width == size.width && current.height == size.height) {
            return;
        }

        // проверяем, допустимо ли новое разрешение

        Dimension[] predefined = getViewSizes();
        Dimension[] custom = getCustomViewSizes();

        assert predefined != null;
        assert custom != null;

        boolean ok = false;
        for (Dimension d : predefined) {
            if (d.width == size.width && d.height == size.height) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            for (Dimension d : custom) {
                if (d.width == size.width && d.height == size.height) {
                    ok = true;
                    break;
                }
            }
        }

        if (!ok) {
            StringBuilder sb = new StringBuilder("Incorrect dimension [");
            sb.append(size.width).append("x").append(size.height).append("] ");
            sb.append("possible ones are ");
            for (Dimension d : predefined) {
                sb.append("[").append(d.width).append("x").append(d.height).append("] ");
            }
            for (Dimension d : custom) {
                sb.append("[").append(d.width).append("x").append(d.height).append("] ");
            }
            throw new IllegalArgumentException(sb.toString());
        }

        LOG.debug("Setting new resolution {}x{}", size.width, size.height);

        device.setResolution(size);
    }

    /**
     * Захватить изображение с детектора и вернуть его.
     * Возвращает объект изображения или ноль, если детектор закрыт или уже погашена JVM.
     * <br>
     * <br>ВАЖНОЕ ПРИМЕЧАНИЕ!!!</b><br>
     * <br>
     * Есть два возможных поведения детектора, когда вы пытаетесь получить изображение и
     * детектор фактически закрыт. Обычно он возвращает null, но есть специальный флаг, который
     * можно статически настроить для переключения всех детекторов в режим автоматического открытия.
     * В этом режиме детектор будет автоматически открывается, когда вы пытаетесь получить
     * изображение с закрытого детектора.
     * <br>
     * <br>
     * Пожалуйста, обратите внимание на некоторые побочные эффекты!
     * В случае многопоточных приложений нет гарантии, что один поток
     * не будет пытаться открыть детектор, даже если он был закрыт вручную в другом потоке.
     *
     * @return Захваченное изображение или ноль, если детектор закрыт или погашена JVM
     */

    public BufferedImage getImage() {
        if (!isReady()) {
            return null;
        }

        long t1 = 0;
        long t2 = 0;

        if (asynchronous) {
            return updater.getImage();
        } else {

            // получаем изображение

            t1 = System.currentTimeMillis();
            BufferedImage image = transform(new DetectorGetImageTask(driver, device).getImage());
            t2 = System.currentTimeMillis();

            if (image == null) {
                return null;
            }

            // получаем фпс

            if (device instanceof DetectorDevice.FPSSource) {
                fps = ((DetectorDevice.FPSSource) device).getFPS();
            } else {
                // +1, чтобы избежать деления на ноль
                fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5;
            }

            // уведомляем слушателей детектора о новом доступном изображении

            notifyDetectorImageAcquired(image);

            return image;
        }
    }

    public boolean isImageNew() {
        if (asynchronous) {
            return updater.isImageNew();
        }
        return true;
    }

    public double getFPS() {
        if (asynchronous) {
            return updater.getFPS();
        } else {
            return fps;
        }
    }

    /**
     * Получить RAW (сырое) изображение ByteBuffer.
     * Он всегда будет возвращать буфер размером 3 x 1 байт на каждый пиксель, где
     * компоненты RGB включены (0, 1, 2) и цветовое пространство sRGB.
     * <br>
     * <br>
     * <b>ВАЖНО!</b>
     * <br>
     * <br>
     * Некоторые драйверы могут возвращать непосредственно ByteBuffer, поэтому нет гарантии,
     * что базовые байты не будут освобождены при следующей операции чтения.
     * Поэтому, чтобы избежать потенциальных ошибок, вы должны преобразовать этот ByteBuffer
     * в массив байтов, прежде чем получите следующее изображение.
     *
     * @return Байтовый буфер
     */
    public ByteBuffer getImageBytes() {

        if (!isReady()) {
            return null;
        }

        assert driver != null;
        assert device != null;

        long t1 = 0;
        long t2 = 0;

        // некоторые устройства могут поддерживать прямые буферы изображений, и для тех, которые вызываются
        // задачами процессора, а для тех, которые не поддерживают прямое изображение,
        // просто конвертируем изображение в байтовый массив RGB

        if (device instanceof DetectorDevice.BufferAccess) {
            t1 = System.currentTimeMillis();
            try {
                return new DetectorGetBufferTask(driver, device).getBuffer();
            } finally {
                t2 = System.currentTimeMillis();
                if (device instanceof DetectorDevice.FPSSource) {
                    fps = ((DetectorDevice.FPSSource) device).getFPS();
                } else {
                    fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5;
                }
            }
        } else {
            throw new IllegalStateException(String.format("Driver %s does not support buffer access",
                    driver.getClass().getName()));
        }
    }

    /**
     * Получить RAW изображение ByteBuffer.
     * Он всегда будет возвращать буфер размером 3 x 1 байт на каждый пиксель, где
     * компоненты RGB включены (0, 1, 2) и цветовое пространство sRGB.
     * <br>
     * <br>
     * <b>ВАЖНО!</b>
     * <br>
     * <br>
     * Некоторые драйверы могут возвращать непосредственно ByteBuffer, поэтому нет гарантии, что базовые байты
     * не будет освобождены при следующей операции чтения.
     * Поэтому, чтобы избежать потенциальных ошибок, вы должны преобразовать этот ByteBuffer в массив байтов,
     * прежде чем получите следующее изображение.
     *
     * @param target целевой объект {@link ByteBuffer} для копирования данных
     */

    public void getImageBytes(ByteBuffer target) {

        if (!isReady()) {
            return;
        }

        assert driver != null;
        assert device != null;

        long t1 = 0;
        long t2 = 0;

        // некоторые устройства могут поддерживать прямые буферы изображений, и для тех, которые вызываются
        // задачами процессора, а для тех, которые не поддерживают прямое изображение,
        // просто конвертируем изображение в байтовый массив RGB

        if (device instanceof DetectorDevice.BufferAccess) {
            t1 = System.currentTimeMillis();
            try {
                new DetectorReadBufferTask(driver, device, target).readBuffer();
            } finally {
                t2 = System.currentTimeMillis();
                if (device instanceof DetectorDevice.FPSSource) {
                    fps = ((DetectorDevice.FPSSource) device).getFPS();
                } else {
                    fps = (4 * fps + 1000 / (t2 - t1 + 1)) / 5;
                }
            }
        } else {
            throw new IllegalStateException(String.format("Driver %s does not support buffer access",
                    driver.getClass().getName()));
        }
    }

    /**
     * Если базовое устройство реализует настраиваемый интерфейс,то ему передаются указанные параметры.
     * Может вызываться до метода open или позже, в зависимости от устройства.
     *
     * @param parameters Карта параметров, изменяющих настройки устройства по умолчанию
     * @см. Configurable
     */

    public void setParameters(Map<String, ?> parameters) {
        DetectorDevice device = getDevice();
        if (device instanceof DetectorDevice.Configurable) {
            ((DetectorDevice.Configurable) device).setParameters(parameters);
        } else {
            LOG.debug(" device {} is not configurable", device);
        }
    }

    /**
     * Готов ли детектор для чтения.
     *
     * @return True, если готов, иначе false
     */
    private boolean isReady() {

        assert disposed != null;
        assert open != null;

        if (disposed.get()) {
            LOG.warn("Cannot get image, detector has been already disposed");
            return false;
        }

        if (!open.get()) {
            if (autoOpen) {
                open();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Получить список детекторов для использования.
     * Этот метод будет ожидать предопределенный интервал времени для обнаружения детектора.
     * По умолчанию это время установлено на 1 минуту.
     *
     * @return Список детекторов, существующих в системе
     * @throws DetectorException, когда что-то не так
     * @see Detector#getDetectors(long, TimeUnit)
     */
    public static List<Detector> getDetectors() throws DetectorException {

        // Исключение тайм-аута ниже никогда не будет перехвачено, так как пользователю придется
        // ждать около трехсот миллиардов лет, пока это произойдет

        try {
            return getDetectors(Long.MAX_VALUE);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получить список детекторов для использования.
     * Этот метод будет ждать заданный интервал времени для устройств, чтобы они были обнаружены.
     * Аргумент времени задается в миллисекундах.
     *
     * @param timeout время ожидания обнаружения устройств
     * @return Список устройств, существующих в системе
     * @throws TimeoutException,         когда происходит тайм-аут
     * @throws DetectorException,        когда что-то не так
     * @throws IllegalArgumentException, когда время ожидания отрицательное
     * @see Detector#getDetectors(long, TimeUnit)
     */
    public static List<Detector> getDetectors(long timeout) throws TimeoutException, DetectorException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        return getDetectors(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Получить список устройств для использования.
     * Этот метод будет ждать заданный интервал времени для устройств, чтобы их обнаружить.
     *
     * @param timeout тайм-аут обнаружения устройств
     * @param tunit   единица времени
     * @return Список устройств
     * @throws TimeoutException          при превышении времени ожидания
     * @throws DetectorException,        когда что-то не так
     * @throws IllegalArgumentException, когда время ожидания отрицательное или tunit null
     */
    public static synchronized List<Detector> getDetectors(long timeout, TimeUnit tunit) throws TimeoutException, DetectorException {

        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        DetectorDiscoveryService discovery = getDiscoveryService();

        assert discovery != null;

        List<Detector> detectors = discovery.getDetectors(timeout, tunit);
        if (!discovery.isRunning()) {
            discovery.start();
        }

        return detectors;
    }

    /**
     * Обнаружит и вернет первый детектор, доступный в системе.
     *
     * @return Детектор по умолчанию (первый в списке)
     * @throws DetectorException, если что-то действительно не так
     * @see Detector#getDetectors()
     */
    public static Detector getDefault() throws DetectorException {

        try {
            return getDefault(Long.MAX_VALUE);
        } catch (TimeoutException e) {
            // этого никогда не должно происходить, так как пользователю придется ждать 300000000
            // лет, чтобы это произошло
            throw new RuntimeException(e);
        }
    }

    /**
     * Обнаружит и вернет первый детектор, доступный в системе.
     *
     * @param timeout тайм-аут обнаружения (по умолчанию 1 минута)
     * @return Детектор по умолчанию (первый в списке)
     * @throws TimeoutException,         когда время ожидания обнаружения превышено
     * @throws DetectorException,        если что-то действительно не так
     * @throws IllegalArgumentException, когда время ожидания отрицательное
     * @see Detector#getDetectors(long)
     */
    public static Detector getDefault(long timeout) throws TimeoutException, DetectorException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        return getDefault(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Обнаружит и вернет первый детектор, доступный в системе.
     *
     * @param timeout the тайм-аут обнаружения (по умолчанию 1 минута)
     * @param tunit   единица времени
     * @return Детектор по умолчанию (первый в списке)
     * @throws TimeoutException,         когда время ожидания обнаружения превышено
     * @throws DetectorException,        если что-то действительно не так
     * @throws IllegalArgumentException, когда время ожидания отрицательное или tunit null
     * @see Detector#getDetectors(long, TimeUnit)
     */
    public static Detector getDefault(long timeout, TimeUnit tunit) throws TimeoutException, DetectorException {

        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        List<Detector> detectors = getDetectors(timeout, tunit);

        assert detectors != null;

        if (!detectors.isEmpty()) {
            return detectors.get(0);
        }

        LOG.warn("No detectors has been detected!");

        return null;
    }

    /**
     * Получить имя устройства.
     *
     * @return Name
     */
    public String getName() {
        assert device != null;
        return device.getName();
    }

    @Override
    public String toString() {
        return String.format("Detector %s", getName());
    }

    /**
     * Добавить прослушиватель детектора.
     *
     * @param l добавляемый слушатель
     * @return True, если слушатель был добавлен, false, если он уже там был
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public boolean addDetectorListener(DetectorListener l) {
        if (l == null) {
            throw new IllegalArgumentException("Detector listener cannot be null!");
        }
        assert listeners != null;
        return listeners.add(l);
    }

    /**
     * @return Все слушатели детектора
     */
    public DetectorListener[] getDetectorListeners() {
        assert listeners != null;
        return listeners.toArray(new DetectorListener[listeners.size()]);
    }

    /**
     * @return Количество слушателей детектора
     */
    public int getDetectorListenersCount() {
        assert listeners != null;
        return listeners.size();
    }

    /**
     * Удаляет прослушивателя детектора.
     *
     * @param l прослушиватель, которого нужно удалить
     * @return True, если слушатель был удален, иначе false
     */
    public boolean removeDetectorListener(DetectorListener l) {
        assert listeners != null;
        return listeners.remove(l);
    }

    /**
     * Возврат драйвера детектора.
     * При необходимости выполните поиск.
     * <br>
     * <br>
     * <b>Этот метод не является потокобезопасным!</b>
     *
     * @return драйвер устройства
     */
    public static synchronized DetectorDriver getDriver() {

        if (driver != null) {
            return driver;
        }

        if (driver == null) {
            driver = DetectorDriverUtils.findDriver(DRIVERS_LIST, DRIVERS_CLASS_LIST);
        }

        LOG.info("{} capture driver will be used", driver.getClass().getSimpleName());

        return driver;
    }

    /**
     * Установить новый драйвер для использования устройством.
     * <br>
     * <br>
     * <b>Этот метод не является потокобезопасным!</b>
     *
     * @param dd будет использоваться новый драйвер устройства
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public static void setDriver(DetectorDriver dd) {

        if (dd == null) {
            throw new IllegalArgumentException("Detector driver cannot be null!");
        }

        LOG.debug("Setting new capture driver {}", dd);

        resetDriver();

        driver = dd;
    }

    /**
     * Установить новый класс драйвера для использования устойством.
     * Класс, указанный в аргументе, должен расширять  интерфейс {@link DetectorDriver}
     * и должен иметь общедоступный конструктор по умолчанию, поэтому экземпляр может быть
     * создан отражением.
     * <br>
     * <br>
     * <b>Этот метод не является потокобезопасным!</b>
     *
     * @param driverClass новый класс драйвера для использования
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public static void setDriver(Class<? extends DetectorDriver> driverClass) {

        if (driverClass == null) {
            throw new IllegalArgumentException("Detector driver class cannot be null!");
        }

        resetDriver();

        try {
            driver = driverClass.newInstance();
        } catch (InstantiationException e) {
            throw new DetectorException(e);
        } catch (IllegalAccessException e) {
            throw new DetectorException(e);
        }
    }

    /**
     * Сброс драйвера.
     * <br>
     * <br>
     * <b>Этот метод не является потокобезопасным!</b>
     */
    public static void resetDriver() {

        synchronized (DRIVERS_LIST) {
            DRIVERS_LIST.clear();
        }

        if (discovery != null) {
            discovery.shutdown();
            discovery = null;
        }

        driver = null;
    }

    /**
     * Зарегистрировать новый драйвер.
     *
     * @param clazz драйвер class
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public static void registerDriver(Class<? extends DetectorDriver> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Detector driver class to register cannot be null!");
        }
        DRIVERS_CLASS_LIST.add(clazz);
        registerDriver(clazz.getCanonicalName());
    }

    /**
     * Зарегистрировать новый драйвер.
     *
     * @param clazzName имя класса драйвера
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public static void registerDriver(String clazzName) {
        if (clazzName == null) {
            throw new IllegalArgumentException("Detector driver class name to register cannot be null!");
        }
        DRIVERS_LIST.add(clazzName);
    }

    /**
     * <b>ВНИМАНИЕ!!!</b>
     * <br>
     * <br>
     * Это экспериментальная функция, которая будет использоваться в основном на этапе разработки.
     * После того, как вы установите дескриптор сигнала TERM в значение true и получите устройство захвата,
     * API будет прослушивать TERM сигнал и попытаться закрыть все устройства после его получения.
     * <br>
     * <br>
     * <b>Эту функция может работать нестабильно на некоторых системах!</b>
     *
     * @param on обработке сигналов будет включен, если true, иначе отключен
     */
    public static void setHandleTermSignal(boolean on) {
        if (on) {
            LOG.warn("Automated deallocation on TERM signal is now enabled! Make sure to not use it in production!");
        }
        deallocOnTermSignal = on;
    }

    /**
     * Включен ли обработчик сигнала TERM.
     *
     * @return True, если включено, иначе false
     */
    public static boolean isHandleTermSignal() {
        return deallocOnTermSignal;
    }

    /**
     * Переключить все детекторы в режим автоматического открытия.
     * В этом режиме каждый детектор будет автоматически открыт
     * всякий раз, когда пользователь попытается получить изображение из экземпляра, который еще не был открыт.
     * Пожалуйста, не забудьте о некоторых побочных эффектах! В случае многопоточных приложений гарантии нет
     * что один поток не будет пытаться открыть устройство, даже если оно была закрыта вручную в разных
     * потоков.
     *
     * @param on true, чтобы включить, false, чтобы отключить
     */
    public static void setAutoOpenMode(boolean on) {
        autoOpen = on;
    }

    /**
     * Включен ли режим автоматического открытия.
     * Режим автоматического открытия будет автоматически открывать детектор всякий раз, когда пользователь
     * попытается получить изображение из экземпляра, который еще не был открыт.
     * Пожалуйста, имейте в виду некоторые побочные последствия!
     * В случае многопоточных приложений нет гарантии, что один поток не будет
     * пытайтесь открыть устройство, даже если оно было закрыто вручную в другом потоке.
     *
     * @return True, если режим включен, иначе false
     */
    public static boolean isAutoOpenMode() {
        return autoOpen;
    }

    /**
     * Добавление нового слушателя обнаружения детектора.
     *
     * @param l добавляемый слушатель
     * @return True, если размер списка слушателей был изменен, иначе false
     * @throws IllegalArgumentException, когда аргумент равен нулю
     */
    public static boolean addDiscoveryListener(DetectorDiscoveryListener l) {
        if (l == null) {
            throw new IllegalArgumentException("Detector discovery listener cannot be null!");
        }
        return DISCOVERY_LISTENERS.add(l);
    }

    /**
     * Получение списка слушателей.
     *
     * @return Список зарегистрированныхслушателей.
     */
    public static DetectorDiscoveryListener[] getDiscoveryListeners() {
        return DISCOVERY_LISTENERS.toArray(new DetectorDiscoveryListener[DISCOVERY_LISTENERS.size()]);
    }

    /**
     * Удалить прослушиватель обнаружения.
     *
     * @param l прослушиватель, которого нужно удалить
     * @return True, если список слушателей содержит указанный элемент
     */
    public static boolean removeDiscoveryListener(DetectorDiscoveryListener l) {
        return DISCOVERY_LISTENERS.remove(l);
    }

    /**
     * Получение сервиса обнаружения
     *
     * @return Служба обнаружения
     */
    public static synchronized DetectorDiscoveryService getDiscoveryService() {
        if (discovery == null) {
            discovery = new DetectorDiscoveryService(getDriver());
        }
        return discovery;
    }

    /**
     * Возврат службы обнаружения без ее создания, если она не существует.
     * <p>
     *
     * @return Служба обнаружения или нуль, если она еще не создана
     */
    public static synchronized DetectorDiscoveryService getDiscoveryServiceRef() {
        return discovery;
    }

    /**
     * Возврат трансформера изображения.
     *
     * @return Экземпляр трансформера
     */
    public DetectorImageTransformer getImageTransformer() {
        return transformer;
    }

    /**
     * Установка трансформера изображений.
     *
     * @param transformer трансформер, который нужно установить
     */
    public void setImageTransformer(DetectorImageTransformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Возврат блокировщика детектора.
     *
     * @return Блокировщик детектора
     */
    public DetectorLock getLock() {
        return lock;
    }

    /**
     * Выключение скелета детектора. Этот метод следует использовать <b>ТОЛЬКО</b> при выходе из JVM,
     * но, пожалуйста, <b>не вызывайте его</b>, если вам это действительно не нужно.
     */
    protected static void shutdown() {

        // остановить службу обнаружения
        DetectorDiscoveryService discovery = getDiscoveryServiceRef();
        if (discovery != null) {
            discovery.stop();
        }

        // остановить процессор
        DetectorProcessor.getInstance().shutdown();
    }

    /**
     * Везвращает детектор с заданным именем или null, если устройство с заданным именем не найдено.
     *
     * @param name имя детектора
     * @return детектор с заданным именем или null, если не найден
     * @throws IllegalArgumentException, когда имя равно null
     */
    public static Detector getDetectorByName(String name) {

        if (name == null) {
            throw new IllegalArgumentException("Detector name cannot be null");
        }

        for (Detector detector : getDetectors()) {
            if (detector.getName().equals(name)) {
                return detector;
            }
        }

        return null;
    }

}
