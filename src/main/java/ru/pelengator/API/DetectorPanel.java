package ru.pelengator.API;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.transformer.comFilters.JHFilter;
import ru.pelengator.API.transformer.comFilters.JHFlipFilter;
import ru.pelengator.Controller;

import static java.awt.RenderingHints.*;
import static ru.pelengator.API.utils.Utils.*;


/**
 * Простая реализация JPanel, позволяющая пользователям отображать изображения
 */
public class DetectorPanel extends JPanel implements DetectorListener {


    /**
     * Это перечисление предназначено для управления тем, как изображение будет отображаться в границах панели.
     */
    public enum DrawMode {

        /**
         * Не изменяет размер изображения - рисует как есть.
         * Изображение исчезнет, если панель меньше размера изображения.
         */
        NONE,

        /**
         * Изменит размер изображения до границ панели.
         * Этот режим не заботится о масштабе изображения, поэтому
         * конечное изображение может быть нарушено.
         */
        FILL,

        /**
         * Будет помещать изображение в границы панели.
         * Это изменит размер изображения и сохранит масштаб как по x, так и по y.
         */
        FIT,
    }

    /**
     * Библиотека для разворота изображения.
     */
    private static JHFlipFilter flipper = null;
    /**
     * Фильтр обработки изображения
     */
    private static JHFilter filter = null;
    /**
     * Фильтр нормалайзер
     */
    private static JHFilter normalayzer = null;

    /**
     * Этот интерфейс можно использовать для передачи {@link BufferedImage} на {@link DetectorPanel}.
     */
    public interface ImageSupplier {

        /**
         * @return {@link BufferedImage} для отображения на {@link DetectorPanel}
         */
        public BufferedImage get();
    }

    /**
     * Реализация {@link ImageSupplier} по умолчанию, используемая в {@link DetectorPanel}.
     * Он вызывает {@link Detector#getImage()} и возвращает {@link BufferedImage}.
     */
    private static class DefaultImageSupplier implements ImageSupplier {

        private final Detector detector;

        public DefaultImageSupplier(Detector detector) {
            this.detector = detector;
        }

        @Override
        public BufferedImage get() {

            try {
                TimeUnit.MILLISECONDS.sleep(PAUSE);
            } catch (InterruptedException e) {
                //ignore
           }
            BufferedImage tempImage = detector.getImage();

            if (tempImage != null) {

                if (flipper != null) {
                    tempImage = flipper.filter(tempImage, null);
                }

                if (filter != null) {
                    tempImage = filter.filter(tempImage, null);
                }
                if (normalayzer != null) {
                    normalayzer.filter(tempImage, tempImage);
                }
            }
            return tempImage;
        }

    }

    /**
     * Интерфейс отрисовщика, используемый для рисования изображения на панели.
     */
    public interface Painter {

        /**
         * Отрисовка панели без изображения.
         *
         * @param panel панель для рисования
         * @param g2    графический 2D-объект, используемый для рисования
         */
        void paintPanel(DetectorPanel panel, Graphics2D g2);

        /**
         * Отрисовка изображения на панели.
         *
         * @param panel панель для рисования
         * @param image изображение
         * @param g2    графический 2D-объект, используемый для рисования
         */
        void paintImage(DetectorPanel panel, BufferedImage image, Graphics2D g2);
    }

    /**
     * Отрисовщик по умолчанию, используемый для рисования изображения на панели.
     */
    public class DefaultPainter implements Painter {

        /**
         * Имя устройства.
         */
        private String name = null;

        /**
         * Время перерисовки, используется для отладки.
         */
        private long lastRepaintTime = -1;

        /**
         * Размер буферизованного изображения изменен, чтобы соответствовать области рисования панели.
         */
        private BufferedImage resizedImage = null;

