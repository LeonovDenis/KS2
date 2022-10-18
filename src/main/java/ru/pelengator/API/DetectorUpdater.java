package ru.pelengator.API;

import static ru.pelengator.API.DetectorExceptionHandler.handle;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.pelengator.API.tasks.DetectorGetImageTask;


/**
 * Паралельное обновление картинки.  Неблокирующий режим (Картинка немедленно вернется).
 */
public class DetectorUpdater implements Runnable {

    /**
     * Реализация этого интерфейса отвечает за расчет задержки между двумя запросами изображения.
     * Неблокирующий (асинхронный) доступ к детектору.
     */
    public static interface DelayCalculator {

        /**
         * Рассчитывает задержку перед получением следующего изображения с детектора.
         * Должно вернуться число больше или равно 0.
         *
         * @param snapshotDuration - продолжительность получения последнего изображения.
         * @param deviceFps        - текущий FPS, полученный от устройства, или -1, если драйвер не
         *                         поддерживает это.
         * @return интервал возврата (в миллисекундах).
         */
        long calculateDelay(long snapshotDuration, double deviceFps);
    }

    /**
     * Использование DelayCalculator по умолчанию на основе TARGET_FPS.
     * Возвращает 0 задержки для snapshotDuration &gt; 20 millis.
     */
    public static class DefaultDelayCalculator implements DelayCalculator {

        @Override
        public long calculateDelay(long snapshotDuration, double deviceFps) {
            // Рассчитаем задержку, необходимую для достижения целевого FPS.
            // В некоторых случаях может быть меньше 0,
            // потому что детектор не может показывать изображения так быстро, как
            // мы хотели бы. В таком случае просто используется без задержки,
            // поэтому будет поддерживаться максимальный FPS,
            // поддерживаемый в данный момент.


            long delay = Math.max((1000 / TARGET_FPS) - snapshotDuration, 0);
            return delay;
        }
    }

    /**
     * Фабрика потоков для исполнителей, используемых без класса оповещателя.
     */
    private static final class UpdaterThreadFactory implements ThreadFactory {

        private static final AtomicInteger number = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format("Detecter-updater-thread-%d", number.incrementAndGet()));
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }

    }

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorUpdater.class);

    /**
     * Целевой FPS.
     */
    private static final int TARGET_FPS = 25;
    /**
     * Фабрика.
     */
    private static final UpdaterThreadFactory THREAD_FACTORY = new UpdaterThreadFactory();

    /**
     * Сервис исполнителя.
     */
    private ScheduledExecutorService executor = null;

    /**
     * Кеширование изображение.
     */
    private final AtomicReference<BufferedImage> image = new AtomicReference<BufferedImage>();

    /**
     * Детектор, к которому привязан апдейтер.
     */
    private Detector detector = null;

    /**
     * Текущая частота кадров.
     */
    private volatile double fps = 0;

    /**
     * Работает ли программа обновления.
     */
    private AtomicBoolean running = new AtomicBoolean(false);
    /**
     * Флаг нового изображения.
     */
    private volatile boolean imageNew = false;

    /**
     * Реализация DelayCalculator.
     */
    private final DelayCalculator delayCalculator;

    /**
     * Создать новый апдейтера, используя DefaultDelayCalculator.
     *
     * @param detector детектор, к которому будет прикреплен апдейтер.
     */
    protected DetectorUpdater(Detector detector) {
        this(detector, new DefaultDelayCalculator());
    }

    /**
     * Создать новый апдейтер детектора.
     *
     * @param detector        детектор, к которому будет прикреплен апдейтер.
     * @param delayCalculator реализация.
     */
    public DetectorUpdater(Detector detector, DelayCalculator delayCalculator) {
        this.detector = detector;
        if (delayCalculator == null) {
            this.delayCalculator = new DefaultDelayCalculator();
        } else {
            this.delayCalculator = delayCalculator;
        }
    }

    /**
     * Старт апдейтера.
     */
    public void start() {

        if (running.compareAndSet(false, true)) {

            image.set(new DetectorGetImageTask(Detector.getDriver(), detector.getDevice()).getImage());

            executor = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
            executor.execute(this);

            LOG.debug("Detector updater has been started");
        } else {
            LOG.debug("Detector updater is already started");
        }
    }

    /**
     * Остановка апдейтера.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {

            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }
            }

            LOG.debug("Detector updater has been stopped");
        } else {
            LOG.debug("Detector updater is already stopped");
        }
    }

    @Override
    public void run() {

        if (!running.get()) {
            return;
        }

        try {
            tick();
        } catch (Throwable t) {
            handle(t);
        }
    }

    private void tick() {
        if (!detector.isOpen()) {
            return;
        }

        // Расчет времени, необходимого для получения 1 изображения.

        DetectorDriver driver = Detector.getDriver();
        DetectorDevice device = detector.getDevice();

        assert driver != null;
        assert device != null;

        boolean imageOk = false;
        long t1 = System.currentTimeMillis();
        try {
            image.set(detector.transform(new DetectorGetImageTask(driver, device).getImage()));
            imageNew = true;
            imageOk = true;
        } catch (DetectorException e) {
            handle(e);
        }
        long t2 = System.currentTimeMillis();

        double deviceFps = -1;
        if (device instanceof DetectorDevice.FPSSource) {
            deviceFps = ((DetectorDevice.FPSSource) device).getFPS();
        }

        long duration = t2 - t1;
        long delay = delayCalculator.calculateDelay(duration, deviceFps);

        long delta = duration + 1; // +1, чтобы избежать деления на ноль
        if (deviceFps >= 0) {
            fps = deviceFps;
        } else {
            fps = (4 * fps + 1000 / delta) / 5;
        }

        // перепланировать задачу

        if (detector.isOpen()) {
            try {

                executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                LOG.trace("Updater dalay {} ", delay);
            } catch (RejectedExecutionException e) {
                LOG.trace("Detector update has been rejected", e);
            }
        }

        // уведомляем слушателей детектора о новом доступном изображении

        if (imageOk) {
            detector.notifyDetectorImageAcquired(image.get());
        }
    }

    /**
     * Вернуть доступное в настоящее время изображение.
     * Этот метод вернется немедленно после открытия детектора.
     * В случае, когда запущены параллельные потоки и есть
     * есть возможность вызвать этот метод во время открытия, или до того, как детектор был открыт
     * этот метод будет блокироваться до тех пор, пока детектора не вернет первое изображение.
     * Максимальное время блокировки будет 10 секунд, по истечении этого времени метод вернет null.
     *
     * @return Изображение, которое хранится в кеше
     */
    public BufferedImage getImage() {
        int i = 0;
        while (image.get() == null) {

            // На тот случай, если другой поток начнет вызывать этот метод раньше, чем
            // программа обновления будет запущена правильно. Будет зацикливание, пока изображение
            // недоступно.

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Возвращаем null, если прошло более 10 секунд (тайм-аут).
            if (i++ > 100) {
                LOG.error("Image has not been found for more than 10 seconds");
                return null;
            }
        }

        imageNew = false;

        return image.get();
    }

    protected boolean isImageNew() {
        return imageNew;
    }

    /**
     * Вернуть текущий FPS.
     * Он рассчитывается в режиме реального времени на основе того, как часто детектор
     * выдает новое изображение.
     *
     * @return число кадров в секунду
     */
    public double getFPS() {
        return fps;
    }
}
