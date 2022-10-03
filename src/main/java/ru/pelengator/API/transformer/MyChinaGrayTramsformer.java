package ru.pelengator.API.transformer;

import ru.pelengator.API.DetectorImageTransformer;

import java.awt.image.BufferedImage;

import static ru.pelengator.API.utils.Utils.*;

public class MyChinaGrayTramsformer implements DetectorImageTransformer {

    private boolean needReverseY;
    private float qvantCount;

    /**
     * Трансформер, реверсирующий строки по оси Y.
     */
    public MyChinaGrayTramsformer() {
        this(true);
    }

    /**
     * Трансформер, реверсирующий строки по оси Y, в зависимости от параметра.
     *
     * @param needReverseY true - если нужен реверс, false -если нет.
     */
    public MyChinaGrayTramsformer(boolean needReverseY) {
        this(true,ACP);
    }

    /**
     * Трансформер, реверсирующий строки по оси Y, в зависимости от параметра.
     *
     * @param needReverseY true - если нужен реверс, false -если нет.
     * @param ACP количество отсчетов АСП
     */
    public MyChinaGrayTramsformer(boolean needReverseY,float ACP) {
        this.needReverseY = needReverseY;
        this.qvantCount=ACP;
    }
    @Override
    public BufferedImage transform(BufferedImage image) {
        convertImageGray(image);
        return image;
    }


    @Override
    public int convertValueToColor(int value) {
        return qvantFilterGray(value);
    }


    @Override
    public float getRazryadnost() {
        return qvantCount;
    }
}