        @Override
        public void paintPanel(DetectorPanel owner, Graphics2D g2) {

            assert owner != null;
            assert g2 != null;

            Object antialiasing = g2.getRenderingHint(KEY_ANTIALIASING);

            g2.setRenderingHint(KEY_ANTIALIASING, isAntialiasingEnabled() ? VALUE_ANTIALIAS_ON : VALUE_ANTIALIAS_OFF);
            g2.setBackground(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cx = (getWidth() - 70) / 2;
            int cy = (getHeight() - 40) / 2;

            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRoundRect(cx, cy, 70, 40, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(cx + 5, cy + 5, 30, 30);
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillOval(cx + 10, cy + 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.fillOval(cx + 12, cy + 12, 16, 16);
            g2.fillRoundRect(cx + 50, cy + 5, 15, 10, 5, 5);
            g2.fillRect(cx + 63, cy + 25, 7, 2);
            g2.fillRect(cx + 63, cy + 28, 7, 2);
            g2.fillRect(cx + 63, cy + 31, 7, 2);

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(0, 0, getWidth(), getHeight());
            g2.drawLine(0, getHeight(), getWidth(), 0);

            String str;

            final String strInitDevice = "Инициализация устройства";
            final String strNoImage = "Изображение не доступно!";
            final String strDeviceError = "Ошибка устройства";

            if (errored) {
                str = strDeviceError;
            } else {
                str = starting ? strInitDevice : strNoImage;
            }

            FontMetrics metrics = g2.getFontMetrics(getFont());
            int w = metrics.stringWidth(str);
            int h = metrics.getHeight();

            int x = (getWidth() - w) / 2;
            int y = cy - h;

            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            g2.drawString(str, x, y);

            if (name == null) {
                name = detector.getName();
            }

            str = name;

            w = metrics.stringWidth(str);
            h = metrics.getHeight();

            g2.drawString(str, (getWidth() - w) / 2, cy - 2 * h);
            g2.setRenderingHint(KEY_ANTIALIASING, antialiasing);
        }

        @Override
        public void paintImage(DetectorPanel owner, BufferedImage image, Graphics2D g2) {

            assert owner != null;
            assert image != null;
            assert g2 != null;

            int pw = getWidth();
            int ph = getHeight();
            int iw = image.getWidth();
            int ih = image.getHeight();

            Object antialiasing = g2.getRenderingHint(KEY_ANTIALIASING);
            Object rendering = g2.getRenderingHint(KEY_RENDERING);

            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_SPEED);
            g2.setBackground(Color.WHITE);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, pw, ph);

            // resized image position and size
            int x = 0;
            int y = 0;
            int w = 0;
            int h = 0;
            double s = 0;
            switch (drawMode) {
                case NONE:
                    w = image.getWidth();
                    h = image.getHeight();
                    break;
                case FILL:
                    w = pw;
                    h = ph;
                    break;
                case FIT:
                    s = Math.max((double) iw / pw, (double) ih / ph);
                    double niw = iw / s;
                    double nih = ih / s;
                    double dx = (pw - niw) / 2;
                    double dy = (ph - nih) / 2;
                    w = (int) niw;
                    h = (int) nih;
                    x = (int) dx;
                    y = (int) dy;
                    break;
            }

            if (resizedImage != null) {
                resizedImage.flush();
            }

            if (w == image.getWidth() && h == image.getHeight() && !mirrored) {
                resizedImage = image;
            } else {

                GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsConfiguration gc = genv.getDefaultScreenDevice().getDefaultConfiguration();

                Graphics2D gr = null;
                try {

                    resizedImage = gc.createCompatibleImage(pw, ph);
                    gr = resizedImage.createGraphics();
                    gr.setComposite(AlphaComposite.Src);

                    for (Map.Entry<RenderingHints.Key, Object> hint : imageRenderingHints.entrySet()) {
                        gr.setRenderingHint(hint.getKey(), hint.getValue());
                    }

                    gr.setBackground(Color.WHITE);
                    gr.setColor(new Color(0xcccccc));
                    gr.fillRect(0, 0, pw, ph);

                    int sx1, sx2, sy1, sy2; // source rectangle coordinates
                    int dx1, dx2, dy1, dy2; // destination rectangle coordinates

                    dx1 = x;
                    dy1 = y;
                    dx2 = x + w;
                    dy2 = y + h;

                    if (mirrored) {
                        sx1 = iw;
                        sy1 = 0;
                        sx2 = 0;
                        sy2 = ih;
                    } else {
                        sx1 = 0;
                        sy1 = 0;
                        sx2 = iw;
                        sy2 = ih;

                    }
                    gr.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

                } finally {
                    if (gr != null) {
                        gr.dispose();
                    }
                }
            }

            g2.drawImage(resizedImage, 0, 0, null);

            if (isFPSDisplayed()) {

                String str = String.format("DEBUG: frames per second: %.1f", detector.getFPS());

                int sx = 5;
                int sy = ph - 5;

                g2.setFont(getFont());
                g2.setColor(Color.BLACK);
                g2.drawString(str, sx + 1, sy + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(str, sx, sy);

            }

            if (isImageSizeDisplayed()) {

                String res = String.format("DEBUG: %d\u2A2F%d px", iw, ih);

                FontMetrics metrics = g2.getFontMetrics(getFont());
                int sw = metrics.stringWidth(res);
                int sx = pw - sw - 5;
                int sy = ph - 5;

                g2.setFont(getFont());
                g2.setColor(Color.BLACK);
                g2.drawString(res, sx + 1, sy + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(res, sx, sy);
            }
            /**
             * Открисовка квадрата
             */
            if (isAimDisplayed() && s != 0) {
                drawRect(g2, iw, ih, aimWidth, aimHeight, x, y, s);
            }

            if (isDisplayDebugInfo()) {

                if (lastRepaintTime < 0) {
                    lastRepaintTime = System.currentTimeMillis();
                } else {

                    long now = System.currentTimeMillis();
                    String res = String.format("DEBUG: repaints per second: %.1f",
                            (double) 1000 / (now - lastRepaintTime));
                    lastRepaintTime = now;
                    g2.setFont(getFont());
                    g2.setColor(Color.BLACK);
                    g2.drawString(res, 6, 16);
                    g2.setColor(Color.WHITE);
                    g2.drawString(res, 5, 15);
                }
            }

            g2.setRenderingHint(KEY_ANTIALIASING, antialiasing);
            g2.setRenderingHint(KEY_RENDERING, rendering);
        }
    }

    private static final class PanelThreadFactory implements ThreadFactory {

        private static final AtomicInteger number = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format("Detector-panel-scheduled-executor-%d",
                    number.incrementAndGet()));
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Отрисовщик библиотеки Swing. Только перерисовывает панель.
     */
    private static final class SwingRepainter implements Runnable {

        private DetectorPanel panel = null;

        public SwingRepainter(DetectorPanel panel) {
            this.panel = panel;
        }

        @Override
        public void run() {
            panel.repaint();
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorPanel.class);

    /**
     * Минимальная частота кадров в секунду.
     */
    public static final double MIN_FREQUENCY = 0.016; // 1 кадр в минуту

    /**
     * Максимальная частота кадров в секунду.
     */
    private static final double MAX_FREQUENCY = 25; // 25 кадров в минуту

    /**
     * Фабрика потоков, используемая службой выполнения.
     */
    private static final ThreadFactory THREAD_FACTORY = new PanelThreadFactory();

    public static final Map<RenderingHints.Key, Object> DEFAULT_IMAGE_RENDERING_HINTS = new HashMap<RenderingHints.Key, Object>();

    static {
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_RENDERING, VALUE_RENDER_SPEED);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
    }

    /**
     * Задание на перерисовку.
     */
    private final Runnable repaint = new SwingRepainter(this);

    /**
     * Подсказки рендеринга, которые будут использоваться при рисовании отображаемого изображения.
     */
    private Map<RenderingHints.Key, Object> imageRenderingHints = new HashMap<RenderingHints.Key, Object>(DEFAULT_IMAGE_RENDERING_HINTS);

    /**
     * Запланированный исполнитель, действующий как таймер.
     */
    private ScheduledExecutorService executor = null;

    /**
     * Задание на обновление изображений. Считывает изображение с устройства и принудительно перекрашивает панель.
     */
    private class ImageUpdater implements Runnable {

        /**
         * Задание на перерисовку панели.
         */
        private class RepaintScheduler extends Thread {

            /**
             * Фабрика в конструкторе.
             */
            public RepaintScheduler() {
                setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
                setName(String.format("Repaint-scheduler-%s", detector.getName()));
                setDaemon(true);
            }

            @Override
            public void run() {

                // ничего не делать, когда устройство не работает
                if (!running.get()) {
                    return;
                }

                repaintPanel();//перерисовка панели Swing

                // цикл при запуске устройства для ожидания поступления изображений
                while (starting) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                // запланировать обновление, когда детектор открыт, в противном случае запланировать
                // второе выполнение планировщика

                try {

                    // Ограничение FPS означает, что частота рендеринга панели
                    // ограничена конкретным значением и панель не будет
                    // отображается чаще, чем конкретное значение

                    if (detector.isOpen()) {

                        if (isFPSLimited()) {
                            executor.scheduleAtFixedRate(updater, 0, (long) (1000 / frequency), TimeUnit.MILLISECONDS);
                        } else {
                            executor.scheduleWithFixedDelay(updater, 100, 1, TimeUnit.MILLISECONDS);
                        }
                    } else {
                        executor.schedule(this, 500, TimeUnit.MILLISECONDS);
                    }
                } catch (RejectedExecutionException e) {

                    // экзекьютор остановлен, значит кто-то
                    // остановил панель/устройство  до того, как оно было на самом деле
                    // полностью запущено (был в "стартовом" таймфрейме)

                    LOG.warn("Executor rejected paint update");
                    LOG.trace("Executor rejected paint update because of", e);
                }
            }
        }

        /**
         * Обновление потока планировщика.
         */
        private Thread scheduler = null;

        /**
         * Работает ли перекрашиватель?
         */
        private AtomicBoolean running = new AtomicBoolean(false);

        /**
         * Запустить перерисовку.
         * Может вызываться много раз, но только первый вызов вступит в силу.
         */
        public void start() {
            if (running.compareAndSet(false, true)) {
                executor = Executors.newScheduledThreadPool(1, THREAD_FACTORY);
                scheduler = new RepaintScheduler();
                scheduler.start();
            }
        }

        /**
         * Остановить перерисовку. Может вызываться много раз, но только первый вызов вступит в силу.
         *
         * @throws InterruptedException
         */
        public void stop() throws InterruptedException {
            if (running.compareAndSet(true, false)) {
                executor.shutdown();
                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                scheduler.join();
            }
        }

        @Override
        public void run() {
            try {
                update();
            } catch (Throwable t) {
                errored = true;
                DetectorExceptionHandler.handle(t);
            }
        }

        /**
         * Выполнить обновление области одной панели (перерисовать вновь полученное изображение).
         */
        private void update() {

            // ничего не делать, когда или:
            // - программа обновления не запущена;
            // - детектор закрыт;
            // - перерисовка панели приостановлена.

            if (!running.get() || !detector.isOpen() || paused) {
                return;
            }

            // получаем новое изображение

            BufferedImage tmp = supplier.get();//запрос в детектор

            boolean repaint = true;

            if (tmp != null) { //если есть изображение

                tmp = copyImage(tmp);

                tmp = transform(tmp);

                // игнорировать перерисовку, если изображение такое же, как и предыдущее
                if (image == tmp) {
                    repaint = false;
                }
                errored = false;
                image = tmp;


            } else {
            }
            if (repaint) {
                repaintPanel();
            }
        }
    }
    /**
     * Режим того, как изображение будет изменено, чтобы соответствовать границам панели.
     * По умолчанию {@link DrawMode#FIT}
     *
     * @see DrawMode
     */
    private DrawMode drawMode = DrawMode.FIT;

    /**
     * Частота запроса кадров.
     */
    private double frequency = 25; // FPS

    /**
     * Ограничена ли частота запроса кадров?
     * Если true, изображения будут загружаться в заданные интервалы времени.
     * Если false, изображения будут загружаться так быстро, как только детектор сможет их обслужить.
     */
    private boolean frequencyLimit = false;

    /**
     * Отображение FPS.
     */
    private boolean frequencyDisplayed = false;

    /**
     * Отображение размера изображения.
     */
    private boolean imageSizeDisplayed = false;

    /**
     * Включено ли сглаживание (true по умолчанию).
     */
    private boolean antialiasingEnabled = true;

    /**
     * Объект детектора, используемый для получения изображений.
     */
    private final Detector detector;
    /**
     * Поставщик изображений.
     */
    private final ImageSupplier supplier;

    /**
     * Апдейтер изображения.
     */
    private final ImageUpdater updater;

    /**
     * Изображение, которое отображается в данный момент.
     */
    private BufferedImage image = null;

    /**
     * Флаг запуска устройства.
     */
    private volatile boolean starting = false;

    /**
     * Флаг приостановки перерисовки.
     * Новые изображения не запрашиваются.
     */
    private volatile boolean paused = false;

    /**
     * Флаг наличия ошибки.
     */
    private volatile boolean errored = false;

    /**
     * Флаг работы детектора.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Отрисовщик по умолчанию.
     */
    private final Painter defaultPainter = new DefaultPainter();

    /**
     * Интерфейс для рисования изображения на панели.
     *
     * @see #setPainter(Painter)
     * @see #getPainter()
     */
    private Painter painter = defaultPainter;

    /**
     * Предпочтительный размер панели.
     */
    private Dimension defaultSize = null;

    /**
     * Должна ли отображаться отладочная информация.
     */
    private boolean displayDebugInfo = false;

    /**
     * Зеркальное ли изображение.
     */
    private boolean mirrored = false;

    /**
     * Отображение квадрата.
     */
    private boolean aimDisplayed = true;
    /**
     * Размеры квадрата
     */
    private int aimWidth = 32;
    private int aimHeight = 32;

    /**
     * Пауза чтения кадров
     */
    private static int PAUSE = 0;

    /**
     * Трансформер изображения.
     */
    private volatile DetectorImageTransformer transformer = null;

    /**
     * Контроллер.
     */
    private Controller controller;

    /**
     * Создает панель и автоматически запускает детектор.
     *
     * @param detector   детектор, который будет использоваться для получения изображений
     * @param controller ссылка на контроллер
     */
    public DetectorPanel(Detector detector, Controller controller,boolean asinc) {
        this(detector, true);
        this.controller = controller;
    }

    /**
     * Создает панель и автоматически запускает детектор.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     */
    public DetectorPanel(Detector detector,boolean asinc) {
        this(detector, true,asinc);
    }

    /**
     * Создает новую панель, которая отображает изображение с устройства в вашем приложении Swing.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     * @param start    true, если детектор должен запускаться автоматически
     */
    public DetectorPanel(Detector detector, boolean start,boolean asinc) {
        this(detector, null, start,asinc);
    }

    /**
     * Создает новую панель, которая отображает изображение с устройства в вашем приложении Swing.
     * Если аргумент размера панели равен null, тогда будет использоваться размер изображения.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     * @param size     размер панели
     * @param start    true, если детектор должен запускаться автоматически
     */
    public DetectorPanel(Detector detector, Dimension size, boolean start,boolean asinc) {
        this(detector, size, start, new DefaultImageSupplier(detector),asinc);
    }

    public DetectorPanel(Detector detector, Dimension size, boolean start, ImageSupplier supplier,boolean asinc) {

        if (detector == null) {
            throw new IllegalArgumentException(String.format("Detector argument in %s constructor cannot be null!",
                    getClass().getSimpleName()));
        }

        this.defaultSize = size;
        this.detector = detector;
        this.updater = new ImageUpdater();
        this.supplier = supplier;

        setDoubleBuffered(true);

        if (size == null) {
            Dimension r = detector.getViewSize();
            if (r == null) {
                r = detector.getViewSizes()[0];
            }
            setPreferredSize(r);
        } else {
            setPreferredSize(size);
        }

        if (start) {
            start(asinc);
        }
    }

    /**
     * Установить новый Painter.
     * Painter — это класс, который делает изображение видимым.
     *
     * @param painter объект рисования, который нужно установить
     */
    public void setPainter(Painter painter) {
        this.painter = painter;
    }

    /**
     * Используемый Painter для рисования изображения на панели
     *
     * @return Painter object
     */
    public Painter getPainter() {
        return painter;
    }

    /**
     * Вызывается каждый раз при необходимости перерисовки панели.
     * Если изображение null, то рисуем панель, если изображение существует, то отрисовываем его.
     *
     * @param g the <code>Graphics</code> object to protect
     */
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (image == null) {
            painter.paintPanel(this, (Graphics2D) g);//рисуем панель
        } else {
            painter.paintImage(this, image, (Graphics2D) g);//рисуем изображение
        }
    }

    /**
     * Открытие панели, старт рендеринга и открытие устройства.
     */
    public void start(boolean async) {

        if (!started.compareAndSet(false, true)) {
            return;
        }

        detector.addDetectorListener(this);

        LOG.debug("Starting panel rendering and trying to open attached detector");

        updater.start();

        starting = true;
        /**
         * Фоновая задачи на открытие детектора.
         */
        final SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                try {
                    if (!detector.isOpen()) {
                        errored = !detector.open(async);
                    }
                } catch (DetectorException e) {
                    errored = true;
                    throw e;
                } finally {
                    starting = false;
                    repaintPanel();
                }
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Остановка рендеринга и закрытие детектора.
     */
    public void stop() {

        if (!started.compareAndSet(true, false)) {
            return;
        }

        detector.removeDetectorListener(this);

        LOG.debug("Stopping panel rendering and closing attached detector");

        try {
            updater.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        image = null;
        /**
         * Фоновая задача на закрытие детектора.
         */
        final SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                try {
                    if (detector.isOpen()) {
                        errored = !detector.close();
                    }
                } catch (DetectorException e) {
                    errored = true;
                    throw e;
                } finally {
                    repaintPanel();
                }
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Перекрашивание панели в асинхронном режиме Swing.
     */
    private void repaintPanel() {
        SwingUtilities.invokeLater(repaint);
    }

    /**
     * Постановка на паузу рендеринг.
     */
    public void pause() {
        if (paused) {
            return;
        }

        LOG.debug("Pausing panel rendering");

        paused = true;
    }

    /**
     * Возобновление рендеринга.
     */
    public void resume() {

        if (!paused) {
            return;
        }

        LOG.debug("Resuming panel rendering");

        paused = false;
    }

    /**
     * Включено ли ограничение частоты?
     *
     * @return True or false
     */
    public boolean isFPSLimited() {
        return frequencyLimit;
    }

    /**
     * Включить или отключить ограничение частоты.
     * Если true, изображения будут загружены в настроенных временных интервалах.
     * Если false, изображения будут загружаться так быстро, как только устройство сможет их обслужить.
     *
     * @param frequencyLimit true, если ограничиваете частоту запросов изображения
     */
    public void setFPSLimited(boolean frequencyLimit) {
        this.frequencyLimit = frequencyLimit;
    }

    /**
     * Получить частоту рендеринга в кадрах в секунду (эквивалентно Гц).
     *
     * @return Частота рендеринга
     */
    public double getFPSLimit() {
        return frequency;
    }

    /**
     * Установите частоту рендеринга (в Гц или FPS).
     * Минимальная частота - 0,016 (1 кадр в минуту),
     * максимум - 25 (25 кадров в секунду).
     *
     * @param fps Частота рендеринга.
     */
    public void setFPSLimit(double fps) {
        if (fps > MAX_FREQUENCY) {
            fps = MAX_FREQUENCY;
        }
        if (fps < MIN_FREQUENCY) {
            fps = MIN_FREQUENCY;
        }
        this.frequency = fps;
    }

    /**
     * Включено ли отображение отладочной информации.
     *
     * @return True, если включена отладочная информация, иначе false
     */
    public boolean isDisplayDebugInfo() {
        return displayDebugInfo;
    }

    /**
     * Отображение отладочной информации на поверхности изображения.
     *
     * @param displayDebugInfo значение для управления отладочной информацией.
     */
    public void setDisplayDebugInfo(boolean displayDebugInfo) {
        this.displayDebugInfo = displayDebugInfo;
    }

    /**
     * Этот метод возвращает значение true, если для устройства настроено отображение FPS.
     * Возвращаемое значение по умолчанию — false.
     *
     * @return True, если FPS отображается.
     * @see #setFPSDisplayed(boolean)
     */
    public boolean isFPSDisplayed() {
        return frequencyDisplayed;
    }

    /**
     * Этот метод предназначен для управления отображением FPS.
     *
     * @param displayed true или false.
     */
    public void setFPSDisplayed(boolean displayed) {
        this.frequencyDisplayed = displayed;
    }

    /**
     * Этот метод вернет true, если панель настроена на отображение размера изображения.
     * Строка будет напечатана в правом нижнем углу поверхности панели.
     *
     * @return True, если панель настроена на отображение размера изображения
     */
    public boolean isImageSizeDisplayed() {
        return imageSizeDisplayed;
    }

    /**
     * Настраивает панель для отображения размера изображения, которое будет отображаться.
     *
     * @param imageSizeDisplayed, если true, размеры в пикселях отображаются поверх изображения.
     */
    public void setImageSizeDisplayed(boolean imageSizeDisplayed) {
        this.imageSizeDisplayed = imageSizeDisplayed;
    }

    /**
     * Включить/выключить сглаживание.
     *
     * @param antialiasing : true для включения, false для отключения.
     */
    public void setAntialiasingEnabled(boolean antialiasing) {
        this.antialiasingEnabled = antialiasing;
    }

    /**
     * @return True, если сглаживание включено, иначе false
     */
    public boolean isAntialiasingEnabled() {
        return antialiasingEnabled;
    }

    /**
     * Находится ли панель в старте.
     *
     * @return True, если панель запускается
     */
    public boolean isStarting() {
        return starting;
    }

    /**
     * Запущен ли детектор и начата ли отрисовка.
     *
     * @return True, если начата отрисовка панели
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Этот метод возвращает текущий режим рисования, в основном используемый пользовательскими рисовальщиками.
     *
     * @return текущее значение {@link DrawMode}
     */
    public DrawMode getDrawMode() {
        return this.drawMode;
    }

    /**
     * Этот метод устанавливает режим рисования
     *
     * @param drawMode желаемый {@link DrawMode}
     */
    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
    }

    /**
     * Указывает, находится ли панель в состоянии ошибки.
     *
     * @return true, если панель в ошибке.
     */
    public boolean isErrored() {
        return errored;
    }

    /**
     * Получить Painter по умолчанию, используемый для рисования панели.
     *
     * @return Painter по умолчанию
     */
    public Painter getDefaultPainter() {
        return defaultPainter;
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

    @Override
    public void detectorOpen(DetectorEvent we) {

        // если размер по умолчанию не указан, то используем размер с устройства
        // (это будет текущее разрешение детектора)

        if (defaultSize == null) {
            setPreferredSize(detector.getViewSize());
        }
    }

    @Override
    public void detectorClosed(DetectorEvent we) {
        stop();
    }

    @Override
    public void detectorDisposed(DetectorEvent we) {
        stop();
    }

    @Override
    public void detectorImageObtained(DetectorEvent we) {
        // do nothing
    }

    /**
     * Этот метод возвращает значение true, если зеркальное отображение изображений включено.
     * Значение по умолчанию false.
     *
     * @return True, если изображение зеркальное, иначе false
     */
    public boolean isMirrored() {
        return mirrored;
    }

    /**
     * Устанавливает, будет ли зеркально отображаться изображение устройства.
     * Если false, то изображение не модифицируется.
     *
     * @param mirrored параметр, чтобы контролировать, должно ли изображение быть зеркальным
     */
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    /**
     * Return {@link Detector} используемый этой панелью.
     *
     * @return {@link Detector}
     */
    public Detector getDetector() {
        return detector;
    }

    /**
     * @return {@link BufferedImage} отображается на {@link DetectorPanel}
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Запрос статуса отображения квадрата.
     *
     * @return true- если отображается.
     */
    public boolean isAimDisplayed() {
        return aimDisplayed;
    }

    /**
     * Установка отображения квадрата.
     *
     * @param aimDisplayed
     */
    public void setAimDisplayed(boolean aimDisplayed) {
        this.aimDisplayed = aimDisplayed;
    }

    /**
     * Получение ширины квадрата.
     *
     * @return
     */
    public int getAimWidth() {
        return aimWidth;
    }

    /**
     * Установка ширины квадрата.
     */
    public void setAimWidth(int aimWidth) {
        this.aimWidth = aimWidth;
    }

    /**
     * Получение высоты квадрата.
     *
     * @return
     */
    public int getAimHeight() {
        return aimHeight;
    }

    /**
     * Установка высоты квадрата.
     */
    public void setAimHeight(int aimHeight) {
        this.aimHeight = aimHeight;
    }

    /**
     * Установка паузы на выборку между кадрами
     *
     * @param pause в миллисекундах
     * @return
     */

    public void setPause(int pause) {
        if (pause > 10000) {
            pause = 10000;
        }
        if (pause < 0) {
            pause = 0;
        }
        PAUSE = pause;
    }

    /**
     * Установка поворота изображения.
     *
     * @param flipper
     */
    public static void setFlipper(JHFlipFilter flipper) {
        DetectorPanel.flipper = flipper;
    }

    /**
     * Установка фильтра.
     *
     * @param filter
     */
    public static void setFilter(JHFilter filter) {
        DetectorPanel.filter = filter;
    }

    /**
     * Установка нормалайзера.
     *
     * @param normalayzer
     */
    public static void setNormalayzer(JHFilter normalayzer) {
        DetectorPanel.normalayzer = normalayzer;
    }

}
