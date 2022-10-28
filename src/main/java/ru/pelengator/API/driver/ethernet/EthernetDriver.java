package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
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

    private DetectorDriver driver = null;
    private DatagramSocket dsock;

    /**
     * Конструктор
     */
    public EthernetDriver(StendParams params, DetectorDriver driver) {
        this.params = params;
        NetworkInfo selNetworkInterface = params.getSelNetworkInterface();
        this.videoPort = params.getDetPortVideo();
        this.comPort = params.getDetPortCommand();
        this.myIp = selNetworkInterface.getAddress();
        this.broadcastIp = selNetworkInterface.getBroadcast();
        this.comList = new ComListEth(this, myIp, broadcastIp);
        this.driver = driver;
    }

    /**
     * Пустой метод
     *
     * @return Статус FT_
     */
    @Override
    public FT_STATUS create() {
        FT_STATUS status = FT_STATUS.FT_OK;
        return status;
    }

    static private int MP;

    static {
        MP = Runtime.getRuntime().availableProcessors();
    }


    /**
     * Завершение работы
     *
     * @return Статус FT_
     */
    @Override
    public FT_STATUS close() {
        return FT_STATUS.values()[0];
    }

    private int PENDING = 100;// время на прослушивание ответа

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

        LOG.debug("GetDevices");
        Set<DetectorDevice> detectorDevices = null;
        if (comList != null && !dispoced.get()) {

            if (comIS == null || comOS == null) {
                try {
                    comIS = new UDPInputStream(myIp, comPort + 1, PENDING);
                    // DatagramSocket dsock = comIS.getDsock();

                    dsock = new DatagramSocket(comPort, myIp);

                    comOS = new UDPOutputStream(dsock, broadcastIp, comPort);

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

        }
        devices.addAll(detectorDevices);
        return Collections.unmodifiableList(devices);
    }


    /**
     * Формирование массива изображения из буффера из интов.
     *
     * @return Обернутый массив сырого изображения инт
     */
    @Override
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
        LOG.debug("StopSession");
        dispoced.set(true);
        try {
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FT_STATUS startSession() {
        LOG.debug("StartSession");
        dispoced.set(false);
        try {
            clientIp = InetAddress.getByName(params.getDetIP());
        } catch (UnknownHostException e) {
            LOG.error("Error in starting session, not avaible DetIP, {}", e.getMessage());
            throw new RuntimeException(e);
        }
        FT_STATUS status;
        try {
            /**
             * Создание видеосервиса.
             */
            videoIS = new UDPInputStream(myIp, videoPort, 20 * PENDING);

            /**
             * Создание командного входящего сервиса.
             */
            if (comIS != null) {
                comIS.close();
            }
            comIS = new UDPInputStream(myIp, comPort + 1, PENDING);
            //  DatagramSocket dsock = comIS.getDsock();

            /**
             * Создание командного исходящего сервиса.
             */
            if (comOS != null || dsock.isClosed()) {
                comOS.close();
                dsock.close();
                dsock = new DatagramSocket(comPort, myIp);
            }

            comOS = new UDPOutputStream(dsock, clientIp, comPort);

            isOpened.compareAndSet(false, true);

            status = FT_STATUS.FT_OK;
        } catch (IOException e) {

            isOpened.compareAndSet(true, false);
            status = FT_STATUS.FT_DEVICE_NOT_CONNECTED;
            throw new RuntimeException(e);
        }

        validHendler.compareAndSet(true, false);

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

        // if (set) {
        //     FT_STATUS ft_status = setID();
        //     LOG.debug("Set ID in SetPower metod. Status: ",ft_status);
        // }

        return comList.setPower(set);
    }

    @Override
    public FT_STATUS setIntTime(int time) {
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
        return comList.setREF(value);
    }


    /**
     * Установка id устройства
     *
     * @return
     */
    @Override
    public FT_STATUS setID() {
        return comList.setID();
    }

    private int frameCount = 0;

    @Override
    public Bytes nextFrame() {

        Bytes bytes = comList.nextFrame();
        if (bytes == null) {
            frameCount++;
            LOG.debug("Online flag null frame {}", frameCount);
            if (frameCount > 3) {//В случае пустых кадров - выставить флаг на отсутствие сигнала
                if (online.compareAndSet(true, false)) {
                    LOG.debug("Online flag setted in false");
                }
                ;

            }
        } else {
            frameCount = 0;
            if (online.compareAndSet(false, true)) {
                LOG.debug("Online flag setted in true");
            }
            ;
        }
        return bytes;
    }

    @Override
    public void clearBuffer() {
        //ignore
    }

    @Override
    public boolean isOnline() {

        return online.get();
    }

    /**
     * Отправить сообщение.
     */
    public byte[] sendMSG(byte[] byteMSG) {
        return sendMSG(byteMSG, clientIp);
    }

    /**
     * Отправить сообщение.
     */
    public byte[] sendMSG(byte[] byteMSG, InetAddress Ip) {

        return sendMSG(byteMSG, Ip, false);
    }

    public byte[] sendMSG(byte[] byteMSG, InetAddress Ip, boolean onlyOneMSG) {

        int tick = 0;
        byte[] incomMSG = null;

        do {
            try {//отправка сообщения
                LOG.debug("sendMSG {} /try {}", Arrays.toString(byteMSG), tick);

                comOS.write(byteMSG);
                comOS.setiAdd(Ip);
                comOS.flush();

                //Прослушивание ответа
                incomMSG = comList.waitAnswer(comIS, byteMSG);
                if (incomMSG != null) {
                    tick = 3;
                }
                LOG.debug("MSG to detector send: {}. ANSWER received: {}", Arrays.toString(byteMSG), Arrays.toString(incomMSG));

            } catch (IOException e) {

                LOG.debug("MSG not delivery. MSG {},/try {}, pending {}", Arrays.toString(byteMSG), tick, PENDING);
            }

        } while (tick++ < 2 && !onlyOneMSG);

        LOG.debug("Exiting from SENDMSG {} /try {}", Arrays.toString(byteMSG), tick);

        return incomMSG;
    }

    /**
     * Остановить отправку сообщения.
     *
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        //ignore
    }

    public UDPInputStream getVideoIS() {
        return videoIS;
    }

    public static Dimension getSize() {
        return size;
    }

    public DetectorDriver getDriver() {
        return driver;
    }
}
