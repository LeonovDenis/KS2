package ru.pelengator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static java.awt.RenderingHints.*;

/**
 * Интерактивная панелька с квадратом.
 */
public class ImagePanel extends JPanel {
    /**
     * Режимы заполнения окна.
     */
    public enum DrawMode {
        NONE,
        FILL,
        FIT,
    }

    public interface Painter {
        /**
         * Рисует изображение.
         *
         * @param panel панель .
         * @param image изображение.
         * @param g2    графический 2D-объект, используемый для рисования.
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
                    gr.drawRect(dx1 - stroke / 2, dy1 - stroke / 2, dx2 - dx1 + stroke - 1, dy2 - dy1 + stroke - 1);

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
    }

    /**
     * Отрисовщик.
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
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImagePanel.class);


    public static final Map<Key, Object> DEFAULT_IMAGE_RENDERING_HINTS = new HashMap<Key, Object>();

    static {
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        DEFAULT_IMAGE_RENDERING_HINTS.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    }

    /**
     * Отрисовщмк.
     */
    private final Runnable repaint = new SwingRepainter(this);

    /**
     * Настройки рендеринга.
     */
    private Map<Key, Object> imageRenderingHints = new HashMap<Key, Object>(DEFAULT_IMAGE_RENDERING_HINTS);

    /**
     * Режим прорисовки.
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
     * Текущее изображение.
     */
    private BufferedImage image = null;


    /**
     * Отрисовщик по-умолчанию.
     */
    private final Painter defaultPainter = new DefaultPainter();

    /**
     * Отрисовщик.
     */
    private Painter painter = defaultPainter;

    /**
     * Предпочтительный размер панели.
     */
    private Dimension defaultSize = null;


    /**
     * Зеркалировать изображение.
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


    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (image != null) {
            painter.paintImage(this, image, (Graphics2D) g);
        }
    }

    /**
     * Старт отрисовки.
     */
    public void start() {
        LOG.debug("Starting panel rendering and trying to open attached detector");
        repaintPanel();
    }

    /**
     * Остановка отрисовки.
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

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

}
