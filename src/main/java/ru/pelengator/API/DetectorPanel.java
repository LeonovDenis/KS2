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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
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
import ru.pelengator.Controller;

import static java.awt.RenderingHints.*;
import static ru.pelengator.API.util.Utils.*;


/**
 * Простая реализация JPanel, позволяющая пользователям отображать изображения
 */
public class DetectorPanel extends JPanel implements DetectorListener, PropertyChangeListener {

    /**
     * Это перечисление предназначено для управления тем, как изображение будет отображаться в границах панели.
     */
    public enum DrawMode {

        /**
         * Не изменяйте размер изображения - рисуйте как есть. Это заставит изображение исчезнуть
         * границы, если панель меньше размера изображения.
         */
        NONE,

        /**
         * Изменит размер изображения до границ панели. Этот режим не заботится о масштабе изображения, поэтому
         * конечное изображение может быть нарушено.
         */
        FILL,

        /**
         * Будет помещать изображение в границы панели. Это изменит размер изображения и сохранит как x, так и y.
         * масштаб.
         */
        FIT,
    }

    /**
     * Этот интерфейс можно использовать для передачи {@link BufferedImage} to {@link DetectorPanel}.
     */
    public interface ImageSupplier {

        /**
         * @return {@link BufferedImage} для отображения в {@link DetectorPanel}
         */
        public BufferedImage get();
    }

    /**
     * Реализация {@link ImageSupplier} по умолчанию, используемая в {@link DetectorPanel}. Он вызывает
     * {@link Detector#getImage()} и вернуть {@link BufferedImage}.
     */
    private static class DefaultImageSupplier implements ImageSupplier {

        private final Detector detector;

        public DefaultImageSupplier(Detector detector) {
            this.detector = detector;
        }

        @Override
        public BufferedImage get() {
            return detector.getImage();
        }
    }

    /**
     * Интерфейс рисовальщика, используемый для рисования изображения в панели.
     */
    public interface Painter {

        /**
         * Панель краски без изображения.
         *
         * @param panel панель для рисования
         * @param g2    графический 2D-объект, используемый для рисования
         */
        void paintPanel(DetectorPanel panel, Graphics2D g2);

        /**
         * Нарисуйте изображение на панели.
         *
         * @param panel панель для рисования
         * @param image изображение
         * @param g2    графический 2D-объект, используемый для рисования
         */
        void paintImage(DetectorPanel panel, BufferedImage image, Graphics2D g2);
    }

    /**
     * Художник по умолчанию, используемый для рисования изображения на панели.
     */
    public class DefaultPainter implements Painter {

        /**
         * Имя устройства
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

            final String strInitDevice = rb.getString("INITIALIZING_DEVICE");
            final String strNoImage = rb.getString("NO_IMAGE");
            final String strDeviceError = rb.getString("DEVICE_ERROR");

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


                String str = String.format("%.1f", detector.getFPS());
                stringFps = str;
            }

            if (isImageSizeDisplayed()) {

                String res = String.format("%d\u2A2F%d px", iw, ih);

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
            if (isAimDisplayed()) {
                drawRect(g2, iw, ih, aimWidth, aimHeight, x, y, s);
            }

            if (isDisplayDebugInfo()) {

                if (lastRepaintTime < 0) {
                    lastRepaintTime = System.currentTimeMillis();
                } else {

                    long now = System.currentTimeMillis();
                    String res = String.format("DEBUG: repaints per second: %.1f", (double) 1000 / (now - lastRepaintTime));
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
            Thread t = new Thread(r, String.format("detector-panel-scheduled-executor-%d", number.incrementAndGet()));
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Этот исполняемый файл будет делать не что иное, как перекрашивать панель.
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

    /**
     * S/N, используемый Java для сериализации bean-компонентов.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorPanel.class);

    /**
     * Минимальная частота кадров в секунду.
     */
    public static final double MIN_FREQUENCY = 0.016; // 1 frame per minute

    /**
     * Максимальная частота кадров в секунду.
     */
    private static final double MAX_FREQUENCY = 50; // 50 frames per second

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
     * Этот исполняемый файл будет делать не что иное, как перекрашивать панель.
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
     * Средство обновления изображений считывает изображения с камеры и принудительно перекрашивает панель.
     */
    private class ImageUpdater implements Runnable {

        /**
         * Перерисовка обновлений панели расписания планировщика.
         */
        private class RepaintScheduler extends Thread {

            /**
             * Перерисовка обновлений панели расписания планировщика.
             */
            public RepaintScheduler() {
                setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
                setName(String.format("repaint-scheduler-%s", detector.getName()));
                setDaemon(true);
            }

