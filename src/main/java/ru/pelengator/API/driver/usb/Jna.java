package ru.pelengator.API.driver.usb;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.bytes.BytesTransformer;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorResolution;
import ru.pelengator.API.devises.china.ChinaDevice;
import ru.pelengator.API.driver.FT_STATUS;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.pelengator.App.getFtd3XX;

/**
 * Драйвер с java на c++
 */
public class Jna {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Jna.class);

    //Размер буфера для отправки сообщения
    private static final int MB = 1024 * 1024;
    //буфер для передачи байтов
    private static volatile Pointer pBuff = new Memory(MB);
    //обработчик устройства
    private static volatile Pointer hendler = new Memory(MB);
    //количество переданных байтов
    private static volatile LongByReference byteTrans = new LongByReference(0);
    private static AtomicBoolean online = new AtomicBoolean(false);
    private static AtomicBoolean isMessageSend = new AtomicBoolean(false);
    private boolean isTest = false;
///////////////////////////////////////////////////////////////////////////////////

    /**
     * Конструктор
     */
    public Jna() {
    }


    /**
     * Нативный интерфейс
     */
    public interface FTD3XX extends StdCallLibrary {

        FTD3XX FTD3XX_INSTANCE = (FTD3XX) Native.loadLibrary(getFtd3XX(), FTD3XX.class);

        /////////////////////////////////////////////////////методы интерфейса//////////////////////////
        //создание интерфейса
        int FT_Create(String pvArg, byte dwFlags, Pointer pftHandle);

        //синхронная запись
        int FT_WritePipe(Pointer ftHandle, byte ucPipeID, Pointer pucBuffer, long ulBufferLength, LongByReference pulBytesTransferred, Structure pOverlapped);

        //синхронное чтение
        int FT_ReadPipe(Pointer ftHandle, byte ucPipeID, Pointer pucBuffer, long ulBufferLength, LongByReference pulBytesTransferred, Structure pOverlapped);

        //закрытие интерфейса
        int FT_Close(Pointer ftHandle);

    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////
    //
    //рабочие методы
    //////////////////////////////////////////////////////////////

    /**
     * Создание обработчика
     * Инициализация работы
     *
     * @return Статус FT_
     */
    public FT_STATUS create() {
        int i = FTD3XX.FTD3XX_INSTANCE.FT_Create("FTDI SuperSpeed-FIFO Bridge", (byte) 0x0A, hendler);
        //тест
        if (isTest) {
            i = 0;
        }

        FT_STATUS value = FT_STATUS.values()[i];
        LOG.error("Открытие обработчика обработчика {}", value);
        return value;
    }

    /**
     * Синхронная запись по обработчику **hendler**
     *
     * @param data Массив для записи
     * @return
     */
    public FT_STATUS writePipe(Bytes data) {
        pBuff.write(0, data.array(), 0, data.length());//запись данных в буфер
        int i = FTD3XX.FTD3XX_INSTANCE.FT_WritePipe(hendler.getPointer(0), (byte) 0x02, pBuff, data.length(), byteTrans, (Structure) null);
        //тест
        if (isTest) {
            i = 0;
        }
        FT_STATUS status = FT_STATUS.values()[i];
        return status;
    }

    long tt = 0;

    /**
     * чтение массива
     *
     * @return Обернутый массив
     */
    public Bytes readPipe() {
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (online.get()) {
            int i = FTD3XX.FTD3XX_INSTANCE.FT_ReadPipe(hendler.getPointer(0), (byte) 0x82, pBuff, MB, byteTrans, (Structure) null);
            byte[] byteArray = pBuff.getByteArray(0, (int) byteTrans.getValue());// из буфера в массив
            Bytes from = Bytes.from(byteArray);
            //тест
            if (isTest) {
                i = 0;
            }
           // Byte[] bytes = from.toBoxedArray();
            FT_STATUS status = FT_STATUS.values()[i];
            if (status != FT_STATUS.FT_OK||from.isEmpty()) {
                online.compareAndSet(true, false);
                LOG.error("From READPIPE FT_notOK{}", online.get());
            }else{
                online.compareAndSet(false, true);
            }
            return from;
        } else {
            LOG.error("From READPIPE Null{}", online.get());
            return null;
        }
    }

    private FT_STATUS lastFT = null;

    /**
     * Реконнект
     */
    public synchronized FT_STATUS reconnect() {
        LOG.error("Реконнект");

        stopSession();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            LOG.error("Ошибка при реконнекте {}", e);
            e.printStackTrace();
        }
        if (create() != FT_STATUS.FT_OK) {
            return FT_STATUS.FT_DEVICE_LIST_NOT_READY;
        }

        FT_STATUS ft_status = setID();

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(ft_status==FT_STATUS.FT_OK){
            online.compareAndSet(false,true);
        }

        return FT_STATUS.FT_OK;
    }

    /**
     * Завершение работы
     * Закрытие обработчика
     *
     * @return Статус FT_
     */
    public FT_STATUS close() {
        int i = FTD3XX.FTD3XX_INSTANCE.FT_Close(hendler.getPointer(0));
        FT_STATUS value = FT_STATUS.values()[i];
        LOG.error("Закрытие обработчика {}", value);
        return value;
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////управление/////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
//todo
    private String name = "ДТ10Э";
    private String id = "ЕАНГ.468424.012";

    private static volatile AtomicBoolean flag_device_connected = new AtomicBoolean(true);

    private static volatile AtomicBoolean flag_frame_ready = new AtomicBoolean(false);

    private static volatile LinkedList<Bytes> bufferVideoParts = new LinkedList<Bytes>();

    private static volatile LinkedList<Bytes> currentFrames = new LinkedList<Bytes>();


    private static Dimension size = DetectorResolution.CHINA.getSize();

    private static Map<String, ?> properties = new HashMap<>();

//todo

    /**
     * Заполнение списка устройств
     *
     * @param devices пустой список устройств
     * @return заполненный список устройств
     */
    public List<DetectorDevice> getDDevices(List<DetectorDevice> devices) {

        if (flag_device_connected.get()) {
            ChinaDevice device = new ChinaDevice(name, id, new Jna2()/*this*/);
            device.setResolution(size);
            devices.add(device);
        } else {
            LOG.error("Устройство НЕ подключено. Пустой лист");
        }
        return Collections.unmodifiableList(devices);
    }

    /**
     * Формирование массива изображения из буффера из интов.
     *
     * @return Обернутый массив сырого изображения инт
     */
    public ByteBuffer getImage() {
        LOG.trace("Запрос на получение Картинки из getImage до nextFrame");
        ByteBuffer bytes = null;
        Bytes frame = nextFrame();
        LOG.trace("Вывод полного кадра из getImage после nextFrame {}", frame);
        if (frame == null) {
            return null;
        }
        bytes = frame.buffer();
        LOG.trace("Вывод полного кадра {}", bytes);
        return bytes;
    }


    /**
     * Запрос ширины полученной картинки
     *
     * @return ширина в px
     */
    public int getWidth() {
        return (int) size.getWidth();
    }

    /**
     * Запрос высоты полученной картинки
     *
     * @return высота в px
     */
    public int getHeight() {
        return (int) size.getHeight();
    }

    /**
     * Остановка текущей сессии.
     * Закрытие устройства
     */
    public void stopSession() {
        LOG.trace("Старт остановки сессии");
        waitToClearUSB();

        LOG.trace("Остановка сессии");
        close();
    }

    private void waitToClearUSB() {
        Bytes bytes = null;
        do {
            bytes = nextFrame();

        } while (bytes != null);
        isMessageSend.compareAndSet(false, true);
    }

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
        online.set(true);
        isMessageSend.set(false);
        byte data;
        if (set) {
            data = (byte) 0xFF;//128*128
            size = DetectorResolution.CHINA.getSize();
        } else {
            data = (byte) 0x00;//92*90
            size = DetectorResolution.CHINALOW.getSize();
        }
        Bytes msg = HEADER              //маска+ID
                .append(SETDIM[0])    //функция
                .append((byte) 0x02)    //размер[команда+данные]||
                .append(SETDIM[1])    //команда               |
                .append(data);          //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;

    }

    /**
     * Установка коэф. усиления
     *
     * @param set - 0xFF -3 (true); 0x00 - 1
     * @return
     */
    public FT_STATUS setCapacity(boolean set) {
        online.set(true);
        isMessageSend.set(false);
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
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;

    }

    /**
     * Подача питания RE
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setRE(boolean set) {
        online.set(true);
        isMessageSend.set(false);
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
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;

    }

    /**
     * Подача питания VDD, VDDA
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setPower(boolean set) {

        online.set(true);
        isMessageSend.set(false);
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
        FT_STATUS ft_status = writePipe(msg);

        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    public FT_STATUS setIntTime(int time) {
        online.set(true);
        isMessageSend.set(false);
        byte[] data = Bytes.from(time).resize(2).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETINT[0])    //функция
                .append((byte) 0x03)  //размер[команда+данные]||
                .append(SETINT[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    /**
     * Установка напр. смещения
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVR0(int value) {
        online.set(true);
        isMessageSend.set(false);
        byte data = Bytes.from(value).resize(1).toByte();
        Bytes msg = HEADER            //маска+ID
                .append(SETVR0[0])    //функция
                .append((byte) 0x02)  //размер[команда+данные]||
                .append(SETVR0[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }


    /**
     * Установка направления сканирования
     *
     * @param value - byte
     * @return
     */
    public FT_STATUS setRo(byte value) {
        online.set(true);
        isMessageSend.set(false);
        Bytes msg = HEADER            //маска+ID
                .append(SETRO[0])    //функция
                .append((byte) 0x02)  //размер[команда+данные]||
                .append(SETRO[1])    //команда               |
                .append(value);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }


    /**
     * Установка напр. антиблюминга
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA(int value) {
        online.set(true);
        isMessageSend.set(false);
        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    /**
     * Установка рефов1
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA1(int value) {
        online.set(true);
        isMessageSend.set(false);
        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA1[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA1[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    /**
     * Установка рефов2
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA2(int value) {
        online.set(true);
        isMessageSend.set(false);
        float floatValue = value / 1000f;
        byte[] data = Bytes.from(floatValue).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETVVA2[0])    //функция
                .append((byte) 0x05)  //размер[команда+данные]||
                .append(SETVVA2[1])    //команда               |
                .append(data);        //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    /**
     * Сброс устройства
     *
     * @param isReset - true - ресет
     * @return
     */
    public FT_STATUS setReset(boolean isReset) {
        online.set(true);
        isMessageSend.set(false);
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
        FT_STATUS ft_status = writePipe(msg);
        while (!isMessageSend.get()) {
            nextFrame();
        }
        return ft_status;
    }

    /**
     * Установка id устройства
     *
     * @return
     */
    public FT_STATUS setID() {
        online.set(true);
        isMessageSend.set(false);
        Bytes msg = HEADER           //маска+ID
                .append(SETID[0])    //функция
                .append((byte) 0x02) //размер[команда+данные]||
                .append(SETID[1])    //команда               |
                .append(DEV_ID);     //данные               _|
        FT_STATUS ft_status = writePipe(msg);
        LOG.info("ID установлено {}", ft_status);
        while (!isMessageSend.get()) {
            nextFrame();
        }

        return ft_status;
    }


    /**
     * Чтение данных сырых данных
     *
     * @return
     */
    public Bytes readData() {
        Bytes bytes = readPipe();
        //todo
        //todo
        //todo
        //todo
        //тест

        if (isTest) {
            bytes = testData(bytes);
        }

        return bytes;
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

    public Bytes nextFrame() {

        LOG.trace("Запрос на получение данных из nextFrame");
        Bytes bytesData = readData();
        LOG.trace("Запрос на получение данных после nextFrame");
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //проверка на пустой массив
        if (bytesData == null) {
            LOG.trace("Запрос на получение данных после nextFrame пустой{}",bytesData);
            return null;
        }
        if (bytesData.isEmpty()) {
            LOG.trace("Запрос на получение данных после нулл {}",bytesData);

            return null;
        }
        online.compareAndSet(false,true);
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
                        Bytes btData = readData();
                        if (bytesData == null) {
                            clearBuffer();
                            return null;
                        }
                        if (bytesData.isEmpty()) {
                            clearBuffer();
                            return null;
                        }
                        tempSize = parse2bdOartOfFrame(btData);
                    } while ((fsize = (fsize - tempSize)) > 0);

                    Bytes bytes = summPartsOfFrame();
                    if (bytes.length() < (2 * getWidth() * getHeight())) {

                        LOG.error("Разрешение картинки и конфигурация детектора не совпадают!");
                        return null;
                    }
                    return bytes;
                case 0x00://отработка отображения всех параметров
                    System.out.println("0x00 " + bytesData);
                    //todo
                    isMessageSend.set(true);
                default:
                    System.out.println("Ответ на команду " + bytesData);
                    isMessageSend.set(true);
                    return null;
            }
        } else {
            /**
             * Заголовка нет
             */
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
    //    if (bytesData==null) {
    //        return 0;
    //    }
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


    public FT_STATUS setParameters(Map<String, ?> params) {
        return FT_STATUS.FT_OK;
    }
}

