package ru.pelengator.API.buildin.china;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.*;
import ru.pelengator.driver.FT_STATUS;
import ru.pelengator.driver.usb.Jna2;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class ChinaDevice implements DetectorDevice, Runnable, DetectorDevice.Configurable, DetectorDevice.ChinaSource {

    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(ChinaDevice.class);

    /**
     * Поддерживаемые разрешения
     */
    private final static Dimension[] DIMENSIONS = new Dimension[]{
            DetectorResolution.CHINA.getSize(),
            DetectorResolution.CHINALOW.getSize(),
    };

    static volatile long PAUSE = 50;

    private class CreateHendlerTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();

        public CreateHendlerTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS create() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на создание обработчика прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.create());
            LOG.trace("Результат по запросу на создание обработчика {}", result.get());
        }
    }

    private class ChengeParamTask extends DetectorTask {

        private final AtomicReference<Map<String, ?>> result = new AtomicReference<Map<String, ?>>();

        public ChengeParamTask(DetectorDevice device) {
            super(device);
        }

        public void setParameters(Map<String, ?> parameters) {
            result.set(parameters);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на обновление параметров прерван", e);
            }

        }

        @Override
        protected void handle() {
            grabber.setParameters(result.get());
            LOG.trace("Результат на обновление параметров ");
        }
    }


    private class SetIDTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();

        public SetIDTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setID() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на установку ID прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setID());
            LOG.trace("Результат по установке ID {}", result.get());
        }
    }

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
                LOG.error("Запрос на включение питания прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setPower(value.get()));
            LOG.trace("Результат по включению питания [{}] {}", value.get(), result.get());
        }
    }

    private class SetResetTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicBoolean value = new AtomicBoolean(false);

        public SetResetTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setReset(boolean value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на ресет прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setReset(value.get()));
            LOG.trace("Результат по ресету [{}] {}", value.get(), result.get());
        }
    }

    private class SetRETask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicBoolean value = new AtomicBoolean(false);

        public SetRETask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setRE(boolean value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на RE прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setRE(value.get()));
            LOG.trace("Результат по RE [{}] {}", value.get(), result.get());
        }
    }


    private class GetDataImageTask extends DetectorTask {

        private final AtomicReference<ByteBuffer> result = new AtomicReference<>();

        public GetDataImageTask(DetectorDevice device) {
            super(device);
        }

        public ByteBuffer getImage() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на получение буфераданных кадра прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            try {
                TimeUnit.MILLISECONDS.sleep(PAUSE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            result.set(grabber.getImage());
        }
    }

    private class ClearBufferTask extends DetectorTask {

        public ClearBufferTask(DetectorDevice device) {
            super(device);
        }

        public void clearBuffer() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на очистку буфера прерван", e);
            }
        }

        @Override
        protected void handle() {
            grabber.clearBuffer();
        }
    }

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
                LOG.error("Запрос на установку инта прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setIntTime(value.get()));
            LOG.trace("Результат по установке инта [{}] {}", value.get(), result.get());
        }
    }

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
                LOG.error("Запрос на установку VOS прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVVA(value.get()));
            LOG.trace("Результат по установке VOS [{}] {}", value.get(), result.get());
        }
    }

    private class SetVOS1Task extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetVOS1Task(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setVOS1(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на установку VOS1 прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVVA1(value.get()));
            LOG.trace("Результат по установке VOS1 [{}] {}", value.get(), result.get());
        }
    }

    private class SetVOS2Task extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetVOS2Task(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setVOS2(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на установку VOS2 прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVVA2(value.get()));
            LOG.trace("Результат по установке VOS2 [{}] {}", value.get(), result.get());
        }
    }


    private class SetVR0Task extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicInteger value = new AtomicInteger(0);

        public SetVR0Task(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setInt(int value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на установку VR0 прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setVR0(value.get()));
            LOG.trace("Результат по установке VR0 [{}] {}", value.get(), result.get());
        }
    }

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
                LOG.error("Запрос на смену разрешения прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setDimension(value.get()));
            LOG.trace("Результат по смену разрешения [{}] {}", value.get(), result.get());
        }
    }

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
                LOG.error("Запрос на смену ёмности прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setCapacity(value.get()));
            LOG.trace("Результат по смену ёмности [{}] {}", value.get(), result.get());
        }
    }

    private class SetRoTask extends DetectorTask {

        private final AtomicReference<FT_STATUS> result = new AtomicReference<FT_STATUS>();
        private final AtomicReference<Byte> value = new AtomicReference<>();

        public SetRoTask(DetectorDevice device) {
            super(device);
        }

        public FT_STATUS setRo(byte value) {
            this.value.set(value);
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Запрос на установку Ro прерван", e);
            }
            return result.get();
        }

        @Override
        protected void handle() {
            result.set(grabber.setRo(value.get()));
            LOG.trace("Результат по установке Ro [{}] {}", value.get(), result.get());
        }
    }


    //todo
    //todo
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
     * Тип данных, используемый в изображении.
     */
    private static final int DATA_TYPE = DataBuffer.TYPE_BYTE;

    /**
     * Цветовое пространство изображения.
     */
    private static final ColorSpace COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    private AtomicReference<int[][]> BANK = new AtomicReference<>();

    private AtomicBoolean flafGrabFrames = new AtomicBoolean(false);

   // private AtomicBoolean flagConnected = new AtomicBoolean(false);

    private Jna2 grabber = null;
    private Dimension size = null;
    private ComponentSampleModel smodel = null;
    private ColorModel cmodel = null;
    private boolean failOnSizeMismatch = false;

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean open = new AtomicBoolean(false);

    private String name = null;
    private String id = null;
    private String fullname = null;

    private long t1 = -1;
    private long t2 = -1;

    /**
     * Текущий FPS.
     */
    private volatile double fps = 0;

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
            throw new IllegalArgumentException("Разрешение не может быть нулевым");
        }

        if (open.get()) {
            throw new IllegalStateException("Невозможно поменять разрешение детектора, сначала необходимо его закрыть");
        }

        this.size = size;
    }

    private void grabFrames(int[][] tempData) {

        if (flafGrabFrames.compareAndSet(false, true)) {
            BANK.set(tempData);

        }
    }

    @Override
    public BufferedImage getImage() {

        ByteBuffer buffer = getImageBytes();

        if (buffer == null) {
      //      flagConnected.set(false);
            return null;
        }

    //    flagConnected.compareAndSet(false, true);
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

    private void reverseImage(BufferedImage src, Dimension size) {

        int width = size.width;
        int height = size.height;
        int[][] tempData = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                try {
                    tempData[height - 1 - y][x] = src.getRGB(x, y) & 0xffffff;
                } catch (Exception e) {
                    System.out.println("Ошибка  в реверсе");
                }

            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                src.setRGB(x, y, tempData[y][x]);
            }
        }
        // System.out.println("//");
        // System.out.println(Arrays.toString(tempData[2]));
        //  System.out.println("///");
        //  System.out.println(Arrays.toString(tempData[126]));

        grabFrames(tempData);

    }

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
                System.out.println("Выход по ошибке. Блок и программа с разными пикселями");
                throw new InterruptedException("Выход по ошибке. Блок и программа с разными пикселями");
            }


        }
    }


    public ByteBuffer getImageBytes() {
        if (disposed.get()) {
            LOG.error("Детектор освобожден, картинка будет null");
            return null;
        }
        if (!open.get()) {
            LOG.error("Детектор закрыт, картинка будет null");
            return null;
        }
        ByteBuffer dataImage = new GetDataImageTask(this).getImage();
        if (dataImage == null) {
            return null;
        }
        return dataImage;
    }


    @Override
    public void open() {
        if (disposed.get()) {
            return;
        }
        LOG.trace("Открываю устройство: {}", getName());

        if (size == null) {
            size = getResolutions()[0];
        }
        if (size == null) {
            throw new RuntimeException("Разрешение не может быть null");
        }

        LOG.trace("Устройство {} стартует, размер {}", fullname, size);

        LOG.trace("Устройство стартовало");
        boolean started = startSession(size.width, size.height, 50);

        if (!started) {
            throw new DetectorException("Невозможно стартонуть сишную библиотеку!");
        }

        Dimension size2 = new Dimension(grabber.getWidth(), grabber.getHeight());

        int w1 = size.width;
        int w2 = size2.width;
        int h1 = size.height;
        int h2 = size2.height;

        if (w1 != w2 || h1 != h2) {

            if (failOnSizeMismatch) {
                throw new DetectorException(String.format("Различие в размере полученном и запрошенном - [%dx%d] vs [%dx%d]", w1, h1, w2, h2));
            }

            Object[] args = new Object[]{w1, h1, w2, h2, w2, h2};
            LOG.trace("Различие в разрешениях полученном и запрошенном - [{}x{}] vs [{}x{}]. Установка корректного. Новое разрешение [{}x{}]", args);

            size = new Dimension(w2, h2);
        }

        smodel = new ComponentSampleModel(DATA_TYPE, size.width, size.height, 3, size.width * 3, BAND_OFFSETS);
        cmodel = new ComponentColorModel(COLOR_SPACE, BITS, false, false, Transparency.OPAQUE, DATA_TYPE);

        LOG.trace("Очистка буфера");

        clearMemoryBuffer();

        LOG.trace("Устройство {} открыто", this);

        open.set(true);
    }

    /**
     * Очистка кадра на устройстве
     */
    private void clearMemoryBuffer() {
        new ClearBufferTask(this).clearBuffer();
    }

    @Override
    public void close() {

        if (!open.compareAndSet(true, false)) {
            return;
        }

        LOG.trace("Закрытие устройства");

        new CloseTask(this).stopSession();
    }

    @Override
    public void dispose() {

        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        LOG.trace("Освобождение ресурсов устройства {}", getName());

        close();
    }

    /**
     * Определяет, должно ли устройство выйти из строя, если запрошенный размер изображения отличается от фактического
     *
     * @param fail флаг сбоя при несоответствии размера, true или false
     */
    public void setFailOnSizeMismatch(boolean fail) {
        this.failOnSizeMismatch = fail;
    }

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

        LOG.debug("Запуск сессии");
        FT_STATUS result = new CreateHendlerTask(this).create();

        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while create hendler: {}", result);
            return false;
        } else {
            /**
             * установка ID
             */
            result = new SetIDTask(this).setID();

            if (result != FT_STATUS.FT_OK) {
                LOG.error("Error while setID: {}", result);
                return false;
            } else {
                return true;
            }
        }

    }

    @Override
    public void run() {
    }

    @Override
    public void setParameters(Map<String, ?> parameters) {
        new ChengeParamTask(this).setParameters(parameters);
    }


    @Override
    public void setPause(int pause) {

        PAUSE = pause;
    }

    @Override
    public void setRO(byte value) {
        FT_STATUS result = new SetRoTask(this).setRo(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setRo[{}]: {}", value, result);
        }
    }
