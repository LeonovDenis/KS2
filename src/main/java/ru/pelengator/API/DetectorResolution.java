package ru.pelengator.API;

import java.awt.Dimension;

/**
 * Набор разрешений
 */
public enum DetectorResolution {
    /**
     * Разрешение для детектора размером 128x128 px.
     */
    CHINA(128, 128),
    /**
     * Разрешение для детектора размером 92x90 px.
     */
    CHINALOW(92, 90);

    /**
     * Разрешение.
     */
    private Dimension size = null;

    /**
     * @param width  разрешение по ширине.
     * @param height разрешение по высоте.
     */
    private DetectorResolution(int width, int height) {
        this.size = new Dimension(width, height);
    }

    /**
     * Получение разрешения.
     *
     * @return Dimension object
     */
    public Dimension getSize() {
        return size;
    }

    public int getPixelsCount() {
        return size.width * size.height;
    }

    public Dimension getAspectRatio() {
        final int factor = getCommonFactor(size.width, size.height);
        final int wr = size.width / factor;
        final int hr = size.height / factor;
        return new Dimension(wr, hr);
    }

    private int getCommonFactor(int width, int height) {
        return (height == 0) ? width : getCommonFactor(height, width % height);
    }

    public int getWidth() {
        return size.width;
    }

    public int getHeight() {
        return size.height;
    }

    @Override
    public String toString() {

        final int w = size.width;
        final int h = size.height;
        final Dimension ratio = getAspectRatio();
        final int rw = ratio.width;
        final int rh = ratio.height;

        return new StringBuilder()
                .append(super.toString())
                .append(' ')
                .append(w).append('x').append(h)
                .append(" (")
                .append(rw).append(':').append(rh)
                .append(')')
                .toString();
    }
}
