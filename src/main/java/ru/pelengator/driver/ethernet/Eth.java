package ru.pelengator.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.bytes.BytesTransformer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorResolution;
import ru.pelengator.API.buildin.china.ChinaDevice;
import ru.pelengator.driver.FT_STATUS;
import ru.pelengator.driver.usb.Jna2;
import ru.pelengator.model.StendParams;

import java.awt.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Eth extends Jna2 {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Eth.class);

    //количество переданных байтов
    private static long byteTrans = 0;
    private StendParams params = null;
    private boolean isTest = true;

    /**
     * Флаг остановки сервисов
     */
    private boolean isPause = true;
///////////////////////////////////////////////////////////////////////////////////

    /**
     * Конструктор
     */
    public Eth(StendParams params) {
        this.params = params;
    }

    //рабочие методы
    //////////////////////////////////////////////////////////////

    /**
     * Инициализация работы
     *
     * @return Статус FT_
     */
    public FT_STATUS create() {

        int i = upConnections(params);

        if (isTest) {
            i = 0;
        }

        FT_STATUS status = FT_STATUS.values()[i];
        return status;
    }

    private int upConnections(StendParams params) {

        if (isPause) {
            boolean b = startUDPServers(params);//старт UDP
            if (b) {
                isPause = false;
                return 0;
            }
        }
        return 3;
    }


    private ExecutorService service;
    ComandClient comandClient = null;
    CommandServer comandServer = null;
    VideoServer videoServer = null;


    static private int MP;
    static volatile ObjectProperty<byte[]> incomingComand = new SimpleObjectProperty();
    static volatile ObjectProperty<byte[]> outComand = new SimpleObjectProperty();
    static volatile ObjectProperty<byte[]> video = new SimpleObjectProperty(new byte[0]);


    {
        MP = Runtime.getRuntime().availableProcessors();
    }

    private boolean startUDPServers(StendParams params) {

        service = Executors.newFixedThreadPool(MP);

        //запуск серверов
        try {
            videoServer = new VideoServer(params);

        } catch (SocketException e) {
            LOG.error("Не удалось запустить видео сервер. Сокет занят", e);
            return false;
        } catch (IOException e) {
            LOG.error("Не удалось запустить видео сервер", e);
        }
        Future<Void> future1 = service.submit(videoServer);
        try {
            comandServer = new CommandServer(params);
        } catch (SocketException e) {
            LOG.error("Не удалось запустить командный сервер. Сокет занят", e);
            return false;
        } catch (IOException e) {
            LOG.error("Не удалось запустить командный сервер", e);
        }
        Future<Void> future2 = service.submit(comandServer);

        outComand.addListener((observable, oldValue, newValue) -> {

            try {
                comandClient = new ComandClient(params, comandServer);
            } catch (SocketException e) {
                LOG.error("Не удалось запустить командный клиент. Сокет занят", e);
            } catch (UnknownHostException e) {
                LOG.error("Не удалось запустить командный клиент. ip не известен", e);
            }
            Future<Boolean> future3 = service.submit(comandClient);
        });
        return true;
    }


    class VideoServer implements Callable<Void> {

        Server server;

        public VideoServer(StendParams params) throws IOException {
            this.server = new Server(params.getDetPortVideo(), params.getServerVideoBuff(), params.getSelNetworkInterface());
        }

        @Override
        public Void call() throws Exception {
            while (true) {
                byte[] listen = server.listen();
                setVideo(listen);
            }
        }

    }

    class CommandServer implements Callable<Void> {

        Server server;

        public CommandServer(StendParams params) throws IOException {
            this.server = new Server(params.getDetPortCommand(), params.getCommandBuff(), params.getSelNetworkInterface());
        }

        @Override
        public Void call() throws Exception {

            while (true) {
                byte[] listen = server.listen();
                setIncomingComand(listen);
            }
        }

        public DatagramSocket getServerDS() {

            return this.server.getSS();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            server.stop();
        }
    }

    class ComandClient implements Callable<Boolean> {

        Client client;

        public ComandClient(StendParams params) throws SocketException, UnknownHostException {
            this.client = new Client(params.getDetIP(), params.getDetPortCommand(), params.getSelNetworkInterface());
        }

        public ComandClient(StendParams params, CommandServer cs) throws SocketException, UnknownHostException {
            this.client = new Client(params.getDetIP(), params.getDetPortCommand(), params.getSelNetworkInterface(), cs.getServerDS());
        }

        @Override
        public Boolean call() throws IOException {
            boolean b = client.sendMsg(getOutComand());
            return b;
        }
    }


    /**
     * Синхронная запись
     *
     * @param data Массив для записи
     * @return
     */
    public FT_STATUS writePipe(Bytes data) {

        setOutComand(data.array());
        int i = 0; //i FT_Status
        //todo доделать оповещение о статусе отправки сообщения

        //тест
        if (isTest) {
            i = 0;
        }

        FT_STATUS status = FT_STATUS.values()[i];

        return status;
    }

    long tt = 0;

    byte[] lastVideo = null;

    /**
     * чтение массива
     *
     * @return Обернутый массив
     */
    public Bytes readPipe() {
        //создание конннекта
        int i = 0; //i FT_Status

        byte[] byteArray = getVideo();// из буфера в массив

        if (byteArray != lastVideo) {
            lastVideo=byteArray;
            Bytes from = Bytes.from(byteArray);
            return from;
        }

        //тест
        if (isTest) {
            i = 0;
        }

        return Bytes.empty();
    }

    /**
     * Реконнект
     */
    public synchronized void reconnect() {
     //enpty
    }

    /**
     * Завершение работы
     *
     * @return Статус FT_
     */
    public FT_STATUS close() {

        try {
            service.shutdown();
            service.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Экстренное завершение");
        } finally {
            if (!service.isTerminated()) {
                LOG.error("Отмена незаконченных задач");
                service.shutdownNow();
            }
        }
        int i = 0; //i FT_Status
        return FT_STATUS.values()[i];
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
    public java.util.List<DetectorDevice> getDDevices(List<DetectorDevice> devices) {


        if (flag_device_connected.get()) {
            ChinaDevice device = new ChinaDevice(name, id, this);
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

        ByteBuffer bytes = null;
        Bytes frame = nextFrame();
        if (frame == null) {
            return null;
        }
        bytes = frame.buffer();
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
        LOG.trace("Остановка сессии");
        close();
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
        return writePipe(msg);

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
        return writePipe(msg);

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
        return writePipe(msg);

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
        return writePipe(msg);

    }

    public FT_STATUS setIntTime(int time) {

        byte[] data = Bytes.from(time).resize(2).reverse().array();
        Bytes msg = HEADER            //маска+ID
                .append(SETINT[0])    //функция
                .append((byte) 0x03)  //размер[команда+данные]||
                .append(SETINT[1])    //команда               |
                .append(data);        //данные               _|
        return writePipe(msg);
    }

    /**
     * Установка напр. смещения
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVR0(int value) {
        // float floatValue = value / 1000f;
        //byte[] data = Bytes.from(floatValue).reverse().array();
        byte data = Bytes.from(value).resize(1).toByte();
        Bytes msg = HEADER            //маска+ID
                .append(SETVR0[0])    //функция
                .append((byte) 0x02)  //размер[команда+данные]||
                .append(SETVR0[1])    //команда               |
                .append(data);        //данные               _|
        return writePipe(msg);
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
        return writePipe(msg);
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
        return writePipe(msg);
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
        return writePipe(msg);
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
        return writePipe(msg);
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
        return writePipe(msg);
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
        FT_STATUS ft_status = writePipe(msg);

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
         //   bytes = testData(bytes);
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


        Bytes bytesData = readData();
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //проверка на пустой массив
        if (bytesData.isEmpty()) {
            return null;
        }

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
                        if (bytesData.isEmpty()) {
                            clearBuffer();
                            return null;
                        }
                        tempSize = parse2bdOartOfFrame(btData);
                    } while ((fsize = (fsize - tempSize)) > 0);
                    return summPartsOfFrame();
                case 0x00://отработка отображения всех параметров
                    //todo
                default:
                    //todo
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
        if (bytesData.isEmpty()) {
            return 0;
        }
        int length = bytesData.length();
        bytesData = reversBytes(bytesData);
        bufferVideoParts.add(bytesData);
        LOG.trace("размер второй части {}", length);
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
        LOG.trace("размер первой части {}", resized);
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


    public void setParameters(Map<String, ?> params) {

    }

    public static byte[] getIncomingComand() {
        return incomingComand.get();
    }

    public static ObjectProperty<byte[]> incomingComandProperty() {
        return incomingComand;
    }

    public static void setIncomingComand(byte[] incomingComand) {
        Eth.incomingComand.set(incomingComand);
    }

    public static byte[] getOutComand() {
        return outComand.get();
    }

    public static ObjectProperty<byte[]> outComandProperty() {
        return outComand;
    }

    public static void setOutComand(byte[] outComand) {
        Eth.outComand.set(outComand);
    }

    public static byte[] getVideo() {
        return video.get();
    }

    public static ObjectProperty<byte[]> videoProperty() {
        return video;
    }

    public static void setVideo(byte[] video) {
        Eth.video.set(video);
    }
}
