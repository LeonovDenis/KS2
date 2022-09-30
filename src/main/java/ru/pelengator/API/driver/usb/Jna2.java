package ru.pelengator.API.driver.usb;

import at.favre.lib.bytes.Bytes;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.pelengator.App.getFtd3XX;

/**
 * Драйвер с java на c++
 */
public class Jna2 {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Jna2.class);

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

    private static AtomicBoolean validHendler = new AtomicBoolean(false);
    private static AtomicBoolean needToWaite = new AtomicBoolean(false);

    private static AtomicBoolean isOpened = new AtomicBoolean(false);

    private ComList comList;


///////////////////////////////////////////////////////////////////////////////////

    /**
     * Конструктор
     */
    public Jna2() {
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
        FT_STATUS value = null;
        if (!isOpened.get()) {
            int i = FTD3XX.FTD3XX_INSTANCE.FT_Create("FTDI SuperSpeed-FIFO Bridge", (byte) 0x0A, hendler);
            //тест
            value = FT_STATUS.values()[i];

            if (value == FT_STATUS.FT_OK) {
                isOpened.compareAndSet(false, true);
                validHendler.compareAndSet(false, true);
            } else {
                validHendler.set(false);
            }
            LOG.error("Открытие обработчика обработчика {} {}", value, validHendler.get());
        } else {
            return FT_STATUS.FT_BUSY;
        }

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
        FT_STATUS status = FT_STATUS.values()[i];
        if (status != FT_STATUS.FT_OK) {
            validHendler.set(false);
        }
        return status;
    }

    /**
     * чтение массива
     *
     * @return Обернутый массив
     */
    public Bytes readPipe() {
        int i = FTD3XX.FTD3XX_INSTANCE.FT_ReadPipe(hendler.getPointer(0), (byte) 0x82, pBuff, MB, byteTrans, (Structure) null);
        byte[] byteArray = pBuff.getByteArray(0, (int) byteTrans.getValue());// из буфера в массив
        Bytes from = Bytes.from(byteArray);
        //тест
        FT_STATUS status = FT_STATUS.values()[i];
        if (status != FT_STATUS.FT_OK) {
            validHendler.set(false);
        }
        return from;
    }


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
        FT_STATUS ft_status1 = create();
        if (ft_status1 != FT_STATUS.FT_OK || ft_status1 != FT_STATUS.FT_BUSY) {
            return FT_STATUS.FT_DEVICE_LIST_NOT_READY;
        }

        FT_STATUS ft_status = setID();

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (ft_status == FT_STATUS.FT_OK) {
            online.compareAndSet(false, true);
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
        FT_STATUS value = null;
        if (isOpened.get()) {
            int i = FTD3XX.FTD3XX_INSTANCE.FT_Close(hendler.getPointer(0));
            value = FT_STATUS.values()[i];
            isOpened.set(false);
            validHendler.set(false);

            LOG.error("Закрытие обработчика {} {}", value, validHendler.get());
        } else {
            return FT_STATUS.FT_BUSY;
        }

        return value;
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////управление/////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
//todo
    private String name = "ДТ10Э";
    private String id = "ЕАНГ.468424.012";
    private static volatile AtomicBoolean flag_device_connected = new AtomicBoolean(true);

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
        LOG.trace("Запрос на получение Картинки из getImage до nextFrame");
        ByteBuffer bytes = null;
        Bytes frame = nextFrame();
        LOG.trace("Вывод полного кадра из getImage после nextFrame {}", frame);
        LOG.trace("isOnline: {}, validHendler {},needWaite {}", isOpened, validHendler, needToWaite);
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
        nextFrame();
        //todo возможно необходимо прочитать разок
        close();
    }

    private void waitToClearUSB() {
        Bytes bytes = null;
        do {
            bytes = nextFrame();

        } while (bytes != null);
        isMessageSend.compareAndSet(false, true);
    }

    /**
     * Запуск сессии
     *
     * @return
     */
    public FT_STATUS startSession() {
        comList = new ComList(this);
        LOG.info("Создан комлист");
        validHendler.compareAndSet(true, false);
        LOG.info("Старт сессии......");
        return FT_STATUS.FT_OK;
    }

    /**
     * Установка разрешения
     *
     * @param set - 0xFF -128*128 (true); 0x00 - 92*90
     * @return
     */
    public FT_STATUS setDimension(boolean set) {
        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        if (set) {
            size = DetectorResolution.CHINA.getSize();
        } else {
            size = DetectorResolution.CHINALOW.getSize();
        }
        return comList.setDimension(set);
    }

    /**
     * Установка коэф. усиления
     *
     * @param set - 0xFF -3 (true); 0x00 - 1
     * @return
     */
    public FT_STATUS setCapacity(boolean set) {
        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setCapacity(set);

    }

    /**
     * Подача питания RE
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setRE(boolean set) {
        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setRE(set);

    }

    /**
     * Подача питания VDD, VDDA
     *
     * @param set - 0xFF -включение (true); 0x00 - выключение
     * @return
     */
    public FT_STATUS setPower(boolean set) {
        FT_STATUS ft_status;
        if (set) {

            LOG.info("Создание обработчика в повере");
            FT_STATUS ft_status1 = create();
            if (ft_status1 == FT_STATUS.FT_BUSY) {
                nextFrame();
                needToWaite.set(true);
                ft_status = comList.setPower(set);
                nextFrame();
                return ft_status;
            } else if (ft_status1 != FT_STATUS.FT_OK) {
                LOG.error("Обработчик в повере не создался");
                return FT_STATUS.FT_EEPROM_NOT_PRESENT;
            }

            LOG.info("Установка ID в повере");
            if (setID() != FT_STATUS.FT_OK) {
                LOG.error("Установка  id не удалась в повере");
                return FT_STATUS.FT_BUSY;
            }
            if (isOpened.get()) {
                needToWaite.set(true);
            }
            //нужно раз прочитать
            nextFrame();
            needToWaite.set(true);
            ft_status = comList.setPower(set);
        } else {

            if (!validHendler.get()) {
                LOG.error("Hendler not valid");
                return FT_STATUS.FT_BUSY;
            } else {
                needToWaite.set(true);
                ft_status = comList.setPower(set);

            }
        }
        return ft_status;
    }

    public FT_STATUS setIntTime(int time) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setIntTime(time);
    }

    /**
     * Установка напр. смещения
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVR0(int value) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setVR0(value);
    }

    /**
     * Установка направления сканирования
     *
     * @param value - byte
     * @return
     */
    public FT_STATUS setRo(byte value) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setRo(value);
    }


    /**
     * Установка напр. антиблюминга
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA(int value) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setVVA(value);
    }

    /**
     * Установка рефов1
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA1(int value) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setVVA1(value);
    }

    /**
     * Установка рефов2
     *
     * @param value - в миливольтах
     * @return
     */
    public FT_STATUS setVVA2(int value) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setVVA2(value);
    }

    /**
     * Сброс устройства
     *
     * @param isReset - true - ресет
     * @return
     */
    public FT_STATUS setReset(boolean isReset) {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setReset(isReset);
    }

    /**
     * Установка id устройства
     *
     * @return
     */
    public FT_STATUS setID() {

        if (!validHendler.get()) {
            LOG.error("Hendler not valid");
            return FT_STATUS.FT_BUSY;
        }
        needToWaite.set(true);
        return comList.setID();
    }