            @Override
            public void run() {

                // ничего не делать, когда не работает
                if (!running.get()) {
                    return;
                }

                repaintPanel();

                // цикл при запуске для ожидания изображений
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
                    // ограничено конкретным значением и панель не будет
                    // отображается чаще, чем конкретное значение

                    if (detector.isOpen()) {

                        if (isFPSLimited()) {
                            executor.scheduleAtFixedRate(updater, 0, (long) (1000 / frequency), TimeUnit.MILLISECONDS);
                        } else {
                            executor.scheduleWithFixedDelay(updater, 100, 1, TimeUnit.MILLISECONDS);
                        }
                    } else {
                        //todo добавка
                    /**    try {
                            if (!detector.isOpen()) {
                            //    errored = !detector.open();
                                System.out.println("Открытие в добавке");
                            }
                        } catch (DetectorException e) {
                            errored = true;
                        } finally {
                            starting = false;
                            repaintPanel();
                        }*/
                        executor.schedule(this, 500, TimeUnit.MILLISECONDS);
                    //    LOG.error("Автоматический повторный запуск детектора");
                    }
                } catch (RejectedExecutionException e) {

                    // экзекьютор остановлен, значит кто-то
                    // остановил панель/устройство  до того, как оно было на самом деле
                    // полностью запущен (был в "стартовом" таймфрейме)

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
         * Запустить ремастер. Может вызываться много раз, но только первый вызов вступит в силу.
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

            // ничего не делать, когда программа обновления не запущена, когда детектор закрыт или
            // перерисовка панели приостановлена

            if (!running.get() || !detector.isOpen() || paused) {
                return;
            }

            // получаем новое изображение

            BufferedImage tmp = supplier.get();
            boolean repaint = true;

            if (tmp != null) {

                tmp = copyImage(tmp);

                convertImageRGB(tmp);

                // игнорировать перерисовку, если изображение такое же, как и раньше
                if (image == tmp) {
                    repaint = false;
                }
                errored = false;
                image = tmp;
            }
            if (repaint) {
                repaintPanel();
            }
        }
    }


    /**
     * Пакет ресурсов.
     */
    private ResourceBundle rb = null;

    /**
     * Режим того, как изображение будет изменено, чтобы соответствовать границам панели. По умолчанию
     * {@link DrawMode#FIT}
     *
     * @see DrawMode
     */
    private DrawMode drawMode = DrawMode.FIT;

    /**
     * Частота запроса кадров.
     */
    private double frequency = 25; // FPS

    /**
     * Ограничена ли частота запроса кадров? Если true, изображения будут загружаться в заданное время.
     * интервалы. Если false, изображения будут загружаться так быстро, как только детектор сможет их обслужить.
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

    private final ImageSupplier supplier;

    /**
     * Repainter используется для получения изображений и принудительной перерисовки панели, когда изображение готово.
     */
    private final ImageUpdater updater;

    /**
     * Изображение отображается в данный момент.
     */
    private BufferedImage image = null;

    /**
     * детектор в настоящее время запускается.
     */
    private volatile boolean starting = false;

    /**
     * Painting приостановлен
     */
    private volatile boolean paused = false;

    /**
     * Есть ли проблемы
     */
    private volatile boolean errored = false;

    /**
     * детектор запущен.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * painter по умолчанию.
     */
    private final Painter defaultPainter = new DefaultPainter();

    /**
     * Painter используется для рисования изображения на панели.
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
     * Если отладочная информация должна отображаться.
     */
    private boolean displayDebugInfo = false;

    /**
     * Зеркальное изображение.
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
     * Контроллер
     */
    private Controller controller;

    /**
     * Создает панель детектора и автоматически запускает детектор.
     *
     * @param detector   детектор, который будет использоваться для получения изображений
     * @param controller ссылка на контроллер
     */
    public DetectorPanel(Detector detector, Controller controller) {
        this(detector, true);
        this.controller = controller;
    }

    /**
     * Создает панель детектора и автоматически запускает детектор.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     */
    public DetectorPanel(Detector detector) {
        this(detector, true);
    }

    /**
     * Создает новую панель, которая отображает изображение с камеры в вашем приложении Swing.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     * @param start    true, если детектор должен запускаться автоматически
     */
    public DetectorPanel(Detector detector, boolean start) {
        this(detector, null, start);
    }