/**
    @Override
    public boolean isConnected() {

        return flagConnected.get();
    }
*/
    @Override
    public void setInt(int value) {
        FT_STATUS result = new SetIntTask(this).setInt(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setInt[{}]: {}", value, result);
        }
    }

    @Override
    public void setVOS(int value) {
        FT_STATUS result = new SetVOSTask(this).setVOS(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVOS[{}]: {}", value, result);
        }
    }

    @Override
    public void setVOS1(int value) {
        FT_STATUS result = new SetVOS1Task(this).setVOS1(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVOS1[{}]: {}", value, result);
        }
    }

    @Override
    public void setVOS2(int value) {
        FT_STATUS result = new SetVOS2Task(this).setVOS2(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVOS2[{}]: {}", value, result);
        }
    }

    @Override
    public void setVR0(int value) {
        FT_STATUS result = new SetVR0Task(this).setInt(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setVR0[{}]: {}", value, result);
        }
    }

    @Override
    public void setССС(boolean value) {
        FT_STATUS result = new SetCapasityTask(this).setCapacity(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setССС[{}]: {}", value, result);
        }
    }

    @Override
    public void setDim(boolean value) {
        if (value) {
            size = DIMENSIONS[0];

        } else {
            size = DIMENSIONS[1];

        }
        FT_STATUS result = new SetDimemsionTask(this).setDimension(value);
        if (result != FT_STATUS.FT_OK) {
            LOG.error("Error while setDim[{}]: {}", value, result);
        }
    }

    @Override
    public void setPower(boolean value) {
    //    if (flagConnected.get() != value) {
            FT_STATUS result = new SetPowerTask(this).setPower(value);
            if (result != FT_STATUS.FT_OK) {
                LOG.error("Error while setPower[{}]: {}", value, result);
            }
      //  }
    }

    @Override
    public int[][] getFrame() {
        if (flafGrabFrames.compareAndSet(true, false)) {
            return BANK.get();
        }
        return null;
    }


}