private int countFR=0;
    public Bytes nextFrame() {
        Bytes bytes = null;
        if (validHendler.get()) {

            do {//todo возможно нужно еще раз прочитать
                bytes = comList.nextFrame();
                LOG.error("КАдр прочитан. Внутри while");
                if(countFR++>10){
                    needToWaite.set(false);
                    LOG.error("Выход принудительный. Внутри while");
                    countFR=0;
                }
            } while (needToWaite.get());
            LOG.error("обработчик валид. видео ок");
        } else {
            if (validHendler.get() && needToWaite.get() && isOpened.get()) {
                close();
            }
            //выход, кагда нет связи или старт без включения
            bytes = null;
            LOG.error("обработчик инвалид. видео нул");
        }
        return bytes;
    }

    public void clearBuffer() {
        comList.clearBuffer();

    }

    public FT_STATUS setParameters(Map<String, ?> params) {
        return FT_STATUS.FT_OK;
    }

    public static AtomicBoolean getNeedToWaite() {
        return needToWaite;
    }

    public static void setNeedToWaite(AtomicBoolean needToWaite) {
        Jna2.needToWaite = needToWaite;
    }

    public static AtomicBoolean getValidHendler() {
        return validHendler;
    }

    public static void setValidHendler(AtomicBoolean validHendler) {
        Jna2.validHendler = validHendler;
    }
}

