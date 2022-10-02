/*
Copyright 2006 Jerry Huxtable
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */


package ru.pelengator.API.transformer.comFilters;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;


/**
 * Фильтр, который переворачивает изображения или поворачивает их на угол, кратный 90 градусам.
 */
public class JHFlipFilter extends JHFilter {
    /**
     * Поворот изображения на 90 градусов по часовой стрелке.
     */
    public static final int FLIP_90CW = 4;

    /**
     * Поворот изображения на 90 градусов против часовой стрелки.
     */
    public static final int FLIP_90CCW = 5;

    /**
     * Поворот изображения на 180 градусов.
     */
    public static final int FLIP_180 = 6;

    private int operation;

    /**
     * FlipFilter, который переворачивается по горизонтали и вертикали.
     */
    public JHFlipFilter() {
        this(FLIP_90CW);
    }

    /**
     * Конструктор.
     *
     * @param operation направление поворота
     */
    public JHFlipFilter(int operation) {
        setOperation(operation);
    }

    /**
     * Установка ориентации.
     *
     * @param operation ориентация
     * @see #getOperation
     */
    public void setOperation(int operation) {
        this.operation = operation;
    }

    /**
     * Получение ориентации.
     *
     * @return ориентацию.
     * @see #setOperation
     */
    public int getOperation() {
        return operation;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        int[] inPixels = getRGB(src, 0, 0, width, height, null);

        int w = width;
        int h = height;

        int newW = w;
        int newH = h;
        switch (operation) {
            case FLIP_90CW:
                newW = h;
                newH = w;
                break;
            case FLIP_90CCW:
                newW = h;
                newH = w;
                break;
            case FLIP_180:
                break;
        }

        int[] newPixels = new int[newW * newH];

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int index = row * width + col;
                int newRow = row;
                int newCol = col;
                switch (operation) {
                    case FLIP_90CW:
                        newRow = col;
                        newCol = h - row - 1;
                        ;
                        break;
                    case FLIP_90CCW:
                        newRow = w - col - 1;
                        newCol = row;
                        break;
                    case FLIP_180:
                        newRow = h - row - 1;
                        newCol = w - col - 1;
                        break;
                }
                int newIndex = newRow * newW + newCol;
                newPixels[newIndex] = inPixels[index];
            }
        }

        if (dst == null) {
            ColorModel dstCM = src.getColorModel();
            dst = new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(newW, newH), dstCM.isAlphaPremultiplied(), null);
        }
        setRGB(dst, 0, 0, newW, newH, newPixels);

        return dst;
    }

    @Override
    public String toString() {
        switch (operation) {
            case FLIP_90CW:
                return "Поворот на 90";
            case FLIP_90CCW:
                return "Поворот на -90";
            case FLIP_180:
                return "Поворот на 180";
        }
        return "Flip";
    }
}
