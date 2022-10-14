package ru.pelengator.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.App;
import ru.pelengator.Controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * Класс хранения параметров стенда.
 */
public class StendParams {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StendParams.class);
    /**
     * Количество кадров на выборку.
     */
    private int countFrames = 128;
    /**
     * Первая температура.
     */
    private double temp0 = 0;
    /**
     * Вторая температура.
     */
    private double temp1 = 0;
    /**
     * Площадь диафрагмы АЧТ.
     */
    private double areaACHT0 = 0;
    private double areaACHT1 = 0;
    /**
     * Площадь фоточувствительного элемента.
     */
    private double areaFPU0 = 0;
    private double areaFPU1 = 0;
    /**
     * Расстояние от АЧТ до фпу.
     */
    private double rasstACHTfpu0 = 0;
    private double rasstACHTfpu1 = 0;
    /**
     * Поток при первых условиях.
     */
    private double potok0 = 0;
    private double potok1 = 0;
    /**
     * Итоговый поток.
     */
    private double potok = 0;
    /**
     * Степень черноты АЧТ.
     */
    private double epsilin0 = 0;
    private double epsilin1 = 0;
    /**
     * Постоянная планка.
     */
    private double plank0 = 0;
    private double plank1 = 0;
    /**
     * Коэффициет поправки потока.
     */
    private double betta0 = 0;
    private double betta1 = 0;
    /**
     * Частота эффективная.
     */
    private double fEfect = 0;
    /**
     * Облученность.
     */
    private double exposure = 0;

    ///////////Состояния/////
    private boolean tempPower = false;//Нажата ли кнопка ПОВЕР
    private int tempInt = 500; //время интегрирования
    private int tempREF = 1600; // напряжение референса два
    private int tempVOS = 1600; // напряжение скимминга
    private int tempVR0 = 20; //Напряжение смещения
    private boolean tempKU = false; //Коэф. усиления

    private String dimention = "128*128";
    /////
    private double arifmeticMeanPersent = 20.00;
    private double quadraticMeanPersent = 20.00;
    private double SKOPersent = 20.00;
    private double vwPersent = 20.00;
    private double porogPersent = 20.00;
    private double porogStarPersent = 20.00;
    private double detectivityPersent = 20.00;
    private double detectivityStarPersent = 20.00;
    private double NETDPersent = 20.00;
    private double exposurePersent = 20.00;
    private String persentColorArifm = "#ffffff";
    private String persentColorQuadratic = "#ffffff";
    private String persentColorSKO = "#ffffff";
    private String persentColorVw = "#ffffff";
    private String persentColorPorog = "#ffffff";
    private String persentColorPorogStar = "#ffffff";
    private String persentColorDetectivity = "#ffffff";
    private String persentColorDetectivityStar = "#ffffff";
    private String persentColorNETD = "#ffffff";
    private String persentColorExposure = "#ffffff";
    private String itogColor = "#ffffff";

    private StringProperty zakaz = new SimpleStringProperty();
    private StringProperty dogovor = new SimpleStringProperty();
    private StringProperty metodika = new SimpleStringProperty();
    private StringProperty nomer_0 = new SimpleStringProperty();
    private StringProperty nomer = new SimpleStringProperty();
    private StringProperty copy = new SimpleStringProperty();
    private StringProperty otk = new SimpleStringProperty();
    private StringProperty data = new SimpleStringProperty();

    private StringProperty TXT_0_0 = new SimpleStringProperty();

    private StringProperty TXT_0_1 = new SimpleStringProperty();

    private StringProperty TXT_0_2 = new SimpleStringProperty();
    private StringProperty TXT_0_3 = new SimpleStringProperty();
    private StringProperty TXT_0_4 = new SimpleStringProperty();
    private StringProperty TXT_0_5 = new SimpleStringProperty();
    private StringProperty TXT_0_6 = new SimpleStringProperty();
    private StringProperty TXT_0_7 = new SimpleStringProperty();
    private StringProperty TXT_0_8 = new SimpleStringProperty();
    private StringProperty TXT_0_9 = new SimpleStringProperty();

    private BooleanProperty tbExel = new SimpleBooleanProperty();
    private BooleanProperty tbTxt = new SimpleBooleanProperty();
    private BooleanProperty tbPdf = new SimpleBooleanProperty();

    private BooleanProperty cbWithBP = new SimpleBooleanProperty();
    private BooleanProperty cbPrint = new SimpleBooleanProperty();

    private int PAUSE = 0;

    private int detPortVideo;
    private int detPortCommand;
    private String detIP;

    private int serverVideoBuff;
    private int commandBuff;

    private NetworkInfo selNetworkInterface;

    Properties properties = new Properties();//Ссыль на настройки

    private Controller controller;

    public StendParams(Controller controller) {
        this.controller = controller;
    }

    public void loadParams(String PATH) {
        LOG.trace("Load params");
        File file = new File(PATH);
        Path path = Paths.get(PATH);
        if (file.exists()) {
            try {
                properties.load(Files.newBufferedReader(path));
            } catch (IOException e) {
                LOG.error("Error in load params file {}", e);
            }
        } else {
            try {
                properties.load(App.class.getResourceAsStream(PATH));
            } catch (IOException e) {
                LOG.error("Error in load params in zip file {}", e);
            }
        }


        zakaz.set(properties.getProperty("zakaz", "\"АО Дукс\""));
        dogovor.set(properties.getProperty("dogovor", "9-2021/051 от 06.10.2021 г."));
        metodika.set(properties.getProperty("metodika", "Отдельная методика"));
        nomer_0.set(properties.getProperty("nomer_0", "001"));
        nomer.set(properties.getProperty("nomer", "FL001"));
        copy.set(properties.getProperty("copy", "FL002"));
        otk.set(properties.getProperty("otk", "Иванов"));
        data.set(properties.getProperty("data", "0x8C140001"));

        TXT_0_0.set(properties.getProperty("TXT_0_0", "---"));// среднее арифм.
        TXT_0_1.set(properties.getProperty("TXT_0_1", "---"));// среднее квадратическое
        TXT_0_2.set(properties.getProperty("TXT_0_2", "---"));// средний шум СКО
        TXT_0_3.set(properties.getProperty("TXT_0_3", "---"));// вольтовая чувствительность
        TXT_0_4.set(properties.getProperty("TXT_0_4", "---"));// порог чувствительности
        TXT_0_5.set(properties.getProperty("TXT_0_5", "---"));// удельный
        TXT_0_6.set(properties.getProperty("TXT_0_6", "---"));// обнаружительная способность
        TXT_0_7.set(properties.getProperty("TXT_0_7", ">8.0Е+09"));// удельная обнаружительная
        TXT_0_8.set(properties.getProperty("TXT_0_8", "---"));// ЭШРТ
        TXT_0_9.set(properties.getProperty("TXT_0_9", "<5.0Е-08"));//пороговая облученность


        tempPower = Boolean.parseBoolean(properties.getProperty("POWER_ON", "true"));
        tempInt = Integer.parseInt(properties.getProperty("INT", "500"));
        controller.getTfInt().setText(String.valueOf(tempInt));
        tempREF = Integer.parseInt(properties.getProperty("REF", "1600"));
        tempVOS = Integer.parseInt(properties.getProperty("VOS", "1600"));
        controller.getTfVOS().setText(String.valueOf(tempVOS));
        tempVR0 = Integer.parseInt(properties.getProperty("VR0", "20"));
        controller.getTfVR0().setText(String.valueOf(tempVR0));
        tempKU = Boolean.parseBoolean(properties.getProperty("KU", "true"));
        controller.getCbCCCOptions().getSelectionModel().select(tempKU ? 1 : 0);

        PAUSE = Integer.parseInt(properties.getProperty("PAUSE", "50"));
        controller.getTfSpeedPlata().setText(String.valueOf(PAUSE));

        temp0 = Double.parseDouble(properties.getProperty("temp0", "303.0"));
        temp1 = Double.parseDouble(properties.getProperty("temp1", "513.0"));
        areaACHT0 = Double.parseDouble(properties.getProperty("diamACHT0", "1.962e-03"));
        areaACHT1 = Double.parseDouble(properties.getProperty("diamACHT1", "1.962e-03"));
        areaFPU0 = Double.parseDouble(properties.getProperty("areaFPU0", "9.000e-10"));
        areaFPU1 = Double.parseDouble(properties.getProperty("areaFPU1", "9.000e-10"));
        rasstACHTfpu0 = Double.parseDouble(properties.getProperty("rasstACHTfpu0", "4.200e-01"));
        rasstACHTfpu1 = Double.parseDouble(properties.getProperty("rasstACHTfpu1", "4.200e-01"));
        exposure = Double.parseDouble(properties.getProperty("exposure", "0"));
        potok = Double.parseDouble(properties.getProperty("potok", "0"));
        potok0 = Double.parseDouble(properties.getProperty("potok0", "0"));
        potok1 = Double.parseDouble(properties.getProperty("potok1", "0"));
        epsilin0 = Double.parseDouble(properties.getProperty("epsilin0", "9.500e-01"));
        epsilin1 = Double.parseDouble(properties.getProperty("epsilin1", "9.500e-01"));
        plank0 = Double.parseDouble(properties.getProperty("plank0", "5.670e-08"));
        plank1 = Double.parseDouble(properties.getProperty("plank1", "5.670e-08"));
        betta0 = Double.parseDouble(properties.getProperty("betta0", "2.075e-01"));
        betta1 = Double.parseDouble(properties.getProperty("betta1", "2.075e-01"));

        fEfect = Double.parseDouble(properties.getProperty("fEfect", "0"));
        countFrames = Integer.parseInt(properties.getProperty("countFrames", "128"));

        arifmeticMeanPersent = Double.parseDouble(properties.getProperty("arifmeticMeanPersent", "20.00"));
        quadraticMeanPersent = Double.parseDouble(properties.getProperty("quadraticMeanPersent", "20.00"));
        SKOPersent = Double.parseDouble(properties.getProperty("SKOPersent", "20.00"));
        vwPersent = Double.parseDouble(properties.getProperty("vwPersent", "20.00"));
        porogPersent = Double.parseDouble(properties.getProperty("porogPersent", "20.00"));
        porogStarPersent = Double.parseDouble(properties.getProperty("porogStarPersent", "20.00"));
        detectivityPersent = Double.parseDouble(properties.getProperty("detectivityPersent", "20.00"));
        detectivityStarPersent = Double.parseDouble(properties.getProperty("detectivityStarPersent", "20.00"));
        NETDPersent = Double.parseDouble(properties.getProperty("NETDPersent", "20.00"));
        exposurePersent = Double.parseDouble(properties.getProperty("exposurePersent", "20.00"));

        persentColorArifm = properties.getProperty("persentColorArifm", "#FFFFFF");
        persentColorQuadratic = properties.getProperty("persentColorQuadratic", "#FFFFFF");
        persentColorSKO = properties.getProperty("persentColorSKO", "#FFFFFF");
        persentColorVw = properties.getProperty("persentColorVw", "#FFFFFF");
        persentColorPorog = properties.getProperty("persentColorPorog", "#FFFFFF");
        persentColorPorogStar = properties.getProperty("persentColorPorogStar", "#FFFFFF");
        persentColorDetectivity = properties.getProperty("persentColorDetectivity", "#FFFFFF");
        persentColorDetectivityStar = properties.getProperty("persentColorDetectivityStar", "#FFFFFF");
        persentColorNETD = properties.getProperty("persentColorNETD", "#FFFFFF");
        persentColorExposure = properties.getProperty("persentColorExposure", "#FFFFFF");
        itogColor = properties.getProperty("itogColor", "#FFFFFF");

        tbExel.set(Boolean.parseBoolean(properties.getProperty("tbExel", "true")));
        tbTxt.set(Boolean.parseBoolean(properties.getProperty("tbTxt", "true")));
        tbPdf.set(Boolean.parseBoolean(properties.getProperty("tbPdf", "true")));
        cbWithBP.set(Boolean.parseBoolean(properties.getProperty("cbWithBP", "true")));
        cbPrint.set(Boolean.parseBoolean(properties.getProperty("cbPrint", "true")));


        ////// сеть
        detPortVideo = Integer.parseInt(properties.getProperty("detPortVideo", "53"));
        detPortCommand = Integer.parseInt(properties.getProperty("detPortCommand", "54"));
        detIP = properties.getProperty("detIP", "127.0.0.1");

        serverVideoBuff = Integer.parseInt(properties.getProperty("serverVideoBuff", "1024"));
        commandBuff = Integer.parseInt(properties.getProperty("commandBuff", "1024"));


    }

    public synchronized void save() {
        save("props.properties");
    }

    private void save(String PATH) {
        LOG.trace("Save params");

        properties.setProperty("zakaz", zakaz.getValue());
        properties.setProperty("dogovor", dogovor.getValue());
        properties.setProperty("metodika", metodika.getValue());
        properties.setProperty("nomer_0", nomer_0.getValue());
        properties.setProperty("nomer", nomer.getValue());
        properties.setProperty("copy", copy.getValue());
        properties.setProperty("otk", otk.getValue());
        properties.setProperty("data", data.getValue());

        properties.setProperty("TXT_0_0", TXT_0_0.getValue());
        properties.setProperty("TXT_0_1", TXT_0_1.getValue());
        properties.setProperty("TXT_0_2", TXT_0_2.getValue());
        properties.setProperty("TXT_0_3", TXT_0_3.getValue());
        properties.setProperty("TXT_0_4", TXT_0_4.getValue());
        properties.setProperty("TXT_0_5", TXT_0_5.getValue());
        properties.setProperty("TXT_0_6", TXT_0_6.getValue());
        properties.setProperty("TXT_0_7", TXT_0_7.getValue());
        properties.setProperty("TXT_0_8", TXT_0_8.getValue());
        properties.setProperty("TXT_0_9", TXT_0_9.getValue());

        properties.setProperty("POWER_ON", String.valueOf(tempPower));
        properties.setProperty("INT", String.valueOf(tempInt));
        properties.setProperty("REF", String.valueOf(tempREF));
        properties.setProperty("VOS", String.valueOf(tempVOS));
        properties.setProperty("VR0", String.valueOf(tempVR0));
        properties.setProperty("KU", String.valueOf(tempKU));

        properties.setProperty("PAUSE", String.valueOf(PAUSE));

//////////////////////////////////////////////////////////////////////////////////////
        String text = String.format(Locale.CANADA, "%.1f", temp0);
        properties.setProperty("temp0", text);
        text = String.format(Locale.CANADA, "%.1f", temp1);
        properties.setProperty("temp1", text);

        text = String.format(Locale.CANADA, "%.3e", areaACHT0);
        properties.setProperty("diamACHT0", text);
        text = String.format(Locale.CANADA, "%.3e", areaACHT1);
        properties.setProperty("diamACHT1", text);

        text = String.format(Locale.CANADA, "%.3e", areaFPU0);
        properties.setProperty("areaFPU0", text);
        text = String.format(Locale.CANADA, "%.3e", areaFPU1);
        properties.setProperty("areaFPU1", text);

        text = String.format(Locale.CANADA, "%.3e", rasstACHTfpu0);
        properties.setProperty("rasstACHTfpu0", text);
        text = String.format(Locale.CANADA, "%.3e", rasstACHTfpu1);
        properties.setProperty("rasstACHTfpu1", text);

        text = String.format(Locale.CANADA, "%.3e", exposure);
        properties.setProperty("exposure", text);

        text = String.format(Locale.CANADA, "%.3e", potok);
        properties.setProperty("potok", text);
        text = String.format(Locale.CANADA, "%.3e", potok0);
        properties.setProperty("potok0", text);
        text = String.format(Locale.CANADA, "%.3e", potok1);
        properties.setProperty("potok1", text);

        text = String.format(Locale.CANADA, "%.3e", epsilin0);
        properties.setProperty("epsilin0", text);
        text = String.format(Locale.CANADA, "%.3e", epsilin1);
        properties.setProperty("epsilin1", text);

        text = String.format(Locale.CANADA, "%.3e", plank0);
        properties.setProperty("plank0", text);
        text = String.format(Locale.CANADA, "%.3e", plank1);
        properties.setProperty("plank1", text);

        text = String.format(Locale.CANADA, "%.3e", betta0);
        properties.setProperty("betta0", text);
        text = String.format(Locale.CANADA, "%.3e", betta1);
        properties.setProperty("betta1", text);

        text = String.format(Locale.CANADA, "%.3e", fEfect);
        properties.setProperty("fEfect", text);

        text = String.format(Locale.CANADA, "%d", countFrames);
        properties.setProperty("countFrames", text);

        text = String.format(Locale.CANADA, "%.2f", arifmeticMeanPersent);
        properties.setProperty("arifmeticMeanPersent", text);
        text = String.format(Locale.CANADA, "%.2f", quadraticMeanPersent);
        properties.setProperty("quadraticMeanPersent", text);
        text = String.format(Locale.CANADA, "%.2f", SKOPersent);
        properties.setProperty("SKOPersent", text);
        text = String.format(Locale.CANADA, "%.2f", vwPersent);
        properties.setProperty("vwPersent", text);
        text = String.format(Locale.CANADA, "%.2f", porogPersent);
        properties.setProperty("porogPersent", text);
        text = String.format(Locale.CANADA, "%.2f", porogStarPersent);
        properties.setProperty("porogStarPersent", text);
        text = String.format(Locale.CANADA, "%.2f", detectivityPersent);
        properties.setProperty("detectivityPersent", text);
        text = String.format(Locale.CANADA, "%.2f", detectivityStarPersent);
        properties.setProperty("detectivityStarPersent", text);
        text = String.format(Locale.CANADA, "%.2f", NETDPersent);
        properties.setProperty("NETDPersent", text);
        text = String.format(Locale.CANADA, "%.2f", exposurePersent);
        properties.setProperty("exposurePersent", text);

        properties.setProperty("persentColorArifm", persentColorArifm);
        properties.setProperty("persentColorQuadratic", persentColorQuadratic);
        properties.setProperty("persentColorSKO", persentColorSKO);
        properties.setProperty("persentColorVw", persentColorVw);
        properties.setProperty("persentColorPorog", persentColorPorog);
        properties.setProperty("persentColorPorogStar", persentColorPorogStar);
        properties.setProperty("persentColorDetectivity", persentColorDetectivity);
        properties.setProperty("persentColorDetectivityStar", persentColorDetectivityStar);
        properties.setProperty("persentColorNETD", persentColorNETD);
        properties.setProperty("persentColorExposure", persentColorExposure);

        properties.setProperty("itogColor", itogColor);
        properties.setProperty("tbExel", String.valueOf(tbExel.getValue()));
        properties.setProperty("tbTxt", String.valueOf(tbTxt.getValue()));
        properties.setProperty("tbPdf", String.valueOf(tbPdf.getValue()));
        properties.setProperty("cbPrint", String.valueOf(cbPrint.getValue()));
        properties.setProperty("cbWithBP", String.valueOf(cbWithBP.getValue()));

        properties.setProperty("detPortVideo", String.valueOf(detPortVideo));
        properties.setProperty("detPortCommand", String.valueOf(detPortCommand));
        properties.setProperty("detIP", String.valueOf(detIP));
        properties.setProperty("serverVideoBuff", String.valueOf(serverVideoBuff));
        properties.setProperty("commandBuff", String.valueOf(commandBuff));


        Path of = Paths.get(PATH);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(of)) {
            properties.store(bufferedWriter, "");
        } catch (IOException e) {
            LOG.error("Error in save params to file {}", e);
        }
    }

    public int getCountFrames() {
        return countFrames;
    }

    public void setCountFrames(int countFrames) {
        this.countFrames = countFrames;
    }

    public double getTemp0() {
        return temp0;
    }

    public void setTemp0(double temp0) {
        this.temp0 = temp0;
    }

    public double getTemp1() {
        return temp1;
    }

    public void setTemp1(double temp1) {
        this.temp1 = temp1;
    }

    public double getAreaACHT0() {
        return areaACHT0;
    }

    public void setAreaACHT0(double areaACHT0) {
        this.areaACHT0 = areaACHT0;
    }

    public String getItogColor() {
        return itogColor;
    }

    public void setItogColor(String itogColor) {
        this.itogColor = itogColor;
    }

    public double getAreaACHT1() {
        return areaACHT1;
    }

    public void setAreaACHT1(double areaACHT1) {
        this.areaACHT1 = areaACHT1;
    }

    public double getAreaFPU0() {
        return areaFPU0;
    }

    public void setAreaFPU0(double areaFPU0) {
        this.areaFPU0 = areaFPU0;
    }

    public double getAreaFPU1() {
        return areaFPU1;
    }

    public void setAreaFPU1(double areaFPU1) {
        this.areaFPU1 = areaFPU1;
    }

    public double getRasstACHTfpu0() {
        return rasstACHTfpu0;
    }

    public void setRasstACHTfpu0(double rasstACHTfpu0) {
        this.rasstACHTfpu0 = rasstACHTfpu0;
    }

    public double getRasstACHTfpu1() {
        return rasstACHTfpu1;
    }

    public void setRasstACHTfpu1(double rasstACHTfpu1) {
        this.rasstACHTfpu1 = rasstACHTfpu1;
    }

    public void setPotok0(double potok0) {
        this.potok0 = potok0;
    }

    public void setPotok1(double potok1) {
        this.potok1 = potok1;
    }

    public double getPotok() {
        return potok;
    }

    public void setPotok(double potok) {
        this.potok = potok;
    }

    public double getEpsilin0() {
        return epsilin0;
    }

    public void setEpsilin0(double epsilin0) {
        this.epsilin0 = epsilin0;
    }

    public double getEpsilin1() {
        return epsilin1;
    }

    public void setEpsilin1(double epsilin1) {
        this.epsilin1 = epsilin1;
    }

    public double getPlank0() {
        return plank0;
    }

    public void setPlank0(double plank0) {
        this.plank0 = plank0;
    }

    public double getPlank1() {
        return plank1;
    }

    public void setPlank1(double plank1) {
        this.plank1 = plank1;
    }

    public double getBetta0() {
        return betta0;
    }

    public void setBetta0(double betta0) {
        this.betta0 = betta0;
    }

    public double getBetta1() {
        return betta1;
    }

    public void setBetta1(double betta1) {
        this.betta1 = betta1;
    }

    public double getfEfect() {
        return fEfect;
    }

    public void setfEfect(double fEfect) {
        this.fEfect = fEfect;
    }

    public void setTempPower(boolean tempPower) {
        this.tempPower = tempPower;
    }

    public int getTempInt() {
        return tempInt;
    }

    public void setTempInt(int tempInt) {
        this.tempInt = tempInt;
    }

    public int getTempREF() {
        return tempREF;
    }

    public void setTempREF(int tempREF) {
        this.tempREF = tempREF;
    }

    public int getTempVOS() {
        return tempVOS;
    }

    public void setTempVOS(int tempVOS) {
        this.tempVOS = tempVOS;
    }

    public int getTempVR0() {
        return tempVR0;
    }

    public void setTempVR0(int tempVR0) {
        this.tempVR0 = tempVR0;
    }

    public boolean isTempKU() {
        return tempKU;
    }

    public void setTempKU(boolean tempKU) {
        this.tempKU = tempKU;
    }

    public double getExposure() {
        return exposure;
    }

    public void setExposure(double exposure) {
        this.exposure = exposure;
    }

    public double getArifmeticMeanPersent() {
        return arifmeticMeanPersent;
    }

    public void setArifmeticMeanPersent(double arifmeticMeanPersent) {
        this.arifmeticMeanPersent = arifmeticMeanPersent;
    }

    public double getQuadraticMeanPersent() {
        return quadraticMeanPersent;
    }

    public void setQuadraticMeanPersent(double quadraticMeanPersent) {
        this.quadraticMeanPersent = quadraticMeanPersent;
    }

    public double getSKOPersent() {
        return SKOPersent;
    }

    public void setSKOPersent(double SKOPersent) {
        this.SKOPersent = SKOPersent;
    }

    public double getVwPersent() {
        return vwPersent;
    }

    public void setVwPersent(double vwPersent) {
        this.vwPersent = vwPersent;
    }

    public double getPorogPersent() {
        return porogPersent;
    }

    public void setPorogPersent(double porogPersent) {
        this.porogPersent = porogPersent;
    }

    public double getPorogStarPersent() {
        return porogStarPersent;
    }

    public void setPorogStarPersent(double porogStarPersent) {
        this.porogStarPersent = porogStarPersent;
    }

    public double getDetectivityPersent() {
        return detectivityPersent;
    }

    public void setDetectivityPersent(double detectivityPersent) {
        this.detectivityPersent = detectivityPersent;
    }

    public double getDetectivityStarPersent() {
        return detectivityStarPersent;
    }

    public void setDetectivityStarPersent(double detectivityStarPersent) {
        this.detectivityStarPersent = detectivityStarPersent;
    }

    public double getNETDPersent() {
        return NETDPersent;
    }

    public void setNETDPersent(double NETDPersent) {
        this.NETDPersent = NETDPersent;
    }

    public double getExposurePersent() {
        return exposurePersent;
    }

    public void setExposurePersent(double exposurePersent) {
        this.exposurePersent = exposurePersent;
    }

    public String getPersentColorArifm() {
        return persentColorArifm;
    }

    public void setPersentColorArifm(String persentColorArifm) {
        this.persentColorArifm = persentColorArifm;
    }

    public String getPersentColorQuadratic() {
        return persentColorQuadratic;
    }

    public void setPersentColorQuadratic(String persentColorQuadratic) {
        this.persentColorQuadratic = persentColorQuadratic;
    }

    public String getPersentColorSKO() {
        return persentColorSKO;
    }

    public void setPersentColorSKO(String persentColorSKO) {
        this.persentColorSKO = persentColorSKO;
    }

    public String getPersentColorVw() {
        return persentColorVw;
    }

    public void setPersentColorVw(String persentColorVw) {
        this.persentColorVw = persentColorVw;
    }

    public String getPersentColorPorog() {
        return persentColorPorog;
    }

    public void setPersentColorPorog(String persentColorPorog) {
        this.persentColorPorog = persentColorPorog;
    }

    public String getPersentColorPorogStar() {
        return persentColorPorogStar;
    }

    public void setPersentColorPorogStar(String persentColorPorogStar) {
        this.persentColorPorogStar = persentColorPorogStar;
    }

    public String getPersentColorDetectivity() {
        return persentColorDetectivity;
    }

    public void setPersentColorDetectivity(String persentColorDetectivity) {
        this.persentColorDetectivity = persentColorDetectivity;
    }

    public String getPersentColorDetectivityStar() {
        return persentColorDetectivityStar;
    }

    public void setPersentColorDetectivityStar(String persentColorDetectivityStar) {
        this.persentColorDetectivityStar = persentColorDetectivityStar;
    }

    public String getPersentColorNETD() {
        return persentColorNETD;
    }

    public void setPersentColorNETD(String persentColorNETD) {
        this.persentColorNETD = persentColorNETD;
    }

    public String getPersentColorExposure() {
        return persentColorExposure;
    }

    public void setPersentColorExposure(String persentColorExposure) {
        this.persentColorExposure = persentColorExposure;
    }

    public String getDimention() {
        return dimention;
    }

    public void setDimention(String dimention) {
        this.dimention = dimention;
    }

    public String getZakaz() {
        return zakaz.get();
    }

    public StringProperty zakazProperty() {
        return zakaz;
    }

    public String getDogovor() {
        return dogovor.get();
    }

    public StringProperty dogovorProperty() {
        return dogovor;
    }

    public String getMetodika() {
        return metodika.get();
    }

    public StringProperty metodikaProperty() {
        return metodika;
    }

    public String getNomer_0() {
        return nomer_0.get();
    }

    public StringProperty nomer_0Property() {
        return nomer_0;
    }

    public String getNomer() {
        return nomer.get();
    }

    public StringProperty nomerProperty() {
        return nomer;
    }

    public String getCopy() {
        return copy.get();
    }

    public StringProperty copyProperty() {
        return copy;
    }

    public String getOtk() {
        return otk.get();
    }

    public StringProperty otkProperty() {
        return otk;
    }

    public String getData() {
        return data.get();
    }

    public StringProperty dataProperty() {
        return data;
    }

    public void setData(String data) {
        this.data.set(data);
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public String getTXT_0_0() {
        return TXT_0_0.get();
    }

    public StringProperty TXT_0_0Property() {
        return TXT_0_0;
    }

    public String getTXT_0_1() {
        return TXT_0_1.get();
    }

    public StringProperty TXT_0_1Property() {
        return TXT_0_1;
    }

    public String getTXT_0_2() {
        return TXT_0_2.get();
    }

    public StringProperty TXT_0_2Property() {
        return TXT_0_2;
    }

    public String getTXT_0_3() {
        return TXT_0_3.get();
    }

    public StringProperty TXT_0_3Property() {
        return TXT_0_3;
    }

    public String getTXT_0_4() {
        return TXT_0_4.get();
    }

    public StringProperty TXT_0_4Property() {
        return TXT_0_4;
    }

    public String getTXT_0_5() {
        return TXT_0_5.get();
    }

    public StringProperty TXT_0_5Property() {
        return TXT_0_5;
    }

    public String getTXT_0_6() {
        return TXT_0_6.get();
    }

    public StringProperty TXT_0_6Property() {
        return TXT_0_6;
    }

    public String getTXT_0_7() {
        return TXT_0_7.get();
    }

    public StringProperty TXT_0_7Property() {
        return TXT_0_7;
    }

    public String getTXT_0_8() {
        return TXT_0_8.get();
    }

    public StringProperty TXT_0_8Property() {
        return TXT_0_8;
    }

    public String getTXT_0_9() {
        return TXT_0_9.get();
    }

    public StringProperty TXT_0_9Property() {
        return TXT_0_9;
    }

    public boolean isTbExel() {
        return tbExel.get();
    }

    public BooleanProperty tbExelProperty() {
        return tbExel;
    }

    public boolean isTbTxt() {
        return tbTxt.get();
    }

    public BooleanProperty tbTxtProperty() {
        return tbTxt;
    }

    public BooleanProperty tbPdfProperty() {
        return tbPdf;
    }

    public BooleanProperty cbWithBPProperty() {
        return cbWithBP;
    }

    public BooleanProperty cbPrintProperty() {
        return cbPrint;
    }

    public int getDetPortVideo() {
        return detPortVideo;
    }

    public void setDetPortVideo(int detPortVideo) {
        this.detPortVideo = detPortVideo;
    }

    public int getDetPortCommand() {
        return detPortCommand;
    }

    public void setDetPortCommand(int detPortCommand) {
        this.detPortCommand = detPortCommand;
    }

    public String getDetIP() {
        return detIP;
    }

    public void setDetIP(String detIP) {
        this.detIP = detIP;
    }

    public int getServerVideoBuff() {
        return serverVideoBuff;
    }

    public int getCommandBuff() {
        return commandBuff;
    }

    public NetworkInfo getSelNetworkInterface() {
        return selNetworkInterface;
    }

    public void setSelNetworkInterface(NetworkInfo selNetworkInterface) {
        this.selNetworkInterface = selNetworkInterface;
    }

    public void setPAUSE(int PAUSE) {
        this.PAUSE = PAUSE;
    }

    public int getPAUSE() {

        return this.PAUSE;
    }
}