    /**
     * Создает новую панель веб-камеры, которая отображает изображение с камеры в вашем приложении Swing. Если
     * Аргумент размера панели равен нулю, тогда будет использоваться размер изображения. Если вы хотите заполнить панель
     * область с изображением, даже если ее размер отличается, то можно использовать
     * Метод {@link DetectorPanel#setFillArea(boolean)} для настройки.
     *
     * @param detector детектор, который будет использоваться для получения изображений
     * @param size     размер панели
     * @param start    true, если детектор должен запускаться автоматически
     * @see DetectorPanel#setFillArea(boolean)
     */
    public DetectorPanel(Detector detector, Dimension size, boolean start) {
        this(detector, size, start, new DefaultImageSupplier(detector));
    }

    public DetectorPanel(Detector detector, Dimension size, boolean start, ImageSupplier supplier) {

        if (detector == null) {
            throw new IllegalArgumentException(String.format("Detector argument in %s constructor cannot be null!", getClass().getSimpleName()));
        }

        this.defaultSize = size;
        this.detector = detector;
        this.updater = new ImageUpdater();
        this.supplier = supplier;
        this.rb = DetectorUtils.loadRB(DetectorPanel.class, getLocale());

        //setDoubleBuffered(true);

        addPropertyChangeListener("locale", this);

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
            start();
        }
    }

    /**
     * Установить новый художник. Painter — это класс, который делает изображение видимым, когда
     *
     * @param painter объект рисования, который нужно установить
     */
    public void setPainter(Painter painter) {
        this.painter = painter;
    }

    /**
     * Используйте Painter для рисования изображения на панели
     *
     * @return Painter object
     */
    public Painter getPainter() {
        return painter;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (image == null) {
            painter.paintPanel(this, (Graphics2D) g);
        } else {
            painter.paintImage(this, image, (Graphics2D) g);
        }
    }

