package ru.pelengator.API;
import java.awt.image.BufferedImage;
import java.util.EventObject;
/**
 * Событие детектора.
 *
 */
public class DetectorEvent extends EventObject{

    private static final long serialVersionUID = 1L;

    /**
     * Полученное изображение.
     */
    private BufferedImage image = null;

    /**
     * Тип события
     */
    private DetectorEventType type = null;

    /**
     * Событие детектора.
     *
     * @param type тип события.
     * @param d объект события.
     */

    public DetectorEvent(DetectorEventType type, Detector d) {
        this(type, d, null);
    }

    /**
     * Событие детектора.
     *
     * @param type тип события.
     * @param d объект события.
     * @param image Полученное изображение.
     */
    public DetectorEvent(DetectorEventType type, Detector d, BufferedImage image) {
        super(d);
        this.type = type;
        this.image = image;
    }

    @Override
    public Detector getSource() {
        return (Detector) super.getSource();
    }

    /**
     * Возврат изображения, полученного устройством.
     * Этот метод вернет ненулевой объект
     * <b>только</b> в случае получения нового изображения.
     * Для всех остальных событий это просто вернет <b>null</b>.
     *
     * @return Полученное изображение
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Тип возвращаемого события.
     *
     * @return Тип события
     * @see DetectorEventType
     */
    public DetectorEventType getType() {
        return type;
    }

}
