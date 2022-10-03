package ru.pelengator.API.driver.usb;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.bytes.BytesTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.driver.FT_STATUS;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ComList {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ComList.class);

    private Jna2 grabber;

    public ComList(Jna2 grabber) {
        this.grabber = grabber;
    }

    private static volatile AtomicBoolean flag_frame_ready = new AtomicBoolean(false);

    private static volatile LinkedList<Bytes> bufferVideoParts = new LinkedList<Bytes>();

    private static volatile LinkedList<Bytes> currentFrames = new LinkedList<Bytes>();


    /**
     * Флаг тестирования.
     */
    private boolean isTest = false;
    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////методы управления
    byte DEV_ID = 0x05;
    Bytes HEADER = Bytes.from((byte) 0xA2, DEV_ID);
    byte[] SETPOWER = new byte[]{(byte) 0x02, (byte) 0x08};
    byte[] SETRESET = new byte[]{(byte) 0x02, (byte) 0x04};
    byte[] SETRE = new byte[]{(byte) 0x05, (byte) 0x1B};
    byte[] SETRO = new byte[]{(byte) 0x05, (byte) 0x1A};
    byte[] SETINT = new byte[]{(byte) 0x05, (byte) 0x00};
    byte[] VIDEOHEADER = new byte[]{(byte) 0x28};
    byte[] SETVVA = new byte[]{(byte) 0x05, (byte) 0x01};

    byte[] SETVVA1 = new byte[]{(byte) 0x05, (byte) 0x02};
    byte[] SETVVA2 = new byte[]{(byte) 0x05, (byte) 0x03};

    byte[] SETVR0 = new byte[]{(byte) 0x05, (byte) 0x23};
    byte[] SETID = new byte[]{(byte) 0x05, (byte) 0x09};
    byte[] SETDIM = new byte[]{(byte) 0x05, (byte) 0x1C};
    byte[] SETCCC = new byte[]{(byte) 0x05, (byte) 0x22};

    /**
     * Установка разрешения.
     *
     * @param set - 0xFF -128*128 (true); 0x00 - 92*90
     * @return
     */
    public FT_STATUS setDimension(boolean set) {
        byte data;
        if (set) {
            data = (byte) 0xFF;//128*128
        } else {
            data = (byte) 0x00;//92*90
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETDIM[0])      //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETDIM[1])      //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;

    }

    /**
     * Установка коэф. усиления.
     *
     * @param set - 0xFF -3 (true); 0x00 - 1
     * @return
     */
    public FT_STATUS setCapacity(boolean set) {
        byte data;
        if (set) {
            data = (byte) 0xFF;//3
        } else {
            data = (byte) 0x00;//1
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETCCC[0])      //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETCCC[1])      //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;

    }


    /**
     * Подача питания VDD, VDDA.
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setPower(boolean set) {

        byte data;
        if (set) {
            data = (byte) 0xFF;//on
        } else {
            data = (byte) 0x00;//off
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETPOWER[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETPOWER[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка времени интегрирования.
     * @param time
     * @return
     */
    public FT_STATUS setIntTime(int time) {

        byte[] data = Bytes.from(time).resize(2).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETINT[0])    //функция
                .append((byte) 0x03)  //размер[команда+данные]||
                .append(SETINT[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка напр. смещения.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVR0(int value) {

        byte data = Bytes.from(value).resize(1).toByte();
        Bytes msg = HEADER            //маска+ID
                .append(SETVR0[0])    //функция
                .append((byte) 0x02)  //размер[команда+данные]||
                .append(SETVR0[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка напр. VOS скимминга.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA(int value) {

        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;
    }

    /**
     * Установка рефов1.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA1(int value) {

        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER             //маска+ID
                .append(SETVVA1[0])    //функция
                .append((byte) 0x05)   //размер[команда+данные]||
                .append(SETVVA1[1])    //команда               |
                .append(data);         //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка id устройства.
     *
     * @return
     */
    public FT_STATUS setID() {

        Bytes msg = HEADER           //маска+ID
                .append(SETID[0])    //функция
                .append((byte) 0x02) //размер[команда+данные]||
                .append(SETID[1])    //команда               |
                .append(DEV_ID);     //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        LOG.info("ID setted {}", ft_status);

        return ft_status;
    }

    /**
     * Чтение данных сырых данных.
     *
     * @return
     */
    public Bytes readData() {
        Bytes bytes = grabber.readPipe();

        if (isTest) {
            bytes = testData(bytes);
        }

        return bytes;
    }

    /**
     * Запросполногокадра.
     * @return
     */
    public Bytes nextFrame() {

        Bytes bytesData = readData();
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //проверка на пустой массив
        if (!grabber.getValidHendler().get()) {
            LOG.trace("Exitig. Closing detector. Hendler not valid",bytesData);
            grabber.close();
            return null;
        }
       // LOG.trace("Parsing frame {}",bytesData);
        /**
         * Есть заголовок?
         */
        if (bytesData.startsWith(HEADER.array())) {

            /**
             * Заголовок есть.
             */
            /**
             * Считываем номер функции.
             */
            byte function = bytesData.byteAt(2);// читаем номер функции

            switch (function) {
                //обработка видео
                case 0x28://Пишем первую часть кадра
                    int fsize = parse1stOartOfFrame(bytesData);
                    int tempSize = 0;
                    do {
                        try {
                            TimeUnit.MILLISECONDS.sleep(5);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                        //повторяем чтение до целого кадра
                        Bytes btData = readData();
                      //выход при обрыве данных
                        if (btData.isEmpty()) {
                            //Выход сюда, когда нет связи.
                            LOG.error("Выкод в середине цикла. когда пропала связь!");
                            grabber.close();
                            clearBuffer();
                            return null;
                        }
                        tempSize = parse2bdOartOfFrame(btData);
                    } while ((fsize = (fsize - tempSize)) > 0);

                    Bytes bytes = summPartsOfFrame();
                    if (bytes.length() < (2 * grabber.getHeight() * grabber.getWidth())) {
                        LOG.trace("Dimension not same!");
                        return null;
                    }
                    return bytes;
                case 0x00://установка ID
                case 0x02://установка питания
                case 0x05://установка какого-либо параметра
                    LOG.trace("Answer " + bytesData);
                    grabber.getNeedToWaite().set(false);
                    return null;

                default:
                    LOG.trace("Answer" + bytesData);
                    return null;
            }
        } else {
            /**
             * Заголовка нет
             */
           //0x008000000 подтверждение ID
            if(bytesData.length()==4){
                LOG.trace("Answer on Set ID " + bytesData);
                grabber.getNeedToWaite().set(false);
            }

            LOG.trace("Answer without header " + bytesData);
            return null;
        }
    }

    /**
     * Обрабатывает составные части кадра.
     *
     * @param bytesData
     */
    private int parse2bdOartOfFrame(Bytes bytesData) {
        if (bufferVideoParts.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        if (bytesData.isEmpty()) {
            return 0;
        }
        int length = bytesData.length();
        bytesData = reversBytes(bytesData);
        bufferVideoParts.add(bytesData);
        return length / 2;
    }

    /**
     * Обрабатывает первую часть кадра.
     *
     * @param bytesData
     */
    private int parse1stOartOfFrame(Bytes bytesData) {
        int headers = 1 + 1 + 1 + 3;

        byte[] size = new byte[3];
        for (int i = 0; i < 3; i++) {
            size[i] = bytesData.byteAt(3 + i);
        }
        int frameSize = Bytes.wrap(size).reverse().resize(4).toInt();

        int box_length = bytesData.length();
        Bytes resized = bytesData.resize(box_length - headers);
        resized = reversBytes(resized);
        bufferVideoParts.add(resized);
        return frameSize - (resized.length()) / 2;
    }

    /**
     * Разворачивает байты.
     *
     * @param bytes
     * @return
     */
    private Bytes reversBytes(Bytes bytes) {

        int length = bytes.length();
        byte[] bytesArray = new byte[length];

        for (int i = 0; i < length; i = i + 2) {
            if (i == length) {
                bytesArray[i] = bytes.byteAt(i - 1);
                bytesArray[i + 1] = bytes.byteAt(i);
            } else {
                bytesArray[i] = bytes.byteAt(i + 1);
                bytesArray[i + 1] = bytes.byteAt(i);
            }
        }
        return Bytes.wrap(bytesArray);
    }

    /**
     * Суммирование частей кадра.
     * @return
     */
    private Bytes summPartsOfFrame() {
        if (!bufferVideoParts.isEmpty()) {

            Bytes fullFrame = Bytes.empty();
            int size = bufferVideoParts.size();
            for (int i = 0; i < size; i++) {
                fullFrame = fullFrame.append(bufferVideoParts.poll());
            }
            return fullFrame;
        } else {
        }
        return null;
    }

    /**
     * Очистка буфера.
     */
    public void clearBuffer() {
        LOG.debug("Clearing buffer. size {}", bufferVideoParts.size());
        bufferVideoParts.clear();

    }
    /**
     * Тестовые данные.
     *
     * @param bytes
     * @return
     */
    private static int count = 0;

    /**
     * Тестовые данные.
     * @param bytes
     * @return
     */
    private Bytes testData(Bytes bytes) {
        if (count == 0) {
            Bytes temp = bytes.append(HEADER).append(VIDEOHEADER);
            //16384 значения
            Bytes length = Bytes.from((int) 16384).reverse().resize(3, BytesTransformer.ResizeTransformer.Mode.RESIZE_KEEP_FROM_ZERO_INDEX);
            bytes = temp.append(length);
            for (int k = 0; k < 8192; k++) {
                if (k == 8191) {
                    k = 8192 * 2;
                }
                Bytes temp_k = Bytes.from((int) k).reverse().resize(2, BytesTransformer.ResizeTransformer.Mode.RESIZE_KEEP_FROM_ZERO_INDEX);
                bytes = bytes.append(temp_k);
            }
        } else if (count == 1) {
            for (int k = 0; k < (8192); k++) {
                Bytes temp_k = Bytes.from((int) (Math.random() * 12000)/*8192+ k*/).reverse().resize(2, BytesTransformer.ResizeTransformer.Mode.RESIZE_KEEP_FROM_ZERO_INDEX);
                bytes = bytes.append(temp_k);
            }
        } else {
            count = -1;
            bytes = bytes.append(HEADER).append((byte) 0x05).append((byte) 0x00);
        }
        count++;
        return bytes;
    }
}
