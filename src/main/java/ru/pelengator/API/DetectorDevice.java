package ru.pelengator.API;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Абстракция устройства.
 */
public interface DetectorDevice {

    /**
     * Этот интерфейс должен быть реализован всеми детекторами, поддерживающими
     * возможность доступа к необработанным байтам или прямому буферу байтов с устройства.
     *
     * @author
     */
    public static interface BufferAccess {

        /**
         * Чтение базового буфера памяти изображений. Этот метод вернет новый
         * ссылка на предварительно выделенную память вне кучи, где байты изображения
         * хранится. Размер этого буфера равен ширине изображения * высоте * 3 байтам . <br>
         * <br>
         *
         * <b>ПРИМЕЧАНИЕ!</b> <b>Не</b> используйте этот буфер для установки значения байтов. Это
         * следует использовать только для чтения!
         *
         * @return Bytes Буфер
         */
        ByteBuffer getImageBytes();

        /**
         * Скопируйте базовую память изображения в целевой буфер, переданный как
         * аргумент. Оставшаяся емкость целевого буфера должна быть
         * наименьшая ширина изображения * высота * 3 байта.
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

        void setInt(int value);
        void setVOS(int value);
        void setVOS1(int value);
        void setVOS2(int value);

        void setVR0(int value);

        void setССС(boolean value);

        void setDim(boolean value);

        void setPower(boolean value);

        int[][] getFrame();

        void setPause(int pause);

        void setRO(byte ro);

     //  boolean isConnected();
    }


    /**
     * Этот интерфейс может быть реализован устройствами, которые ожидают каких-либо конкретных
     * параметров.
     */
    public static interface Configurable {

        /**
         * Устанавливает параметры устройства. Каждая реализация устройства может принимать свои собственные
         * набор параметров. Все допустимые ключи, типы значений, возможные значения
         * и значения по умолчанию должны быть обоснованно задокументированы разработчиком. Может
         * вызываться до метода open или позже, в зависимости от устройства.
         *
         * @param parameters - Карта параметров, изменяющих настройки устройства по умолчанию
         * @see Detector#setParameters(Map)
         */
        void setParameters(Map<String, ?> parameters);
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
     * Установите новый ожидаемый размер изображения.
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
