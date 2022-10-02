package ru.pelengator.API;
import java.util.EventObject;
/**
 * Это событие генерируется, когда детектор был найден или потерян.
 */
public class DetectorDiscoveryEvent extends EventObject  {

    private static final long serialVersionUID = 1L;

    /**
     * Тип события, информирующий о недавно подключенном детекторе.
     */
    public static final int ADDED = 1;

    /**
     * Тип события, информирующий о недавно отелюченном детекторе.
     */
    public static final int REMOVED = 2;

    /**
     * Тип события (детектор подключен/отключен).
     */
    private int type = -1;

    /**
     * Создать новое событие обнаружения детектора.
     *
     * @param detector детектор, который был найден или удален
     * @param type тип события
     * @see #ADDED
     * @see #REMOVED
     */
    public DetectorDiscoveryEvent(Detector detector, int type) {
        super(detector);
        this.type = type;
    }

    /**
     * Возврат найденного или удаленного детектора.
     *
     * @return Экземпляр детектора
     */
    public Detector getDetector() {
        return (Detector) getSource();
    }

    /**
     * Тип возвращаемого события (детектор подключен/отключен).
     *
     * @return Целочисленное  значение
     * @see #ADDED
     * @see #REMOVED
     */
    public int getType() {
        return type;
    }
}
