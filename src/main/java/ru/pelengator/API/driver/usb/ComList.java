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
     * Установка разрешения
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
                .append(SETDIM[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETDIM[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;

    }

    /**
     * Установка коэф. усиления
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
                .append(SETCCC[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETCCC[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;

    }

    /**
     * Подача питания RE
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setRE(boolean set) {
        byte data;
        if (set) {
            data = (byte) 0xFF;//on
        } else {
            data = (byte) 0x00;//off
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETRE[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETRE[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;

    }

    /**
     * Подача питания VDD, VDDA
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
     * Установка напр. смещения
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
     * Установка направления сканирования
     *
     * @param value - byte
     * @return
     */
    public FT_STATUS setRo(byte value) {

        Bytes msg = HEADER            //маска+ID
                .append(SETRO[0])    //функция
                .append((byte) 0x02)  //размер[команда+данные]||
                .append(SETRO[1])    //команда               |
                .append(value);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);
        return ft_status;
    }


    /**
     * Установка напр. антиблюминга
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
     * Установка рефов1
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA1(int value) {

        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA1[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA1[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка рефов2
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA2(int value) {

        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA2[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA2[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Сброс устройства
     *
     * @param isReset - true - ресет
     * @return
     */
    public FT_STATUS setReset(boolean isReset) {

        byte data;
        if (!isReset) {
            data = (byte) 0xFF;//work
        } else {
            data = (byte) 0x00;//reset
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETRESET[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETRESET[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = grabber.writePipe(msg);

        return ft_status;
    }

    /**
     * Установка id устройства
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
        LOG.info("ID установлено {}", ft_status);

        return ft_status;
    }

    /**
     * Чтение данных сырых данных
     *
     * @return
     */
    public Bytes readData() {
        Bytes bytes = grabber.readPipe();

        //тест

        if (isTest) {
            bytes = testData(bytes);
        }

        return bytes;
    }

    public Bytes nextFrame() {

        LOG.trace("Запрос на получение данных из nextFrame");
        Bytes bytesData = readData();
        LOG.trace("Запрос на получение данных после nextFrame");
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //проверка на пустой массив
        if (/*bytesData.isEmpty()||*/!grabber.getValidHendler().get()) {
            LOG.trace("Мы на паузе",bytesData);
            grabber.close();//todo нужно ли еще читать?
            return null;
        }
        LOG.trace("Запрос на получение данных после норм nextFrame{}",bytesData);
        /**
         * Есть заголовок?
         */
        if (bytesData.startsWith(HEADER.array())) {

            /**
             * Заголовок есть
             */
            /**
             * Считываем номер функции
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
                            e.printStackTrace();
                        }
                        //повторяем чтение до целого кадра
                        Bytes btData = readData();
                      //выход при обрыве данных
                        if (btData.isEmpty()) {
                            //Выход сюда, когда нет связи.
                            grabber.close();
                            clearBuffer();
                            return null;
                        }
                        tempSize = parse2bdOartOfFrame(btData);
                    } while ((fsize = (fsize - tempSize)) > 0);

                    Bytes bytes = summPartsOfFrame();
                    if (bytes.length() < (2 * grabber.getHeight() * grabber.getWidth())) {
                        LOG.error("Разрешение картинки и конфигурация детектора не совпадают!");
                        return null;
                    }
                    return bytes;
                case 0x00://установка ID
                    System.out.println("0x00 " + bytesData);
                    grabber.getNeedToWaite().set(false);
                    //todo
                    return null;
                case 0x05://установка какого-либо параметра
                    System.out.println("0x05 " + bytesData);
                    grabber.getNeedToWaite().set(false);
                    //todo
                    return null;
                case 0x02://установка питания
                    System.out.println("0x02 " + bytesData);
                    grabber.getNeedToWaite().set(false);
                    //todo
                    return null;
                default:
                    System.out.println("default" + bytesData);
                    return null;
            }
        } else {
            /**
             * Заголовка нет
             */
           //0x008000000 подтверждение ID
            if(bytesData.length()==4){
                System.out.println("0x0080 " + bytesData);
                grabber.getNeedToWaite().set(false);
            }
            System.out.println("Заголовка нет" + bytesData);
            return null;
        }
    }

    /**
     * Обрабатывает составные части кадра
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
        // LOG.trace("размер второй части {}", length);
        return length / 2;
    }

    /**
     * Обрабатывает первую часть кадра
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
        //  LOG.trace("размер первой части {}", resized);
        return frameSize - (resized.length()) / 2;
    }

    /**
     * Разворачивает байты
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

    public void clearBuffer() {
        LOG.debug("Очистка буфера размером {}", bufferVideoParts.size());
        bufferVideoParts.clear();

    }
    /**
     * Тестовые данные
     *
     * @param bytes
     * @return
     */
    private static int count = 0;

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
