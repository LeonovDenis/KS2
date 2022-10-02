package ru.pelengator.API.transformer;

import ru.pelengator.API.DetectorImageTransformer;
import ru.pelengator.API.devises.china.ChinaDevice;

import java.awt.image.BufferedImage;

import static ru.pelengator.API.utils.Utils.*;

/**
 * Трансформер для обработки и квантования изображения, полученного с {@link ChinaDevice}
 */

public class MyChinaRgbImageTransformer implements DetectorImageTransformer {

    private boolean needReverseY;
    private float qvantCount;

    /**
     * Трансформер, реверсирующий строки по оси Y.
     */
    public MyChinaRgbImageTransformer() {
        this(true);
    }

    /**
     * Трансформер, реверсирующий строки по оси Y, в зависимости от параметра.
     *
     * @param needReverseY true - если нужен реверс, false -если нет.
     */
    public MyChinaRgbImageTransformer(boolean needReverseY) {
        this(true,ACP);
    }

    /**
     * Трансформер, реверсирующий строки по оси Y, в зависимости от параметра.
     *
     * @param needReverseY true - если нужен реверс, false -если нет.
     * @param ACP количество отсчетов АСП
     */
    public MyChinaRgbImageTransformer(boolean needReverseY,float ACP) {
        this.needReverseY = needReverseY;
        this.qvantCount=ACP;
    }

    @Override
    public BufferedImage transform(BufferedImage image) {
        convertImageRGB(image);
        return image;
    }

    @Override
    public int convertValueToColor(int value) {
        return qvantFilterRGB(value);
    }

    @Override
    public float getRazryadnost() {
        return this.qvantCount;
    }

}
