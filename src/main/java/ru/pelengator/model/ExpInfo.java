package ru.pelengator.model;

import ru.pelengator.API.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Класс для списка экспериментов.
 */
public class ExpInfo {
    /**
     * Наименование эксперимента.
     */
    private String expName;
    /**
     * Индекс эксперимента в списке.
     */
    private int expIndex;
    /**
     * Список кадров при первом потоке.
     */
    private ArrayList<int[][]> dataArray0;
    /**
     * Список кадров при втором потоке.
     */
    private ArrayList<int[][]> dataArray1;


    private int bpInCenter = -1;
    private int bpAll = -1;
    /**
     * Параметры стенда.
     */
    private StendParams params;
    /**
     * Пустая картинка
     */
    private BufferedImage tempImage;

    private double arifmeticMean;
    private double quadraticMean;
    private double SKO;
    private double vw;
    private double porog;
    private double porogStar;
    private double detectivity;
    private double detectivityStar;
    private double NETD;
    private double exposure;
    private int sizeY;
    private int sizeX;
    private boolean printBpList = false;
    private byte[] buffToTXT;
    private boolean withDefPx = true;
    /////////////////
    /**
     * Лист кадров с дефектами
     */
    private ArrayList<Utils.Frame> frList;
    /**
     * Лист картинок для отчета
     */
    private ArrayList<BufferedImage> scList;

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public void setExpIndex(int expIndex) {
        this.expIndex = expIndex;
    }

    public ArrayList<int[][]> getDataArray0() {
        return dataArray0;
    }

    public void setDataArray0(ArrayList<int[][]> dataArray0) {
        this.dataArray0 = dataArray0;
    }

    public ArrayList<int[][]> getDataArray1() {
        return dataArray1;
    }

    public void setDataArray1(ArrayList<int[][]> dataArray1) {
        this.dataArray1 = dataArray1;
    }

    public byte[] getBuffToTXT() {
        return buffToTXT;
    }

    public void setBuffToTXT(byte[] buffToTXT) {
        this.buffToTXT = buffToTXT;
    }

    public StendParams getParams() {
        return params;
    }

    public void setParams(StendParams params) {
        this.params = params;
    }

    public double getArifmeticMean() {
        return arifmeticMean;
    }

    public void setArifmeticMean(double arifmeticMean) {
        this.arifmeticMean = arifmeticMean;
    }

    public double getQuadraticMean() {
        return quadraticMean;
    }

    public void setQuadraticMean(double quadraticMean) {
        this.quadraticMean = quadraticMean;
    }

    public double getSKO() {
        return SKO;
    }

    public void setSKO(double SKO) {
        this.SKO = SKO;
    }

    public double getVw() {
        return vw;
    }

    public void setVw(double vw) {
        this.vw = vw;
    }

    public double getPorog() {
        return porog;
    }

    public void setPorog(double porog) {
        this.porog = porog;
    }

    public double getPorogStar() {
        return porogStar;
    }

    public void setPorogStar(double porogStar) {
        this.porogStar = porogStar;
    }

    public double getDetectivity() {
        return detectivity;
    }

    public void setDetectivity(double detectivity) {
        this.detectivity = detectivity;
    }

    public double getDetectivityStar() {
        return detectivityStar;
    }

    public void setDetectivityStar(double detectivityStar) {
        this.detectivityStar = detectivityStar;
    }

    public double getNETD() {
        return NETD;
    }

    public void setNETD(double NETD) {
        this.NETD = NETD;
    }

    public double getExposure() {
        return exposure;
    }

    public void setExposure(double exposure) {
        this.exposure = exposure;
    }

    public ArrayList<Utils.Frame> getFrList() {
        return frList;
    }

    public void setFrList(ArrayList<Utils.Frame> frList) {
        this.frList = frList;
    }

    public BufferedImage getTempImage() {
        return tempImage;
    }

    public void setTempImage(BufferedImage tempImage) {
        this.tempImage = tempImage;
    }

    public ArrayList<BufferedImage> getScList() {
        return scList;
    }

    public void setScList(ArrayList<BufferedImage> scList) {
        this.scList = scList;
    }

    @Override
    public String toString() {
        return expName;
    }

    public boolean isPrintBpList() {
        return printBpList;
    }

    public void setPrintBpList(boolean printBpList) {
        this.printBpList = printBpList;
    }

    public int getBpInCenter() {
        return bpInCenter;
    }

    public void setBpInCenter(int bpInCenter) {
        this.bpInCenter = bpInCenter;
    }

    public int getBpAll() {
        return bpAll;
    }

    public void setBpAll(int bpAll) {
        this.bpAll = bpAll;
    }

    public boolean isWithDefPx() {
        return withDefPx;
    }

    public void setWithDefPx(boolean withDefPx) {
        this.withDefPx = withDefPx;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }
}