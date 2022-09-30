package ru.pelengator.API.utils;

import ru.pelengator.API.DetectorException;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;


/**
 * Вспомогательный классдля работы сизображениями
 */
public class ImageUtils {

    /**
     * Формат обмена графикой.
     */
    public static final String FORMAT_GIF = "GIF";

    /**
     * Портативный сетевой графический формат.
     */
    public static final String FORMAT_PNG = "PNG";

    /**
     * Формат Объединенной группы экспертов по фотографии.
     */
    public static final String FORMAT_JPG = "JPG";

    /**
     * Формат растрового изображения.
     */
    public static final String FORMAT_BMP = "BMP";

    /**
     * Формат растрового изображения протокола беспроводных приложений.
     */
    public static final String FORMAT_WBMP = "WBMP";

    /**
     * Преобразование {@link BufferedImage} в массив байтов.
     *
     * @param image  изображение для преобразования
     * @param format формат выходного изображения
     * @return Новый массив байтов
     */
    public static byte[] toByteArray(BufferedImage image, String format) {

        byte[] bytes = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, format, baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            throw new DetectorException(e);
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                throw new DetectorException(e);
            }
        }

        return bytes;
    }

    /**
     * Чтение изображения из пути
     * @param resource путь
     * @return изображение
     */
    public static BufferedImage readFromResource(String resource) {
        InputStream is = null;
        try {
            return ImageIO.read(is = ImageUtils.class.getClassLoader().getResourceAsStream(resource));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /**
     * Создание нового изображения
     * @param source старое изображение
     * @return
     */
    public static BufferedImage createEmptyImage(final BufferedImage source) {
        return new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Фиксация значения в диапазоне 0..255
     */
    public static int clamp(int c) {
        if (c < 0) {
            return 0;
        }
        if (c > 255) {
            return 255;
        }
        return c;
    }

    /**
     * Возвращает растр изображения в виде массива байтов.
     *
     * @param bi {@link BufferedImage}
     * @return Растровые данные в виде массива байтов
     */
    public static byte[] imageToBytes(BufferedImage bi) {
        return ((DataBufferByte) bi.getData().getDataBuffer()).getData();
    }
}