    /**
     * Откройте и начните рендеринг.
     */
    public void start() {

        if (!started.compareAndSet(false, true)) {
            return;
        }

        detector.addDetectorListener(this);

        LOG.debug("Starting panel rendering and trying to open attached detector");

        updater.start();

        starting = true;

        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                try {
                    if (!detector.isOpen()) {
                        errored = !detector.open();
                        System.out.println("Открытие на старте");
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
     * Остановите рендеринг и закройте detector.
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

        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

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
     * Пауза рендеринга.
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
     * Включить или отключить ограничение частоты. Ограничение по частоте следует использовать для <b>всех IP-камер
     * работает в режиме pull</b> (чтобы сократить количество HTTP-запросов). Если true, изображения будут загружены
     * в настроенных временных интервалах. Если false, изображения будут загружаться так быстро, как только камера сможет их обслужить.
     * их.
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
     * Установите частоту рендеринга (в Гц или FPS). Минимальная частота 0,016 (1 кадр в минуту) и
     * максимум 25 (25 кадров в секунду).
     *
     * @param fps the frequency
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
     * Включено ли отображение некоторой отладочной информации.
     *
     * @return True, если включена отладочная информация, иначе false
     */
    public boolean isDisplayDebugInfo() {
        return displayDebugInfo;
    }

    /**
     * Отображение некоторой отладочной информации на поверхности изображения.
     *
     * @param displayDebugInfo значение для управления отладочной информацией
     */
    public void setDisplayDebugInfo(boolean displayDebugInfo) {
        this.displayDebugInfo = displayDebugInfo;
    }

    /**
     * Этот метод возвращает значение true, если для камеры настроено отображение FPS на поверхности панели.
     * Возвращаемое значение по умолчанию — false.
     *
     * @return True, если FPS камеры отображается на поверхности панели
     * @see #setFPSDisplayed(boolean)
     */
    public boolean isFPSDisplayed() {
        return frequencyDisplayed;
    }

    /**
     * Этот метод предназначен для управления отображением FPS камеры на поверхности панели .
     *
     * @param displayed значение для управления отображением FPS камеры .
     */
    public void setFPSDisplayed(boolean displayed) {
        this.frequencyDisplayed = displayed;
    }

    /**
     * Этот метод вернет true, если панель настроена на отображение размера изображения. То
     * строка будет напечатана в правом нижнем углу поверхности панели.
     *
     * @return True, если панель настроена на отображение размера изображения
     */
    public boolean isImageSizeDisplayed() {
        return imageSizeDisplayed;
    }

    /**
     * Настройте панель для отображения размера изображения камеры, которое будет отображаться.
     *
     * @param imageSizeDisplayed, если true, размеры в пикселях отображаются поверх изображения.
     */
    public void setImageSizeDisplayed(boolean imageSizeDisplayed) {
        this.imageSizeDisplayed = imageSizeDisplayed;
    }

    /**
     * Включить/выключить сглаживание.
     *
     * @param antialiasing : true для включения, false для отключения сглаживания
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
     * Начинается ли перекраска панели детектора.
     *
     * @return True, если панель запускается
     */
    public boolean isStarting() {
        return starting;
    }

    /**
     * Начата ли перекраска панели детектора.
     *
     * @return True, если начата перекраска панели
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
     * Указывает, находится ли панель в состоянии ошибки
     *
     * @return true, если на панели присутствует ошибка
     */
    public boolean isErrored() {
        return errored;
    }

    /**
     * Подсказки по рендерингу, в основном используемые для пользовательских рисовальщиков
     *
     * @return сохраненные RenderingHints
     * @deprecated используйте {@link #getDrawMode()}.
     */
    @Deprecated
    public Map<RenderingHints.Key, Object> getImageRenderingHints() {
        return imageRenderingHints;
    }

    @Deprecated
    public boolean isFitArea() {
        return drawMode == DrawMode.FIT;
    }

    /**
     * Этот метод изменит режим рисования области панели, поэтому изображение будет изменено в размере и
     * Сохраняйте коэффициент масштабирования, чтобы он соответствовал границам рисуемой панели. Если установлено значение false, режим будет
     * сбросить на {@link DrawMode#NONE}, поэтому изображение будет отображаться как есть.
     *
     * @param fitArea режим подгонки включен или отключен
     * @deprecated use {@link #setDrawMode(DrawMode drawMode)} instead.
     */
    @Deprecated
    public void setFitArea(boolean fitArea) {
        this.drawMode = fitArea ? DrawMode.FIT : DrawMode.NONE;
    }

    /**
     * Размер изображения будет изменен, чтобы заполнить область панели, если это правда. Если false, то изображение будет отображаться так, как оно
     * было получено из экземпляра детектора.
     *
     * @param fillArea должно размещать изображение в области заполнения панели
     * @deprecated use {@link #setDrawMode(DrawMode drawMode)} instead.
     */
    @Deprecated
    public void setFillArea(boolean fillArea) {
        this.drawMode = fillArea ? DrawMode.FILL : DrawMode.NONE;
    }

    /**
     * Получить значение настройки области заливки. Размер изображения будет изменен, чтобы заполнить область панели, если это правда. Если ложь
     * то изображение будет отображаться так, как оно было получено с экземпляра веб-камеры.
     *
     * @return True if image is being resized, false otherwise
     * @deprecated use {@link #getDrawMode()} instead.
     */
    @Deprecated
    public boolean isFillArea() {
        return drawMode == DrawMode.FILL;
    }

    /**
     * Получить рисовальщик по умолчанию, используемый для рисования панели.
     *
     * @return Художник по умолчанию
     */
    public Painter getDefaultPainter() {
        return defaultPainter;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Locale lc = (Locale) evt.getNewValue();
        if (lc != null) {
            rb = DetectorUtils.loadRB(DetectorPanel.class, lc);
        }
    }

    @Override
    public void detectorOpen(DetectorEvent we) {

        // если размер по умолчанию не указан, то используем размер с Detector
        // устройство (это будет текущее разрешение Detector)

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
     * Этот метод возвращает значение true, если зеркальное отображение изображений включено. Значение по умолчанию неверно.
     *
     * @return True, если изображение зеркальное, иначе false
     */
    public boolean isMirrored() {
        return mirrored;
    }

    /**
     * Решите, будет ли зеркально отображаться изображение с веб-камеры, нарисованное на поверхности панели. То
     * изображение с самой камеры не модифицируется.
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

    public static String stringFps = "";

    /**
     * Запрос статуса отображения квадрата
     *
     * @return true- если отображается
     */
    public boolean isAimDisplayed() {
        return aimDisplayed;
    }

    /**
     * установка отображения квадрата
     *
     * @param aimDisplayed
     */
    public void setAimDisplayed(boolean aimDisplayed) {
        this.aimDisplayed = aimDisplayed;
    }

    /**
     * Получение ширины квадрата
     *
     * @return
     */
    public int getAimWidth() {
        return aimWidth;
    }

    /**
     * Установка ширины квадрата
     */
    public void setAimWidth(int aimWidth) {
        this.aimWidth = aimWidth;
    }

    /**
     * получение высоты квадрата
     *
     * @return
     */
    public int getAimHeight() {
        return aimHeight;
    }

    /**
     * Установка высоты квадрата
     */
    public void setAimHeight(int aimHeight) {
        this.aimHeight = aimHeight;
    }
}
