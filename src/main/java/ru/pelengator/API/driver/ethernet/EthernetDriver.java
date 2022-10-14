package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorExceptionHandler;
import ru.pelengator.API.DetectorResolution;
import ru.pelengator.API.driver.Driver;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.model.NetworkInfo;
import ru.pelengator.model.StendParams;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * Броадкаст ip
     */
    private InetAddress broadcastIp;
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
    private int к = 0;
    private AtomicBoolean dispoced = new AtomicBoolean(false);
    private AtomicBoolean online = new AtomicBoolean(false);

    /**
     * Конструктор
     */
    public EthernetDriver(StendParams params) {
        this.params = params;
        NetworkInfo selNetworkInterface = params.getSelNetworkInterface();
        this.videoPort = params.getDetPortVideo();
        this.comPort = params.getDetPortCommand();
        LOG.debug("Создан драйвер");
        this.myIp = selNetworkInterface.getAddress();
        this.broadcastIp = selNetworkInterface.getBroadcast();
        this.comList = new ComListEth(this, myIp, broadcastIp);
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

    private Future<?> msgTask;
    private String stend = "ДТ10Э";

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
        Set<DetectorDevice> detectorDevices = null;
        if (comList != null && !dispoced.get()) {

            if (comIS == null || comOS == null) {
                try {
                    comIS = new UDPInputStream(myIp.getHostAddress(), comPort, 500);
                    comOS = new UDPOutputStream(broadcastIp, comPort);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            detectorDevices = new HashSet<>();
            comList.whoIsThere(detectorDevices);

            while (!msgTask.isDone()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        devices.addAll(detectorDevices);
        return Collections.unmodifiableList(devices);
    }

    private AtomicBoolean onlyOneMSG = new AtomicBoolean(false);

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
        dispoced.set(true);
        try {
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FT_STATUS startSession() {
        LOG.debug("Запуск сессии");
        dispoced.set(false);
        try {
            clientIp = InetAddress.getByName(params.getDetIP());
        } catch (UnknownHostException e) {
            LOG.error("Error in starting session, not avaible DetIP, {}",e.getMessage());
            throw new RuntimeException(e);
        }
        FT_STATUS status;
        try {
            /**
             * Создание видеосервиса.
             */
            videoIS = new UDPInputStream(myIp.getHostAddress(), videoPort, 500);
            /**
             * Командный сокет.
             */
            //   DatagramSocket clientDatagramSocket = new DatagramSocket(comPort);
            /**
             * Создание командного сервиса.
             */
            if (comIS != null) {
                comIS.close();
            }
            comIS = new UDPInputStream(myIp.getHostAddress(), comPort, 500);

            /**
             * Создание видеосервиса.
             */
            if (comOS != null) {
                comOS.close();
            }
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
        if (bytes == null) {
            online.compareAndSet(true, false);
        } else {
            online.compareAndSet(false, true);
        }
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
        return online.get();
    }


    /**
     * Запланированный исполнитель, действующий как таймер.
     */
    private ScheduledExecutorService executor = null;


    /**
     * Задание на отправку сообщения.
     */
    private class SenderScheduler extends Thread {

        private volatile byte[] MSG;
        private volatile InetAddress Ip = null;
        private volatile int N=0;
        private AtomicBoolean onlyOneMSG = new AtomicBoolean(false);

        /**
         * Фабрика в конструкторе.
         */
        public SenderScheduler(byte[] byteMSG, InetAddress broadcastIp) {
            setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            setName(String.format("DatagramSender-scheduler-%s", stend));
            setDaemon(true);
            this.MSG = byteMSG;
            this.Ip = broadcastIp;
        }

        private int tick = 0;

        @Override
        public void run() {
            LOG.debug(" {} Старт отправки сообщения {}",N++, MSG);
            try {
                if (!onlyOneMSG.get()) {
                    comOS.write(MSG);
                    comOS.setiAdd(Ip);
                    comOS.flush();
                } else {
                    comOS.write(MSG);
                    comOS.setiAdd(Ip);
                    comOS.flush();
                    onlyOneMSG.set(false);
                }
                boolean quality = comList.waitAnsvwer(comIS, MSG);
                LOG.debug("quality сообщения {}", quality);

                if (quality) {
                    LOG.debug("MSG podtvergdeno: {}", MSG);
                    tick = 0;
                } else if (tick++ <= 5 && !dispoced.get()) {//если подстверждения нет, то повторяем
                    executor.schedule(this, 300, TimeUnit.MILLISECONDS);
                    LOG.debug("Повторная отсылка {} try {}", Arrays.toString(MSG), tick);
                } else {
                    tick = 0;
                    LOG.debug("Нет подтверждения отправки сообщения {}", Arrays.toString(MSG));
                }
            } catch (RejectedExecutionException e) {
                LOG.warn("Executor rejected sender msg");
                LOG.trace("Executor rejected sender msg because of", e);
            } catch (IOException e) {
                LOG.debug("сообщение не доставлено, вышло время {} try {}", Arrays.toString(MSG), tick);
                //   if (tick++ <= 5) {//если подстверждения нет, то повторяем
                //       executor.schedule(this, 300, TimeUnit.MILLISECONDS);
                //   } else {
                //       tick = 0;
                //    }
            }
        }

        public void setMSG(byte[] MSG) {
            this.MSG = MSG;
        }

        public void setIp(InetAddress ip) {
            this.Ip = ip;
        }

        public AtomicBoolean getOnlyOneMSG() {
            return onlyOneMSG;
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
        sendMSG(byteMSG, clientIp);

    }

    /**
     * Отправить сообщение.
     */
    public void sendMSG(byte[] byteMSG, InetAddress Ip) {

        sendMSG(byteMSG, Ip, false);
    }

    public void sendMSG(byte[] byteMSG, InetAddress Ip, boolean onlyOneMSG) {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(1);
        }
        if (scheduler == null) {
            scheduler = new SenderScheduler(byteMSG, Ip);
        } else {
            scheduler.setMSG(byteMSG);
            scheduler.setIp(Ip);
        }
        scheduler.getOnlyOneMSG().set(onlyOneMSG);
        msgTask = executor.submit(scheduler);

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

    public static Dimension getSize() {
        return size;
    }
}
