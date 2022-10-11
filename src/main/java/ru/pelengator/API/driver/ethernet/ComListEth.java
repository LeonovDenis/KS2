package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.driver.FT_STATUS;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ComListEth {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ComListEth.class);

    private EthernetDriver grabber;

    public ComListEth(EthernetDriver grabber) {
        this.grabber = grabber;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////методы управления
    byte DEV_ID = 0x05;
    Bytes COM_HEADER = Bytes.from((byte) 0xA1, DEV_ID);
    byte[] VIDEO_HEADER = new byte[]{(byte) 0xA2, DEV_ID};
    byte WRITE = 0x01;


    byte WHO_IS_THERE = (byte) 0x00;
    byte ID = (byte) 0x01;
    byte POWER = (byte) 0x02;
    byte INT = (byte) 0x03;
    byte VR0 = (byte) 0x04;
    byte REF = (byte) 0x05;
    byte VOS = (byte) 0x06;
    byte CCC = (byte) 0x07;
    byte DIM = (byte) 0x08;


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
                .append(WRITE)          //команда
                .append(DIM)            //функция
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
                .append(WRITE)          //команда
                .append(CCC)            //функция
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
                .append(WRITE)          //команда
                .append(POWER)          //функция
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
                .append(WRITE)          //команда
                .append(INT)            //функция
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
                .append(WRITE)          //команда
                .append(VR0)            //функция
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
                .append(WRITE)          //команда
                .append(VOS)            //функция
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
                .append(WRITE)          //команда
                .append(REF)            //функция
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
                .append(WRITE)          //команда
                .append(ID)             //функция
                .append(1)              //размер данных
                .append(DEV_ID);        //данные
        grabber.sendMSG(msg.array());

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

    public boolean waitAnsvwer(UDPInputStream comIS, byte[] msg) throws IOException {
        byte[] bytes = new byte[4];

        int read = comIS.read(bytes);
        int availableBytes = comIS.available();

        if (read == 4 && availableBytes == 0) {
            if (bytes[3] < 0) {
                //обработка ошибки
                LOG.error("ERROR MSG {}, ANSVER CODE: {}", Arrays.toString(msg), Arrays.toString(bytes));
            } else {
                //подтверждение записи
                LOG.error("MSG {}, ANSVER CODE: {}", Arrays.toString(msg), Arrays.toString(bytes));
            }
        } else if (availableBytes > 0) {
            //обработка подтверждения
        }
        return true;
    }
}
