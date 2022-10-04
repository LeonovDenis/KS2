package ru.pelengator.API.devises.china;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.*;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.API.driver.usb.Jna2;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class ChinaDevice implements DetectorDevice, DetectorDevice.ChinaSource {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ChinaDevice.class);

    /**
     * Поддерживаемые разрешения.
     */
    private final static Dimension[] DIMENSIONS = new Dimension[]{
            DetectorResolution.CHINA.getSize(),
            DetectorResolution.CHINALOW.getSize(),
    };

    /**
     * Задача на открытие сессии.
     */
    private class StartSessionTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();

        public StartSessionTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS startSession() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Session not started", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.startSession());
        }
    }

    /**
     * Задача на включение детектора.
     */
    private class SetPowerTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicBoolean value = new AtomicBoolean(false);

        public SetPowerTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setPower(boolean value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Power not submitted", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setPower(value.get()));
        }
    }

    /**
     * Задание на получение картинки.
     */
    private class GetDataImageTask extends DetectorTask {

        private final AtomicReference<ByteBuffer> result = new AtomicReference<>();

        /**
         * Получение видео с паузой чтения
         * @param device
         */
        public GetDataImageTask(DetectorDevice device) {
            super(device);
        }

        public ByteBuffer getImage() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("GetImage interapted", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.getImage());
        }
    }

    /**
     * Задание на подчистку буфера.
     */
    private class ClearBufferTask extends DetectorTask {

        public ClearBufferTask(DetectorDevice device) {
            super(device);
        }

        public void clearBuffer() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("ClearBuffer interapted", e);
            }
        }

        @Override
        protected void handle() {
            grabber.clearBuffer();
        }
    }

    /**
     * Задание на закрытие устройства.
     */
    private class CloseTask extends DetectorTask {

        public CloseTask(DetectorDevice device) {
            super(device);
        }

        public void stopSession() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на закрытие прерван", e);
            }
        }

        @Override
        protected void handle() {
            grabber.stopSession();
        }
    }

    /**
     * Задание на установку времени интегрирования.
     */
    private class SetIntTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetIntTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setInt(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Set Int time interapted", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setIntTime(value.get()));
        }
    }

    /**
     * Задание на установку vos/скимминга.
     */
    private class SetVOSTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetVOSTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setVOS(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Set VOS interapted", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVVA(value.get()));
        }
    }

    /**
     * Задача на поставку референса.
     */
    private class SetReferenceTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetReferenceTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setReference(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Reference not setted", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setREF(value.get()));
        }
    }

    /**
     * Задача на установку смещения на фотодиодах.
     */
    private class SetVR0Task extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetVR0Task(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setVR0(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Error while setting VR0", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVR0(value.get()));
        }
    }

    /**
     * Задача на смену разрешения.
     */
    private class SetDimemsionTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicBoolean value = new AtomicBoolean(false);

        public SetDimemsionTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setDimension(boolean value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Error while setting dimension", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setDimension(value.get()));
        }
    }

    /**
     * Задача на смену ёмкостей.
     */
    private class SetCapasityTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicBoolean value = new AtomicBoolean(false);

        public SetCapasityTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setCapacity(boolean value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Error while setting capacity", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setCapacity(value.get()));
        }
    }


    /**
     * Смещение RGB.
     */
    private static final int[] BAND_OFFSETS = new int[]{0, 1, 2};

    /**
     * Количество битов в каждом пикселе.
     */
    private static final int[] BITS = {8, 8, 8};

    /**
     * Смещение изображения.
     */
    private static final int[] OFFSET = new int[]{0};

    /**
     * Тип данных, используемых в изображении.
     */
    private static final int DATA_TYPE = DataBuffer.TYPE_BYTE;

    /**
     * Цветовое пространство изображения.
     */
    private static final ColorSpace COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    /**
     * Буффер сырых данных.
     */
    private AtomicReference<int[][]> BANK = new AtomicReference<>();
    /**
     * Флаг готовности буфера.
     */
    private AtomicBoolean flafGrabFrames = new AtomicBoolean(false);
    /***
     * Ссылка на граббер/драйвер.
     */
    private Jna2 grabber = null;
    /**
     * Разрешение.
     */
    private Dimension size = null;
    /**
     * Моделька для картинки.
     */
    private ComponentSampleModel smodel = null;
    /**
     * Моделька для картинки.
     */
    private ColorModel cmodel = null;
    /**
     * Флаг несовпадения разрешений.
     */
    private boolean failOnSizeMismatch = false;
    /**
     * Флаг удаления.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    /**
     * Флаг открытия.
     */
    private final AtomicBoolean open = new AtomicBoolean(false);
    /**
     * Имя устройства.
     */
    private String name = null;
    /**
     * ID устройства.
     */

    private String id = null;
    /**
     * Полное название.
     */
    private String fullname = null;
    /**
     * Таймеры для подсчета FPS.
     */
    private long t1 = -1;
    private long t2 = -1;

    /**
     * Текущий FPS.
     */
    private volatile double fps = 0;

    /**
     * Конструктор
     *
     * @param name    Имя
     * @param id      ID
     * @param grabber драйвер
     */
    public ChinaDevice(String name, String id, Jna2 grabber) {
        this.name = name;
        this.id = id;
        this.grabber = grabber;
        this.fullname = String.format("%s %s", this.name, this.id);
    }

    @Override
    public String getName() {
        return fullname;
    }

    public String getDeviceName() {
        return name;
    }

    public String getDeviceId() {
        return id;
    }

    @Override
    public Dimension[] getResolutions() {
        return DIMENSIONS;
    }

    @Override
    public Dimension getResolution() {
        if (size == null) {
            size = getResolutions()[0];
        }
        return size;
    }

    @Override
    public void setResolution(Dimension size) {

        if (size == null) {
            throw new IllegalArgumentException("Dimension cant be null");
        }

        if (open.get()) {
            throw new IllegalStateException("Cant change dimension, 1st close it");
        }

        this.size = size;
    }

    /**
     * Помещение данных в буфер.
     *
     * @param tempData входные данные
     */
    private void grabFrames(int[][] tempData) {

        if (flafGrabFrames.compareAndSet(false, true)) {
            BANK.set(tempData);

        }
    }

    /**
     * Получение картинки с пикселем на 3 байта и реверсом по оси Y
     *
     * @return
     */
    @Override
    public BufferedImage getImage() {

        ByteBuffer buffer = getImageBytes();
        if (buffer == null) {
            return null;
        }
        byte[] bytes = new byte[size.width * size.height * 3];
        byte[][] data = new byte[][]{bytes};
        byte[] array = buffer.array();
        if (smodel.getHeight() != size.height || smodel.getWidth() != size.width) {
            return null;
        }
        try {
            copyWith1Byte(array, bytes);
        } catch (InterruptedException e) {
            return null;
        }
        DataBufferByte dbuf = new DataBufferByte(data, bytes.length, OFFSET);
        WritableRaster raster = Raster.createWritableRaster(smodel, dbuf, null);
        BufferedImage bi = new BufferedImage(cmodel, raster, false, null);
        reverseImage(bi, size);

        return bi;
    }

    /**
     * Зеркалирование по оси Y.
     *
     * @param src  исходное изображение
     * @param size разрешение
     */
    private void reverseImage(BufferedImage src, Dimension size) {

        int width = size.width;
        int height = size.height;
        int[][] tempData = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                try {
                    tempData[height - 1 - y][x] = src.getRGB(x, y) & 0xffffff;
                } catch (Exception e) {
                    LOG.error("Error while reverse Y");
                }

            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                src.setRGB(x, y, tempData[y][x]);
            }
        }
        grabFrames(tempData);
    }

    /**
     * Расширение цвета пикселя на 3 байта
     *
     * @param src    исходный массив по 2 байта на пиксель
     * @param target конечный массив по 3 байта на пиксель
     * @throws InterruptedException ошибка при расширении
     */
    private void copyWith1Byte(byte[] src, byte[] target) throws InterruptedException {
        LinkedList<Byte> aSrc = new LinkedList<Byte>();
        LinkedList<Byte> aTarget = new LinkedList<Byte>();
        for (int i = 0; i < src.length; i++) {
            aSrc.add(Byte.valueOf(src[i]));
        }
        for (int i = 1; i < target.length + 1; i++) {
            if (i % 3 == 0) {
                aTarget.add(aSrc.poll());
            } else if (i % 3 == 1) {
                aTarget.add((byte) 0x00);
            } else if (i % 3 == 2) {
                aTarget.add(aSrc.poll());
            }
        }
        for (int i = 0; i < target.length; i++) {
            try {
                target[i] = aTarget.poll();
            } catch (Exception e) {
                LOG.error("Dimensions not dont match");
                throw new InterruptedException("Dimensions not dont match");
            }
        }
    }

    /**
     * Получение картинки
     *
     * @return байтовый буфер
     */
    public ByteBuffer getImageBytes() {
        if (disposed.get()) {
            LOG.error("Detector disposed, image null");
            return null;
        }
        if (!open.get()) {
            LOG.error("Detector closed, image null");
            return null;
        }
        ByteBuffer dataImage = new GetDataImageTask(this).getImage();
        if (dataImage == null) {
            return null;
        }
        return dataImage;
    }

    /**
     * Открытие устройства
     */
    @Override
    public void open() {
        if (disposed.get()) {
            return;
        }
        LOG.trace("Opening detector: {}", getName());
        if (size == null) {
            size = getResolutions()[0];
        }
        if (size == null) {
            throw new RuntimeException("Dimension cant be null");
        }
        LOG.trace("Detector {} starting, dimension {}", fullname, size);

        boolean started = startSession(size.width, size.height, 50);

        if (!started) {
            throw new DetectorException("Cant start JNA!");
        }
        Dimension size2 = new Dimension(grabber.getWidth(), grabber.getHeight());
        int w1 = size.width;
        int w2 = size2.width;
        int h1 = size.height;
        int h2 = size2.height;

        if (w1 != w2 || h1 != h2) {
            if (failOnSizeMismatch) {
                throw new DetectorException(String.format("Dimensions various - [%dx%d] vs [%dx%d]", w1, h1, w2, h2));
            }
            Object[] args = new Object[]{w1, h1, w2, h2, w2, h2};
            LOG.trace("Dimensions various - [{}x{}] vs [{}x{}]. Setting correct. New Dimension [{}x{}]", args);

            size = new Dimension(w2, h2);
        }

        smodel = new ComponentSampleModel(DATA_TYPE, size.width, size.height, 3, size.width * 3, BAND_OFFSETS);
        cmodel = new ComponentColorModel(COLOR_SPACE, BITS, false, false, Transparency.OPAQUE, DATA_TYPE);

        LOG.trace("Clearing buffer");

        clearMemoryBuffer();

        LOG.trace("Detector {} opened", this.fullname);

        open.set(true);
    }

    /**
     * Очистка кадра на устройстве.
     */
    private void clearMemoryBuffer() {
        new ClearBufferTask(this).clearBuffer();
    }

    /**
     * Закрытие
     */
    @Override
    public void close() {

        if (!open.compareAndSet(true, false)) {
            return;
        }

        LOG.trace("Closing detector");

        new SetPowerTask(this).setPower(false);

        new CloseTask(this).stopSession();
    }

    /**
     * Удаление детектора
     */
    @Override
    public void dispose() {

        if (!disposed.compareAndSet(false, true)) {
            return;
        }
        LOG.trace("Releasing resource {}", getName());

        close();
    }

    /**
     * Определяет должно ли устройство выйти из строя при несовпадении разрешений
     *
     * @param fail флаг сбоя при несоответствии размера, true или false
     */
    public void setFailOnSizeMismatch(boolean fail) {
        this.failOnSizeMismatch = fail;
    }

    /**
     * Определяет открыто ли устройство
     *
     * @return
     */
    @Override
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Запуск сессии детектора
     *
     * @param width             ширина кадра
     * @param height            высото кадра
     * @param reqMillisPerFrame задержка между кадрами
     * @return true - в случае удачного старта, false - при неудаче
     */
    private boolean startSession(int width, int height, int reqMillisPerFrame) {

        LOG.trace("Start session");
        FT_STATUS ft_status = new StartSessionTask(this).startSession();
        if (ft_status != FT_STATUS.FT_OK) {
            LOG.error("Error in startSession {}", ft_status);
        }else{
            LOG.trace("Session start normal {}", ft_status);
        }
        return true;
    }

    @Override
    public FT_STATUS setInt(int value) {
        FT_STATUS result = new SetIntTask(this).setInt(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setInt[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setVOS(int value) {
        FT_STATUS result = new SetVOSTask(this).setVOS(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVOS[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setReference(int value) {
        FT_STATUS result = new SetReferenceTask(this).setReference(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setReference[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setVR0(int value) {
        FT_STATUS result = new SetVR0Task(this).setVR0(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVR0[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setССС(boolean value) {
        FT_STATUS result = new SetCapasityTask(this).setCapacity(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setССС[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setDim(boolean value) {
        if (value) {
            size = DIMENSIONS[0];
        } else {
            size = DIMENSIONS[1];
        }
        smodel = new ComponentSampleModel(DATA_TYPE, size.width, size.height, 3, size.width * 3, BAND_OFFSETS);
        FT_STATUS result = new SetDimemsionTask(this).setDimension(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setDim[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public FT_STATUS setPower(boolean value) {
        FT_STATUS result = null;
        result = new SetPowerTask(this).setPower(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setPower[{}]: {}", value, result);
        }
        return result;
    }

    @Override
    public int[][] getFrame() {
        if (flafGrabFrames.compareAndSet(true, false)) {
            return BANK.get();
        }
        return null;
    }

    @Override
    public boolean isOnline() {
        return grabber.getValidHendler().get();
    }


}
