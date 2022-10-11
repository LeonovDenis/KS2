package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorExceptionHandler;
import ru.pelengator.API.DetectorResolution;
import ru.pelengator.API.devises.china.ChinaDevice;
import ru.pelengator.API.driver.Driver;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.model.StendParams;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EthernetDriver implements Driver {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(EthernetDriver.class);
    /**
     * Параметры стенда
     */
    private StendParams params = null;
    /**
     * Задействованный интерфейс
     */
    private NetworkInterface netInterface;
    /**
     * Свой ip
     */
    private InetAddress myIp;
    /**
     * Клиентсткий ip
     */
    private InetAddress clientIp;
    /**
     * Видео порт
     */
    private int videoPort;
    /**
     * Командный порт
     */
    private int comPort;
    /**
     * Класс обработки команд
     */
    private ComListEth comList;
    /**
     * Видео поток
     */
    private UDPInputStream videoIS;
    /**
     * Командный входной поток
     */
    private UDPInputStream comIS;
    /**
     * Командный выходной поток
     */
    private UDPOutputStream comOS;
    private int к;

    /**
     * Конструктор
     */
    public EthernetDriver(StendParams params) {
        this.params = params;
        try {
            this.netInterface = NetworkInterface.getByName(this.params.getSelNetworkInterface().getName());
            this.videoPort = params.getDetPortVideo();
            this.comPort = params.getDetPortCommand();
            this.clientIp = InetAddress.getByName(params.getDetIP());
            LOG.debug("Создан драйвер");

        } catch (SocketException e) {
            LOG.debug("Ощибка создания драйвера {}", e);
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            LOG.debug("Ощибка создания драйвера {}", e);
            throw new RuntimeException(e);
        }

        this.myIp = netInterface.getInetAddresses().nextElement();

    }

    /**
     * Пустой метод
     *
     * @return Статус FT_
     */
    @Override
    public FT_STATUS create() {
        FT_STATUS status = FT_STATUS.FT_OK;
        LOG.debug("Вызван метод креате");
        return status;
    }

    static private int MP;

    static {
        MP = Runtime.getRuntime().availableProcessors();
        LOG.debug("Количество ядер: {}", MP);
    }


    /**
     * Завершение работы
     *
     * @return Статус FT_
     */
    @Override
    public FT_STATUS close() {
        LOG.debug("Вызван метод клосе");
        return FT_STATUS.values()[0];
    }

    private String stringDetIP = "ДТ10Э";
    private String stringID = "";

    private static Dimension size = DetectorResolution.CHINA.getSize();

    private static Map<String, ?> properties = new HashMap<>();


    /**
     * Заполнение списка устройств
     *
     * @param devices пустой список устройств
     * @return заполненный список устройств
     */
    @Override
    public java.util.List<DetectorDevice> getDDevices(List<DetectorDevice> devices) {

        LOG.debug("Поиск устройств");

        if (true) {
            stringDetIP = clientIp.getHostAddress();
            stringID = String.format("Ports C/V [%d/%d]", comPort, videoPort);

            ChinaDevice device = new ChinaDevice(stringDetIP, stringID, this);
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
    @Override
    public ByteBuffer getImage() {
        LOG.debug("Запрос getImage()");
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
    @Override
    public int getWidth() {
        return (int) size.getWidth();
    }

    /**
     * Запрос высоты полученной картинки
     *
     * @return высота в px
     */
    @Override
    public int getHeight() {
        return (int) size.getHeight();
    }

    /**
     * Остановка текущей сессии.
     * Закрытие устройства
     */
    @Override
    public void stopSession() {
        LOG.debug("Остановка сессии");
        try {
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FT_STATUS startSession() {
        LOG.debug("Запуск сессии");
        comList = new ComListEth(this);

        FT_STATUS status;
        try {
            /**
             * Создание видеосервиса.
             */
            videoIS = new UDPInputStream(myIp.getHostAddress(), videoPort,0);
            /**
             * Командный сокет.
             */
         //   DatagramSocket clientDatagramSocket = new DatagramSocket(comPort);
            /**
             * Создание командного сервиса.
             */
            comIS = new UDPInputStream(myIp.getHostAddress(), comPort,500);
            /**
             * Создание видеосервиса.
             */
            comOS = new UDPOutputStream(clientIp, comPort);

            isOpened.compareAndSet(false, true);

            status = FT_STATUS.FT_OK;
        } catch (IOException e) {

            isOpened.compareAndSet(true, false);
            status = FT_STATUS.FT_DEVICE_NOT_CONNECTED;
            throw new RuntimeException(e);
        }

        validHendler.compareAndSet(true, false);
        LOG.debug("Сессия стартовала");
        return status;
    }

    /**
     * Установка разрешения
     *
     * @param set - 0xFF -128*128 (true); 0x00 - 92*90
     * @return
     */
    @Override
    public FT_STATUS setDimension(boolean set) {
        LOG.debug("setDimension {}", set);
        return comList.setDimension(set);
    }

    /**
     * Установка коэф. усиления
     *
     * @param set - 0xFF -3 (true); 0x00 - 1
     * @return
     */

    @Override
    public FT_STATUS setCapacity(boolean set) {
        LOG.debug("setCapacity {}", set);
        return comList.setCapacity(set);

    }

    /**
     * Подача питания VDD, VDDA
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    @Override
    public FT_STATUS setPower(boolean set) {
        LOG.debug("setPower {}", set);
        return comList.setPower(set);
    }

    @Override
    public FT_STATUS setIntTime(int time) {
        LOG.debug("setIntTime {}", time);
        return comList.setIntTime(time);
    }

    /**
     * Установка напр. смещения
     *
     * @param value - в миливольтах
     * @return
     */
    @Override
    public FT_STATUS setVR0(int value) {
        LOG.debug("setVR0 {}", value);
        return comList.setVR0(value);
    }


    /**
     * Установка напр. антиблюминга
     *
     * @param value - в миливольтах
     * @return
     */
    @Override
    public FT_STATUS setVVA(int value) {
        LOG.debug("setVVA {}", value);
        return comList.setVVA(value);
    }

    /**
     * Установка рефов1
     *
     * @param value - в миливольтах
     * @return
     */
    @Override
    public FT_STATUS setREF(int value) {
        LOG.debug("setREF {}", value);
        return comList.setREF(value);
    }


    /**
     * Установка id устройства
     *
     * @return
     */
    @Override
    public FT_STATUS setID() {
        LOG.debug("setID");
        return comList.setID();
    }


    @Override
    public Bytes nextFrame() {
        //разовый запрос, почему?
        LOG.debug("nextFrame()");
        Bytes bytes = comList.nextFrame();
        return bytes;
    }

    @Override
    public void clearBuffer() {
        //после запуска сессии
        LOG.debug("clearBuffer()");
    }

    @Override
    public boolean isOnline() {
        //главный поток запрашивает постоянно
       // LOG.debug("isOnline()");
        return true;
    }


    /**
     * Запланированный исполнитель, действующий как таймер.
     */
    private ScheduledExecutorService executor = null;


    /**
     * Задание на отправку сообщения.
     */
    private class SenderScheduler extends Thread {

        byte[] MSG;

        /**
         * Фабрика в конструкторе.
         */
        public SenderScheduler(byte[] byteMSG) {
            setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            setName(String.format("DatagramSender-scheduler-%s", stringDetIP));
            setDaemon(true);
            this.MSG = byteMSG;
        }


        @Override
        public void run() {
            LOG.debug("Отправка сообщения {}", MSG);
            try {
                comOS.write(MSG);
                comOS.flush();
                boolean quality = comList.waitAnsvwer(comIS, MSG);
                LOG.debug("quality сообщения {}", quality);
                if (quality) {
                    LOG.debug("MSG podtvergdeno: {}", MSG);

                } else {//если подстверждения нет, то повторяем
                    executor.schedule(this, 500, TimeUnit.MILLISECONDS);
                    LOG.debug("Повторная отсылка");
                }
            } catch (RejectedExecutionException e) {
                LOG.warn("Executor rejected sender msg");
                LOG.trace("Executor rejected sender msg because of", e);
            } catch (IOException e) {
                LOG.debug("сообщение не доставлено {}",Arrays.toString(MSG));
            }
        }

        public void setMSG(byte[] MSG) {
            this.MSG = MSG;
        }
    }

    /**
     * Класс для отправки.
     */
    private SenderScheduler scheduler = null;

    /**
     * Отправить сообщение.
     */
    public void sendMSG(byte[] byteMSG) {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(MP);
        }
        if (scheduler == null) {
            scheduler = new SenderScheduler(byteMSG);
        }else{
            scheduler.setMSG(byteMSG);
        }
        executor.submit(scheduler);
      //  scheduler.start();

    }

    /**
     * Остановить отправку сообщения.
     *
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        LOG.debug("stop()");
        executor.shutdown();
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        scheduler.join();
        LOG.debug("stop() закончен");
    }

    public UDPInputStream getVideoIS() {
        return videoIS;
    }
}
