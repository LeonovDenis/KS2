package ru.pelengator.API;

import java.awt.image.BufferedImage;

/**
 * Трансформер изображенияс детектора.
 */
public interface DetectorImageTransformer {
    /**
     * Преобразование картинки
     * @param image ресурс
     * @return преобразованный ресурс
     */
    BufferedImage transform(BufferedImage image);

    /**
     * Конвертирование значения АЦП в цвет
     * @param value
     * @return
     */
    int convertValueToColor(int value);

    /**
     *
     * @return разрядность преобразования сигнала
     */
    float getRazryadnost();
}
