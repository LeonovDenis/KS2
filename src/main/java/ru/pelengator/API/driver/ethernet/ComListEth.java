package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.devises.china.ChinaDevice;
import ru.pelengator.API.driver.FT_STATUS;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class ComListEth {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ComListEth.class);
    private InetAddress myIp;
    private InetAddress broadcastIp;
    private EthernetDriver grabber;

    public ComListEth(EthernetDriver grabber, InetAddress myIp, InetAddress broadcastIp) {
        this.grabber = grabber;
        this.myIp = myIp;
        this.broadcastIp = broadcastIp;

    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////методы управления
    private final byte DEV_ID = 0x05;
    private final Bytes COM_HEADER = Bytes.from((byte) 0xA1, DEV_ID);
    private final byte[] VIDEO_HEADER = new byte[]{(byte) 0xA2, DEV_ID};

    //команды
    private final byte COM_WHO_IS_THERE = (byte) 0x00;
    private final byte COM_WRITE = (byte) 0x01;
    private final byte COM_READ = (byte) 0x02;
    private final byte COM_BLK_WRITE = (byte) 0x03;
    private final byte COM_BLK_READ = (byte) 0x04;

    //Функции
    private final byte FUNC_WHO_IS_THERE = (byte) 0x00;
    private final byte FUNC_ID = (byte) 0x01;
    private final byte FUNC_POWER = (byte) 0x02;
    private final byte FUNC_INT = (byte) 0x03;
    private final byte FUNC_VR0 = (byte) 0x04;
    private final byte FUNC_REF = (byte) 0x05;
    private final byte FUNC_VOS = (byte) 0x06;
    private final byte FUNC_CCC = (byte) 0x07;
    private final byte FUNC_DIM = (byte) 0x08;


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
        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_DIM)            //функция
                .append(1)              //размер данных
                .append(data);          //данные
        grabber.sendMSG(msg.array());
        return FT_STATUS.FT_OK;

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
        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_CCC)            //функция
                .append(1)              //размер данных
                .append(data);          //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
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
        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_POWER)          //функция
                .append(1)              //размер данных
                .append(data);          //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    /**
     * Установка времени интегрирования.
     *
     * @param time
     * @return
     */
    public FT_STATUS setIntTime(int time) {

        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_INT)            //функция
                .append(4)              //размер данных
                .append(time);          //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    /**
     * Установка напр. смещения.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVR0(int value) {

        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_VR0)            //функция
                .append(4)              //размер данных
                .append(value);         //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    /**
     * Установка напр. VOS скимминга.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA(int value) {

        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_VOS)            //функция
                .append(4)              //размер данных
                .append(value);         //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    /**
     * Установка VREF и VOUTREF.
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setREF(int value) {

        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_REF)            //функция
                .append(4)              //размер данных
                .append(value);         //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    /**
     * Установка id устройства.
     *
     * @return
     */
    public FT_STATUS setID() {

        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WRITE)          //команда
                .append(FUNC_ID)             //функция
                .append(1)              //размер данных
                .append(DEV_ID);        //данные
        grabber.sendMSG(msg.array());

        return FT_STATUS.FT_OK;
    }

    private Set<DetectorDevice> devices;

    /**
     * Поиск устройств в сети с переданным ID.
     *
     * @return
     */
    public FT_STATUS whoIsThere(Set<DetectorDevice> devices) {
        this.devices =devices;
        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WHO_IS_THERE)   //команда
                .append(FUNC_WHO_IS_THERE);    //функция
        grabber.sendMSG(msg.array(), broadcastIp, true);

        return FT_STATUS.FT_OK;
    }


    /**
     * Видео лист
     */
    private Map<Integer, Bytes> videoList = new HashMap<>();
    /**
     * Буфер сырой
     */
    private byte[] headerBuff = new byte[16];
    private byte[] videoBuff;

    private static int lastFrameID = -1;
    private static int currentFrameID = -1;

    private static int fragID;
    private int videoLen, reserv;
    private static int ro = -1;
    private static int width, heigth = -1;

    private static UDPInputStream is;


    private int bytesAvaible, videoBytesCount;

    /**
     * Запросполногокадра.
     *
     * @return
     */
    public Bytes nextFrame() {


        //Читаем заголовок
        if (is == null) {
            is = grabber.getVideoIS();
        }

        try {
            is.read(headerBuff);


            parseHeaders(headerBuff);

            bytesAvaible = is.available();

            videoBuff = new byte[videoLen];

            videoBytesCount = is.read(videoBuff, 0, bytesAvaible);//пишем первую часть видео из первого пакета


            if ((videoLen = videoLen - videoBytesCount) > 0) {//если есть еще фрагменты
                //продолжаем читать пакеты

                do {
                    is.skip(16);
                    bytesAvaible = is.available();
                    videoBytesCount = is.read(videoBuff, videoBytesCount, bytesAvaible);//пишем следующую часть видео из пакета

                } while ((videoLen = videoLen - videoBytesCount) > 0);


            }
        } catch (IOException e) {
            LOG.error("Error in DIS metods: {}", e.getMessage());
            return null;
        }
        return Bytes.wrap(videoBuff);
    }

    private DataInputStream createDataIS(byte[] inputArray) {
        ByteArrayInputStream bais = new ByteArrayInputStream(inputArray);
        DataInputStream dis = new DataInputStream(bais);
        return dis;
    }

    private void closeDIS(DataInputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseHeaders(byte[] headerBuff) throws IOException {

        DataInputStream dataIS = createDataIS(headerBuff);

        dataIS.skipBytes(2);

        currentFrameID = dataIS.readUnsignedShort();

        fragID = dataIS.readUnsignedShort();

        width = dataIS.readUnsignedShort();
        heigth = dataIS.readUnsignedShort();

        ro = dataIS.readUnsignedByte();

        reserv = dataIS.readUnsignedByte();

        videoLen = dataIS.readInt();

        closeDIS(dataIS);
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

    public boolean waitAnsvwer(UDPInputStream comIS, byte[] msg) throws IOException{

        byte[] bytes = new byte[4];
        Bytes wrapMsg = Bytes.wrap(bytes);
        int tick = 0;
        int availableBytes = -1;
        do {
            int read = 0;
            try {
                read = comIS.read(bytes);
                availableBytes = comIS.available();
            } catch (IOException e) {
                if (tick == 4) {
                    LOG.debug("Выход по тику {}",wrapMsg);
                    throw new IOException(e);
                }
            }
            if (read == 4) {//если подтверждение или ощибка

                //Читаем заголовок
                if (wrapMsg.startsWith(COM_HEADER.array())) {
                    if (isError(wrapMsg)) {
                        //Если ошибка, то повтор отправки сообщения сообщения
                        return false;
                    }

                    switch (wrapMsg.byteAt(2)) {

                        case COM_WHO_IS_THERE:
                            LOG.debug("COM_WHO_IS_THERE: {}", availableBytes);
                            if (!comIS.getClientIP().equals(myIp)) {
                                addDevises(comIS);
                            }
                            break;
                        case COM_WRITE:
                            tick = 5;
                            LOG.debug("COM_WRITE: {}", availableBytes);
                            comIS.skip(availableBytes);
                            break;
                        case COM_READ:
                            tick = 5;
                            LOG.debug("COM_READ: {}", availableBytes);
                            comIS.skip(availableBytes);
                            break;
                        case COM_BLK_WRITE:
                            tick = 5;
                            LOG.debug("COM_BLK_WRITE: {}", availableBytes);
                            comIS.skip(availableBytes);
                            break;
                        case COM_BLK_READ:
                            tick = 5;
                            LOG.debug("COM_BLK_READ: {}", availableBytes);
                            comIS.skip(availableBytes);
                            break;
                        default:
                            comIS.skip(availableBytes);
                            throw new IllegalStateException("Unexpected value: " + COM_HEADER.byteAt(2));
                    }

                } else {
                    //повторно отправить сообщение
                    LOG.error("AnswerMSG without Header, {}", wrapMsg);
                    comIS.skip(availableBytes);
                    return false;
                }

            }else{

                LOG.debug("Read empty Answer MSG {} try , {}",wrapMsg, tick);
                return false;
            }

        }
        while (tick++ < 5);
        return true;
    }

    private void addDevises(UDPInputStream clientIP) {

        String stringDetIP = clientIP.getClientIP().getHostAddress();
        int detPort = clientIP.getClientPort();
        String stringID = String.format("Ports C/V [%d/%d]", detPort, detPort + 1);
        ChinaDevice device = new ChinaDevice(stringDetIP, stringID, grabber);
        device.setResolution(grabber.getSize());

        devices.add(device);
        LOG.error("Добавлено устройство {}", device);

    }

    private boolean isError(Bytes wrapMsg) {
        byte byteAt = wrapMsg.byteAt(3);

        if (byteAt < 0) {
            //обработка ошибки
            LOG.error("ERROR MSG {}, ANSVER CODE: {}", wrapMsg, byteAt);
            return true;
        } else {
            //подтверждение записи
            LOG.debug("MSG {}, ANSVER function: {}", wrapMsg, byteAt);
        }
        return false;
    }

}
