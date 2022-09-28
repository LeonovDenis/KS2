package ru.pelengator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.*;
import ru.pelengator.Controller;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.RenderingHints.*;
import static ru.pelengator.API.util.Utils.convertImageRGB;
import static ru.pelengator.API.util.Utils.copyImage;


public class ImagePanel extends JPanel {

    public enum DrawMode {
        NONE,
        FILL,
        FIT,
    }

    public interface Painter {
        /**
         * Нарисуйте изображение на панели.
         *
         * @param panel панель для рисования
         * @param image изображение
         * @param g2    графический 2D-объект, используемый для рисования
         */
        void paintImage(ImagePanel panel, BufferedImage image, Graphics2D g2);
    }


    public class DefaultPainter implements Painter {

        /**
         * Размер буферизованного изображения изменен, чтобы соответствовать области рисования панели.
         */
        private BufferedImage resizedImage = null;
        @Override
        public void paintImage(ImagePanel owner, BufferedImage image, Graphics2D g2) {

            assert owner != null;
            assert image != null;
            assert g2 != null;

            int pw = getWidth();
            int ph = getHeight();
            int iw = image.getWidth();
            int ih = image.getHeight();

            Object antialiasing = g2.getRenderingHint(KEY_ANTIALIASING);
            Object rendering = g2.getRenderingHint(KEY_RENDERING);

            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g2.setBackground(Color.WHITE);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, pw, ph);

            // resized image position and size
            int x = 0;
            int y = 0;
            int w = 0;
            int h = 0;

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
                    double s = Math.max((double) (iw) / pw, (double) (ih) / ph);
                    double niw = (iw / s) - 90;
                    double nih = (ih / s) - 90;
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

               //     for (Entry<Key, Object> hint : imageRenderingHints.entrySet()) {
                //        gr.setRenderingHint(hint.getKey(), hint.getValue());
                //    }

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

                    int stroke = 4;
                    int line = 10;

                    gr.setStroke(new BasicStroke(stroke));
                    gr.setColor(Color.BLACK);
                    Font font = new Font("sans-serif", Font.BOLD, 16);
                    gr.setFont(font);
                    FontMetrics metrics = gr.getFontMetrics(font);
                    gr.drawRect(dx1 - stroke / 2, dy1 - stroke / 2, dx2 - dx1 + stroke-1, dy2 - dy1 + stroke-1);

