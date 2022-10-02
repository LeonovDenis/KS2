package ru.pelengator.API.transformer.comFilters;

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

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;


/**
 * Удобный класс, который реализует те методы BufferedImageOp, которые редко изменяются.
 */
public abstract class JHFilter implements BufferedImageOp {

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
        if (dstCM == null) {
            dstCM = src.getColorModel();
        }
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), null);
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double();
        }
        dstPt.setLocation(srcPt.getX(), srcPt.getY());
        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    /**
     * Удобный метод получения пикселей ARGB из изображения.
     * Это попытка избежать cнижения производительности из-за того,
     * что BufferedImage.getRGB не управляет изображением.
     *
     * @param image  объект BufferedImage
     * @param x      левый край блока пикселей
     * @param y      правый край блока пикселей
     * @param width  ширина массива пикселей
     * @param height высота массива пикселей
     * @param pixels массив для хранения возвращаемых пикселей. Может быть нулевым.
     * @return пиксели
     * @see #setRGB
     */
    public int[] getRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
        }
        return image.getRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * Удобный метод установки пикселей ARGB в изображении.
     * Это попытка избежать cнижения производительности за то,
     * что BufferedImage.setRGB не управляет изображением.
     *
     * @param image  объект BufferedImage
     * @param x      левый край блока пикселей
     * @param y      правый край блока пикселей
     * @param width  ширина массива пикселей
     * @param height высота массива пикселей
     * @param pixels массив пикселей для установки
     * @see #getRGB
     */
    public void setRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            image.getRaster().setDataElements(x, y, width, height, pixels);
        } else {
            image.setRGB(x, y, width, height, pixels, 0, width);
        }
    }
}
