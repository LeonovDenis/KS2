package ru.pelengator.API;

import ru.pelengator.API.driver.FT_STATUS;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Абстракция устройства.
 */
public interface DetectorDevice {

    /**
     * Этот интерфейс должен быть реализован всеми детекторами.
     */
    public static interface BufferAccess {

        /**
         * Чтение базового буфера памяти изображений.
         * Этот метод вернет новыйбайтовый буфер, где байты изображения хранится.
         * Размер этого буфера равен ширине изображения * высоте * 3 байтам .
         *
         * @return Буфер
         */
        ByteBuffer getImageBytes();

        /**
         * Скопировать базовую память изображения в целевой буфер, переданный как аргумент.
         * Емкость целевого буфера должна быть ширина изображения * высота * 3 байта.
         *
         * @param target буфер, в который должны быть скопированы данные изображения
         */
        void getImageBytes(ByteBuffer target);

    }

    public static interface FPSSource {

        /**
         * Получить текущий FPS устройства.
         *
         * @return FPS
         */
        double getFPS();

    }

    /**
     * Методы китайского детектора
     */
    public static interface ChinaSource {

        FT_STATUS setInt(int value);

        FT_STATUS setVOS(int value);

        FT_STATUS setReference(int value);

        FT_STATUS setVR0(int value);

        FT_STATUS setССС(boolean value);

        FT_STATUS setDim(boolean value);

        FT_STATUS setPower(boolean value);

        FT_STATUS setID();
        FT_STATUS setID(byte[] data);

        int[][] getFrame();

        boolean isOnline();
    }


    /**
     * Этот интерфейс может быть реализован устройствами, которые ожидают каких-либо конкретных
     * параметров.
     */
    public static interface Configurable {

        /**
         * Устанавливает параметры устройства.
         * Каждая реализация устройства может принимать свои собственные наборы параметров.
         * Может вызываться до метода <b>open</b> или позже.
         *
         * @param parameters Карта параметров, изменяющих настройки устройства по умолчанию
         * @see Detector#setParameters(Map)
         */
        FT_STATUS setParameters(Map<String, ?> parameters);
    }

    /**
     * Получить имя устройства.
     *
     * @return Имя устройства
     */
    String getName();

    /**
     * Получить список всех возможных разрешений изображения.
     *
     * @return Возможные решения
     */
    Dimension[] getResolutions();

    /**
     * Получить установленный размер изображения.
     *
     * @return Размер, установленный в данный момент
     */
    Dimension getResolution();

    /**
     * Установить новый размер изображения.
     *
     * @param size устанавливаемый размер
     */
    void setResolution(Dimension size);

    /**
     * Получить изображение с детектора.
     *
     * @return изображение
     */
    BufferedImage getImage();

    /**
     * Открыть устройство, можно закрыть в любое время.
     */
    void open();

    /**
     * Закрытье устройство, однако его можно снова открыть.
     */
    void close();

    /**
     * Удалить устройство. После того, как устройство удалено, его нельзя открыть снова.
     */
    void dispose();

    /**
     * Открыт ли детектор?
     *
     * @return True, если детектор открыт, иначе false
     */
    boolean isOpen();
}