                    double iHy = ih / 4.0;
                    double iHx = ih / 4.0;
                    int iy = (dy2 - dy1) / 4;
                    int ix = (dx2 - dx1) / 4;
                    for (int j = 0; j < 5; j++) {
                        int valueY = (ih - 1 - (int) (iHy * j));
                        int valueX = (iw - 1 - (int) (iHx * j));

                        if (valueY < 5) {
                            valueY = 0;
                            valueX = 0;
                            //вертикаль
                            gr.drawString(String.valueOf(valueY), dx1 - stroke / 2 - line - (metrics.stringWidth(String.valueOf(valueY))) - 5,
                                    dy1 - 1 - 1 - stroke + iy * j + metrics.getHeight() / 2);
                            gr.drawLine(dx1 - stroke / 2 - line, dy1 - stroke / 2 + iy * j, dx1 - stroke / 2, dy1 - stroke / 2 + iy * j);

                            //горизонталь
                            gr.drawString(String.valueOf(valueX),
                                    (int) (dx2 + (stroke / 2) - ix * j - metrics.stringWidth(String.valueOf(valueX)) / 2.0),
                                    dy2 + stroke + line + 5 + metrics.getHeight() / 2);
                            gr.drawLine(dx2 - 1 + stroke / 2 - ix * j, dy2 + stroke / 2 + line, dx2 - 1 + stroke / 2 - ix * j, dy2 + stroke / 2);

                        } else {
                            //вертикаль
                            gr.drawString(String.valueOf(valueY), dx1 - stroke / 2 - line - (metrics.stringWidth(String.valueOf(valueY))) - 5,
                                    dy1 - 1 - stroke + iy * j + metrics.getHeight() / 2);
                            gr.drawLine(dx1 - stroke / 2 - line, dy1 - 1 + stroke / 2 + iy * j, dx1 - stroke / 2, dy1 - 1 + stroke / 2 + iy * j);

                            //горизонталь
                            gr.drawString(String.valueOf(valueX),
                                    (int) (dx2 - 1 - (stroke / 2) - ix * j - metrics.stringWidth(String.valueOf(valueX)) / 2.0),
                                    dy2 + stroke + line + 5 + metrics.getHeight() / 2);

                            gr.drawLine(dx2 - 1 - stroke / 2 - ix * j, dy2 + stroke / 2 + line, dx2 - 1 - stroke / 2 - ix * j, dy2 + stroke / 2);
                        }
                    }
                } finally {
                    if (gr != null) {
                        gr.dispose();
                    }
                }
            }

            g2.drawImage(resizedImage, 0, 0, null);
            g2.setRenderingHint(KEY_ANTIALIASING, antialiasing);
            g2.setRenderingHint(KEY_RENDERING, rendering);

        }

        private void drawRect(Graphics2D g2, int w, int h) {

            int rw = 33;
            int rh = 33;
            int rx = (int) ((w - rw) / 2.0) - 0;
            int ry = (int) ((h - rh) / 2.0) - 0;


            g2.setColor(new Color(255, 255, 255, 255));
            g2.drawRect(rx, ry, rw, rh);
        }
    }

    /**
     * Этот исполняемый файл будет делать не что иное, как перекрашивать панель.
     */
    private static final class SwingRepainter implements Runnable {

        private ImagePanel panel = null;

        public SwingRepainter(ImagePanel panel) {
            this.panel = panel;
        }

        @Override
        public void run() {
            panel.repaint();
        }
    }

    /**
     * Регистратор.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImagePanel.class);


    public static final Map<Key, Object> DEFAULT_IMAGE_RENDERING_HINTS = new HashMap<Key, Object>();

    static {
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    /**
     * Этот исполняемый файл будет делать не что иное, как перекрашивать панель.
     */
    private final Runnable repaint = new SwingRepainter(this);

    /**
     * Подсказки рендеринга, которые будут использоваться при рисовании отображаемого изображения.
     */
    private Map<Key, Object> imageRenderingHints = new HashMap<Key, Object>(DEFAULT_IMAGE_RENDERING_HINTS);


    /**
     * Режим того, как изображение будет изменено, чтобы соответствовать границам панели. По умолчанию
     * {@link DrawMode#FIT}
     *
     * @see DrawMode
     */
    private DrawMode drawMode = DrawMode.FIT;

    /**
     * Включено ли сглаживание (true по умолчанию).
     */
    private boolean antialiasingEnabled = true;

    /**
     * Изображение отображается в данный момент.
     */
    private BufferedImage image = null;


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
     * Зеркальное изображение.
     */
    private boolean mirrored = false;


    public ImagePanel(Dimension size, boolean start) {

        this.defaultSize = size;
        setDoubleBuffered(true);
        setPreferredSize(size);
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

        if (image != null) {
            painter.paintImage(this, image, (Graphics2D) g);
        }
    }

    /**
     * Откройте и начните рендеринг.
     */
    public void start() {
        LOG.debug("Starting panel rendering and trying to open attached detector");
        repaintPanel();
    }

    /**
     * Остановите рендеринг и закройте detector.
     */
    public void stop() {
        LOG.debug("Stopping panel rendering and closing attached detector");
        image = null;
        repaintPanel();
    }

    /**
     * Перекрашивание панели в асинхронном режиме Swing.
     */
    public void repaintPanel() {
        SwingUtilities.invokeLater(repaint);
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
     * Получить рисовальщик по умолчанию, используемый для рисования панели.
     *
     * @return Художник по умолчанию
     */
    public Painter getDefaultPainter() {
        return defaultPainter;
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

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

}
