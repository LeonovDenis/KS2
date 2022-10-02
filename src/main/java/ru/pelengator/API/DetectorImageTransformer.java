package ru.pelengator.API;

import java.awt.image.BufferedImage;

/**
 * Трансформер изображенияс детектора.
 */
public interface DetectorImageTransformer {
    BufferedImage transform(BufferedImage image);
}
