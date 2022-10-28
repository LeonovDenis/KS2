package ru.pelengator.API.driver.ethernet;

import at.favre.lib.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorException;
import ru.pelengator.API.devises.china.ChinaDevice;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.API.tasks.DetectorNetTask;

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
    private final Bytes VIDEO_HEADER = Bytes.from((byte) 0xA2, DEV_ID);

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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
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
        sendSynhMSG(msg.array());
        return FT_STATUS.FT_OK;
    }

    private Set<DetectorDevice> devices;

    /**
     * Поиск устройств в сети с переданным ID.
     *
     * @return
     */
    public FT_STATUS whoIsThere(Set<DetectorDevice> devices) {
        this.devices = devices;
        Bytes msg = COM_HEADER          //маска+ID
                .append(COM_WHO_IS_THERE)   //команда
                .append(FUNC_WHO_IS_THERE);    //функция
        sendSynhBroadcastMSG(msg.array());
        return FT_STATUS.FT_OK;
    }


    /**
     * Видео лист
     */
    private Map<Integer, Bytes> videoList = new HashMap<>();
    /**
     * Буфер сырой
     */
    private int HEADER_SIZE = 16;
    private byte[] headerBuff = new byte[HEADER_SIZE];
    private byte[] videoBuff;

    private static int lastFrameID = -1;
    private static int currentFrameID = -1;

    private static int fragID;
    private int videoLen, reserv;
    private static int ro = -1;
    private static int width, heigth = -1;

    private static UDPInputStream is;


    private int bytesAvaible, lastVideoSize;

    /**
     * Запросполногокадра.
     *
     * @return
     */
    public Bytes nextFrame() {

        //Получаем поток
        if (is == null) {
            is = grabber.getVideoIS();
        }

        try {
            //Копим заголовок заголовок 16 байт
            readHeader();

            //Парсим всё в заголовке
            parseFirstHeaders(headerBuff);

            //Пороверяем оставшиеся количество байт
            bytesAvaible = is.available();
            LOG.debug("VIDEO_DATA_SIZE {}",bytesAvaible);
            assert videoLen > 0;

            if (videoBuff != null && videoBuff.length == videoLen) {
                //continue
            } else {
                videoBuff = new byte[videoLen];
                LOG.debug("CREATE BUFFER {}",bytesAvaible);
            }

            lastVideoSize = is.read(videoBuff, 0, bytesAvaible);//пишем первую часть видео из первого пакета

            LOG.debug("WRITED in BUFFER {} bytes",lastVideoSize);

            if ((videoLen = videoLen - lastVideoSize) > 0) {//если есть еще фрагменты
                //продолжаем читать пакеты
                LOG.debug("NEED MORE DATA {} bytes",videoLen);
                do {
                    LOG.debug("READ NEXT PART");
                    //читаем следующую часть
                    readHeader();
                    parseSecondHeaders(headerBuff);

                    bytesAvaible = is.available();
                    LOG.debug("VIDEO_DATA_SIZE {}",bytesAvaible);
                    lastVideoSize = is.read(videoBuff, lastVideoSize, bytesAvaible);//пишем следующую часть видео из пакета
                    LOG.debug("WRITED in BUFFER {} bytes",lastVideoSize);
                } while ((videoLen = videoLen - lastVideoSize) > 0);

                LOG.debug("PARTS ENDED. NEED to read {} bytes",videoLen);
            }else{
                LOG.debug("NO SECOND PART");
            }

        } catch (IOException e) {
            LOG.debug("Error in DIS metods: {}. No video data.", e.getMessage());
            return null;
        } catch (RuntimeException e) {

            LOG.debug("Error in DIS metods: {}", e.getMessage());
            return null;
        } finally {

            try {
                int available = is.available();
                if (available > 0) {
                    is.skip(available);
                }
            } catch (IOException e) {
                //ignore
            }
        }

        if (grabber.getHeight() * grabber.getWidth() != heigth * width) {
            LOG.error("Error in DIS metods: resolutions not same detector resolution {}x{} / video resolution  {}x{}",
                    grabber.getWidth(), grabber.getHeight(), width, heigth);
            return null;
        }

        LOG.debug("FRAME READY: FRAME {}, PATS {}",currentFrameID,fragID);
        return Bytes.wrap(videoBuff);
    }

    private void parseSecondHeaders(byte[] headerBuff) throws IOException, RuntimeException {
        DataInputStream dataIS = createDataIS(headerBuff);

        byte[] tempHeader = new byte[2];
        dataIS.read(tempHeader);
        //Если заголовок не совпадает, тогда кидаем исключение
        if (!VIDEO_HEADER.startsWith(tempHeader)) {
            LOG.debug("Packege without valid Header");
            throw new DetectorException("Packege without valid Header");
        }

        int tempFrameID = dataIS.readUnsignedShort();
        if (currentFrameID != tempFrameID) {
            LOG.debug("FrameID first and second part not valid");
            throw new DetectorException("FrameID first and second part not valid");
        }

        int tempFragID = dataIS.readUnsignedShort();
        if (fragID != tempFragID - 1) {
            LOG.debug("FragID second part not valid");
            throw new DetectorException("FragID second part not valid");
        } else {
            LOG.debug("FragID second part valid");
            fragID = tempFragID;

        }

        int tempWidth = dataIS.readUnsignedShort();
        int tempHeigth = dataIS.readUnsignedShort();

        if (width != tempWidth && heigth != tempHeigth) {
            LOG.debug("Resolution second part not valid");
            throw new DetectorException("Resolution second part not valid");
        }

        int tempRo = dataIS.readUnsignedByte();
        int tempReserv = dataIS.readUnsignedByte();

        if (ro != tempRo && reserv != tempReserv) {
            LOG.debug("Ro and Reserv second part not valid");
            throw new DetectorException("Ro and Reserv second part not valid");
        }

        int tempVideoLen = dataIS.readInt();


        LOG.debug("Incoming second video part: HEADER/DEV_ID {}, FRAME_ID {}, FRAG_ID {}, " +
                        "WIDTH {}, HEIGTH  {}, RO {}, RESERV {}, DLEN {}.", VIDEO_HEADER, tempFrameID, tempFragID,
                tempWidth, tempHeigth, tempRo, tempReserv, tempVideoLen);

        closeDIS(dataIS);

    }

    private void readHeader() throws IOException, RuntimeException {
        int read = is.read(headerBuff);

        if (read < HEADER_SIZE) {
            LOG.debug("Package size not valid");
            throw new DetectorException("Package size not valid");
        }

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

    private void parseFirstHeaders(byte[] headerBuff) throws IOException, RuntimeException {

        DataInputStream dataIS = createDataIS(headerBuff);

        byte[] tempHeader = new byte[2];
        dataIS.read(tempHeader);
        //Если заголовок не совпадает, тогда кидаем исключение
        if (!VIDEO_HEADER.startsWith(tempHeader)) {
            LOG.debug("Packege without valid Header");
            throw new DetectorException("Packege without valid Header");

        }

        currentFrameID = dataIS.readUnsignedShort();


        fragID = dataIS.readUnsignedShort();
        if (fragID != 0) {
            LOG.debug("Not first fragment not null in Video data package");
            throw new DetectorException("Not first fragment not null in Video data package");
        }

        width = dataIS.readUnsignedShort();
        heigth = dataIS.readUnsignedShort();

        ro = dataIS.readUnsignedByte();
        reserv = dataIS.readUnsignedByte();

        videoLen = dataIS.readInt();
        if (videoLen != width * heigth * 2) {
            LOG.debug("Video data not for this resolution");
            throw new DetectorException("Video data not for this resolution");
        }

        LOG.debug("Incoming first video part: HEADER/DEV_ID {}, FRAME_ID {}, FRAG_ID {}, " +
                        "WIDTH {}, HEIGTH  {}, RO {}, RESERV {}, DLEN {}.", VIDEO_HEADER, currentFrameID, fragID, width,
                heigth, ro, reserv, videoLen);
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

    private static int tick = 0;

    /**
     * Чтение ответа
     *
     * @param comIS входной поток
     * @param msg   отправленное сообщение
     * @return принятое сообщение, null если формат подтверждения не подходит
     * @throws IOException, если нет ответа
     */
    public byte[] waitAnswer(UDPInputStream comIS, byte[] msg) throws IOException {
        tick = 0;
        byte[] bAnswer = null;

        byte[] bytes = new byte[4];
        Bytes wrapMsg = Bytes.wrap(bytes);

        int availableBytes = -1;
        do {
            int read = 0;

            try {
                read = comIS.read(bytes);
                availableBytes = comIS.available();
            } catch (IOException e) {
                //выход в случае отсутствия ответа
                if (tick == 3) {
                    LOG.debug("Throw IOException {}", e.getMessage());
                    throw new IOException(e);
                }
            }


            if (read == 4 && (!comIS.getClientIP().equals(myIp))) {//если подтверждение, ошибка, пришли данные

                //Читаем заголовок
                if (wrapMsg.startsWith(COM_HEADER.array())) {

                    if (isError(wrapMsg, msg)) {// проверяем на наличие ошибки
                        LOG.debug("Error in answer {} /tick {}", wrapMsg, tick);
                        throw new IOException("Error MSG: " + wrapMsg);
                    }

                    //раскадровка по командам
                    switch (wrapMsg.byteAt(2)) {

                        case COM_WHO_IS_THERE:
                            LOG.debug("COM_WHO_IS_THERE: {}", availableBytes);

                            addDevises(comIS);

                            break;
                        case COM_WRITE:

                            LOG.debug("COM_WRITE: {}", availableBytes);
                            bAnswer = parseAnswer(comIS, wrapMsg, msg);

                            break;
                        case COM_READ:
                            LOG.debug("COM_READ: {}", availableBytes);
                            bAnswer = parseAnswer(comIS, wrapMsg, msg);
                            break;
                        case COM_BLK_WRITE:
                            LOG.debug("COM_BLK_WRITE: {}", availableBytes);
                            bAnswer = parseAnswer(comIS, wrapMsg, msg);
                            break;
                        case COM_BLK_READ:
                            LOG.debug("COM_BLK_READ: {}", availableBytes);
                            bAnswer = parseAnswer(comIS, wrapMsg, msg);
                            break;
                        default:
                            comIS.skip(availableBytes);
                            throw new IllegalStateException("Unexpected value: " + COM_HEADER.byteAt(2));
                    }

                } else {
                    //повторно отправить сообщение
                    LOG.error("AnswerMSG without Header {} try : {}", wrapMsg, tick);
                    comIS.skip(availableBytes);
                }

            } else {
                //Неправильное сообщение
                LOG.debug("No answer, or my IP, MSG / {} / try : {}", wrapMsg, tick);
            }

        }
        while (tick++ < 3);

        return bAnswer;
    }

    /**
     * Распарсивание принятого сообщения.
     *
     * @param comIS  входной поток
     * @param header ШАПКА
     * @param outMSG отправленное сообщение
     * @return ответное сообщение
     */
    private byte[] parseAnswer(UDPInputStream comIS, Bytes header, byte[] outMSG) throws IOException {

        //////////////////////////////////////////////////////////////////
        //   |b1       |b0      |b0     |b0     /|b3 b2 b1 b0	|VAR    //
        //   |HEADER   |DEV_ID	|CMD	|FUNC	/|DLEN          |DATA   //
        //////////////////////////////////////////////////////////////////

        Bytes wrapOutMSG = Bytes.wrap(outMSG);

        boolean compare = wrapOutMSG.byteAt(2) == header.byteAt(2) &&
                wrapOutMSG.byteAt(3) == header.byteAt(3);


        int available = comIS.available();

        if (available == 0) {
            //пришло подтверждение сообщения

            if (compare) {
                //если подтверждение, то возвращаем его
                tick = 5;
                return header.array();
            } else {
                //если не совпадает, то возвращаем null и идет повтор
                return null;
            }

        } else if (available >= 4) {
            //пришли данные

            byte[] dlen = new byte[4];//длина данных
            comIS.read(dlen);
            Bytes wrapdlen = Bytes.wrap(dlen);

            int valueLength = wrapdlen.toInt();

            byte[] value = new byte[available - 4]; //данные
            comIS.read(value);
            Bytes wrapValue = Bytes.wrap(value);

            byte func = header.byteAt(3);

            switch (func) {
                case (0x01):
                    LOG.debug("ID setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x02):
                    LOG.debug("Power setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x03):
                    LOG.debug("Int time setted in: (int) {}", wrapValue.toInt());
                    break;
                case (0x04):
                    LOG.debug("VR0 setted in: (int) {}", wrapValue.toInt());
                    break;
                case (0x05):
                    LOG.debug("REF setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x06):
                    LOG.debug("VVA setted in: (int) {}", wrapValue.toInt());
                    break;
                case (0x07):
                    LOG.debug("Cap setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x08):
                    LOG.debug("Dim setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x09):
                    LOG.debug("Reset setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x0A):
                    LOG.debug("Flip mode setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x0B):
                    LOG.debug("Spec power setted in: (int[]) {}", Arrays.toString(wrapValue.toIntArray()));
                    break;
                case (0x0C):
                    LOG.debug("Sensor temp setted in: (int) {}", wrapValue.toInt());
                    break;
                case (0x0D):
                    LOG.debug("M_power setted in: (byte) {}", wrapValue.toByte());
                    break;
                case (0x0E):
                    LOG.debug("Property setted in: (byte[]) {}", wrapValue);
                    break;
                default:
                    throw new IllegalStateException("Unexpected func value: " + func);
            }
            tick = 5;
            return value;
        }
        return null;
    }

    /**
     * Добавление устройства в список.
     *
     * @param clientIP IP ответившего устройства
     */
    private void addDevises(UDPInputStream clientIP) {

        String stringDetIP = clientIP.getClientIP().getHostAddress();
     //   int detPort = clientIP.getClientPort();
      //  String stringID = String.format("Ports C/V [%d/%d]", detPort, detPort + 1);
        String stringID="[ДТ10Э]";
        ChinaDevice device = new ChinaDevice(stringDetIP, stringID, grabber);
        device.setResolution(grabber.getSize());

        devices.add(device);
        LOG.trace("Added device: {}", device);

    }

    /**
     * Распарсивание на наличие ошибок
     *
     * @param wrapMsg входящее сообщение
     * @param msg
     * @return
     */
    private boolean isError(Bytes wrapMsg, byte[] msg) {
        byte byteAt = wrapMsg.byteAt(3);

        if (byteAt < 0) {
            //обработка ошибки
            switch (byteAt) {
                case (byte) 0x81:
                    LOG.debug("Error in MSG {} - ILLEGAL COMAND", Arrays.toString(msg));
                    break;
                case (byte) 0x82:
                    LOG.debug("Error in MSG {} - ILLEGAL FUNCTION", Arrays.toString(msg));
                    break;
                case (byte) 0x83:
                    LOG.debug("Error in MSG {} - ILLEGAL DATA VALUE", Arrays.toString(msg));
                    break;
                case (byte) 0x84:
                    LOG.debug("Error in MSG {} - SERVER DEVICE FAILURE", Arrays.toString(msg));
                    break;
                case (byte) 0x85:
                    LOG.debug("Error in MSG {} - ACKNOWLEDGE", Arrays.toString(msg));
                    break;
                case (byte) 0x86:
                    LOG.debug("Error in MSG {} - SERVER DEVICE BUSY", Arrays.toString(msg));
                    break;
                case (byte) 0x87:
                    LOG.debug("Error in MSG {} - MEMORY PARITY ERROR", Arrays.toString(msg));
                    break;
                case (byte) 0x88:
                    LOG.debug("Error in MSG {} - RESERV", Arrays.toString(msg));
                    break;
                default:
                    throw new IllegalStateException("Unexpected error value: " + byteAt);
            }
            return true;
        } else {
            //подтверждение записи
        }
        return false;
    }

    /**
     * Отправка синхронного сообщения на детектор
     *
     * @param MSG сообщение
     * @return ответ от устройства. null если нет ответа
     */
    private byte[] sendSynhMSG(byte[] MSG) {

        DetectorNetTask detectorNetTask = new DetectorNetTask(grabber.getDriver(), null, MSG);
        byte[] incMSG = detectorNetTask.sendCMD();

        return incMSG;
    }

    /**
     * Отправка броадкаст сообщения.
     *
     * @param MSG сообщение
     * @return ответ
     */
    private byte[] sendSynhBroadcastMSG(byte[] MSG) {

        DetectorNetTask detectorNetTask = new DetectorNetTask(grabber.getDriver(), null, MSG, broadcastIp);
        byte[] incMSG = detectorNetTask.sendCMD();

        return incMSG;
    }

}
