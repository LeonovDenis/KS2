package ru.pelengator.API;
/**
 * Слушатель детектора
 *
 * @author
 */
public interface DetectorListener {

    /**
     * Детектор был открыт.
     *
     * @param de событие
     */
    void detectorOpen(DetectorEvent de);

    /**
     *  Детектор  был закрыт
     *
     * @param de событие
     */
    void detectorClosed(DetectorEvent de);

    /**
     *  Детектор  был удален
     *
     * @param de событие
     */
    void detectorDisposed(DetectorEvent de);

    /**
     *  Получено изображение с детектора.
     *
     * @param de событие
     */
    void detectorImageObtained(DetectorEvent de);
}
