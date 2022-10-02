package ru.pelengator.API;

import ru.pelengator.API.utils.ImageUtils;
import ru.pelengator.API.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;


/**
 * Утилиты для работы с картинкой
 */
public class DetectorUtils {

    public static final void capture(Detector detector, File file) {
        if (!detector.isOpen()) {
            detector.open();
        }
        try {
            ImageIO.write(detector.getImage(), ImageUtils.FORMAT_JPG, file);
        } catch (IOException e) {
            throw new DetectorException(e);
        }
    }

    public static final void capture(Detector detector, File file, String format) {
        if (!detector.isOpen()) {
            detector.open();
        }
        try {
            ImageIO.write(detector.getImage(), format, file);
        } catch (IOException e) {
            throw new DetectorException(e);
        }
    }

    public static final void capture(Detector detector, String filename) {
        if (!filename.endsWith(".jpg")) {
            filename = filename + ".jpg";
        }
        capture(detector, new File(filename));
    }

    public static final void capture(Detector detector, String filename, String format) {
        String ext = "." + format.toLowerCase();
        if (!filename.endsWith(ext)) {
            filename = filename + ext;
        }
        capture(detector, new File(filename), format);
    }

    public static final byte[] getImageBytes(Detector detector, String format) {
        return ImageUtils.toByteArray(detector.getImage(), format);
    }

    /**
     * Захват изображения как ByteBuffer.
     *
     * @param detector детектор, с которой должно быть получено изображение
     * @param format формат файла
     * @return Байтовый буфер
     */
    public static final ByteBuffer getImageByteBuffer(Detector detector, String format) {
        return ByteBuffer.wrap(getImageBytes(detector, format));
    }

    /**
     * Получить пакет ресурсов для определенного класса.
     *
     * @param clazz класс, для которого нужно найти пакет ресурсов
     * @param locale объект {@link Locale}
     * @return Пакет ресурсов
     */
    public static final ResourceBundle loadRB(Class<?> clazz, Locale locale) {
        String PACAGE="PATH";
        String pkg = DetectorUtils.class.getPackage().getName().replaceAll("\\.", "/");
        return PropertyResourceBundle.getBundle(String.format("%s"+
                Utils.separator+PACAGE+Utils.separator+"%s", pkg, clazz.getSimpleName()));
    }

}
