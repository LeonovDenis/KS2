package ru.pelengator.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.utils.StatisticsUtils;
import ru.pelengator.API.utils.Utils;
import ru.pelengator.ParamsController;
import ru.pelengator.model.ExpInfo;
import ru.pelengator.model.StendParams;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.pelengator.API.utils.Utils.*;

/**
 * Сервис расчета параметров
 */
public class ParamsService extends Service<Void> {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ParamsService.class);

    //Массивы исходных данных
    private double[][] arifmeticMeanValue_0;
    private double[][] arifmeticMeanValue_1;
    private double[][] quadraticMeanValue_0;
    private double[][] quadraticMeanValue_1;
    private double[][] SKOValue;
    private double[][] arifmeticMeanValue_delta;
    private double[][] quadraticMeanValue_delta;
    //Массивы расчитанные
    double[][] voltWatka;
    double[][] porogSensivity;
    double[][] porogSensivityStar;
    double[][] dDetectivity;
    double[][] dDetectivityStar;
    double[][] netd;
    double[][] eExposure;

    //переменные
    private int sizeY;
    private int sizeX;
    private StatisticsUtils[][] dataArrayStat_0;
    private StatisticsUtils[][] dataArrayStat_1;
    //флаг расчета с/ без учета деф. пикселей
    private boolean noCorrection;
    private StendParams params;
    //итоговые средние значения по ФПУ
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
    private Double vWHeterogeneity;

    private byte[] buffToTXT;
    private int bpInCenter;
    private int bpAll;

    private boolean withDefPx;
    private List<BadBigPoint> badBigPoints;
    /**
     * Лист с фреймами
     */
    private ArrayList<Utils.Frame> frList;
    /**
     * Лист с квадратами для печати
     */
    private ArrayList<BufferedImage> scList;
    /**
     * Ссылка на контроллер
     */
    private ParamsController controller;
    /**
     * Контейер для гистограмм и квадратов
     */
    private VBox pane;
    /**
     * Контейер для итога
     */
    private VBox pane1;

    /**
     * Заготовка для отрисовки квадрата
     */
    private BufferedImage tempImage;


    public ParamsService(ParamsController controller) {
        this.controller = controller;
    }

    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> controller.getpIndicator2().setVisible(true));

                updateMessage("Старт");
                updateProgress(0.0, 1);
                updateMessage("Инициализация данных");
                updateProgress(0.1, 1);
                initParams();
                updateProgress(0.15, 1);
                updateMessage("Расчет исходных данных");

                calculateMeanValuesAndSKO();
                updateProgress(0.2, 1);


                updateMessage("Расчет срендего арифм.");
                arifmeticMean = calculateAndCheckArifmSignal(
                        controller.getCbArifmeticMean().isSelected(),
                        noCorrection);
                updateProgress(0.25, 1);
                updateMessage("Расчет среднего квадрат.");
                quadraticMean = calculateAndCheckQuadraticSignal(
                        controller.getCbQuadraticMean().isSelected(),
                        noCorrection);
                updateProgress(0.3, 1);
                updateMessage("Расчет шума");
                SKO = calculateAndCheckSKO(controller.getCbSKO().isSelected(),
                        noCorrection);
                updateProgress(0.35, 1);
                updateMessage("Расчет вольтовой чувств.");
                vw = calculateAndCheckVw(controller.getCbVw().isSelected(),
                        noCorrection);
                updateProgress(0.45, 1);
                updateMessage("Расчет порога чувств.");
                porog = calculateAndCheckPorog(controller.getCbPorog().isSelected(),
                        noCorrection);
                updateProgress(0.5, 1);
                updateMessage("Расчет удельного порога чувств.");
                porogStar = calculateAndCheckPorogStar(controller.getCbPorogStar().isSelected(),
                        noCorrection);
                updateProgress(0.55, 1);
                updateMessage("Расчет обнаружительной способности");
                detectivity = calculateAndCheckDetectivity(controller.getCbDetectivity().isSelected(),
                        noCorrection);
                updateProgress(0.65, 1);
                updateMessage("Расчет удельной обнаруж. способности");
                detectivityStar = calculateAndCheckDetectivityStar(controller.getCbDetectivityStar().isSelected(),
                        noCorrection);
                updateProgress(0.7, 1);
                updateMessage("Расчет NETD");
                NETD = calculateAndCheckNETD(controller.getCbNETD().isSelected(),
                        noCorrection);
                updateProgress(0.75, 1);
                updateMessage("Расчет пороговой облученности");
                exposure = calculateAndCheckExposure(controller.getCbExposure().isSelected(),
                        noCorrection);
                updateProgress(0.8, 1);


                updateMessage("Расчет итоговой дефектности");
                calculateAllDefects();
                updateMessage("Запись параметров");
                saveExpData();
                updateProgress(0.85, 1);
                updateMessage("Отрисовка результатов");
                showResults();
                updateProgress(1, 1);
                updateMessage("");

                return null;
            }

            private void calculateAllDefects() {
                badBigPoints = bedPxToList(params.getItogColor());
                bpAll = badBigPoints.size();
                bpInCenter = bbpInCentral(badBigPoints, 32, sizeX, sizeY);
            }
        };
    }

    /**
     * Расчет облученности и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckExposure(boolean selected, boolean noCorrection) {
        double persent = params.getExposurePersent();
        String color = params.getPersentColorExposure();
        DEF_TYPE type = DEF_TYPE.TYPE_EXPOSURE;

        eExposure = exposure(quadraticMeanValue_1, quadraticMeanValue_0, SKOValue,
                params.getExposure());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(9).getBpList(), eExposure, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет NETD и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckNETD(boolean selected, boolean noCorrection) {
        double persent = params.getNETDPersent();
        String color = params.getPersentColorNETD();
        DEF_TYPE type = DEF_TYPE.TYPE_NETD;

        netd = NETD(arifmeticMeanValue_1, arifmeticMeanValue_0, SKOValue,
                params.getTemp1(), params.getTemp0());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(8).getBpList(), netd, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет удельной обнаруж. способн. и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckDetectivityStar(boolean selected, boolean noCorrection) {
        double persent = params.getDetectivityStarPersent();
        String color = params.getPersentColorDetectivityStar();
        DEF_TYPE type = DEF_TYPE.TYPE_DETECTIVITY_STAR;

        dDetectivityStar = detectivityStar(quadraticMeanValue_1, quadraticMeanValue_0, SKOValue,
                params.getPotok(), params.getfEfect(), params.getAreaFPU0());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(7).getBpList(), dDetectivityStar, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет обнаруж. способн и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckDetectivity(boolean selected, boolean noCorrection) {
        double persent = params.getDetectivityPersent();
        String color = params.getPersentColorDetectivity();
        DEF_TYPE type = DEF_TYPE.TYPE_DETECTIVITY;

        dDetectivity = detectivity(quadraticMeanValue_1, quadraticMeanValue_0, SKOValue,
                params.getPotok(), params.getfEfect());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(6).getBpList(), dDetectivity, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет удельного порога и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckPorogStar(boolean selected, boolean noCorrection) {
        double persent = params.getPorogStarPersent();
        String color = params.getPersentColorPorogStar();
        DEF_TYPE type = DEF_TYPE.TYPE_POROG_STAR;

        porogSensivityStar = porogSensivityStar(quadraticMeanValue_1, quadraticMeanValue_0, SKOValue,
                params.getPotok(), params.getfEfect(), params.getAreaFPU0());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(5).getBpList(), porogSensivityStar, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет порога и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckPorog(boolean selected, boolean noCorrection) {
        double persent = params.getPorogPersent();
        String color = params.getPersentColorPorog();
        DEF_TYPE type = DEF_TYPE.TYPE_POROG;

        porogSensivity = porogSensivity(quadraticMeanValue_1, quadraticMeanValue_0, SKOValue,
                params.getPotok(), params.getfEfect());

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(4).getBpList(), porogSensivity, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет вольтовой чувствительности и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckVw(boolean selected, boolean noCorrection) {
        double persent = params.getVwPersent();
        String color = params.getPersentColorVw();
        DEF_TYPE type = DEF_TYPE.TYPE_VW;

        voltWatka = voltWatka(quadraticMeanValue_1, quadraticMeanValue_0, params.getPotok());

        vWHeterogeneity = calculateHeterogeneity(voltWatka, noCorrection, persent);

        if (!selected) {
            return -1;
        }

        return addBpToList(frList.get(3).getBpList(), voltWatka, noCorrection,
                persent, color, type);
    }


    /**
     * Расчет СКО и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckSKO(boolean selected, boolean noCorrection) {
        double persent = params.getSKOPersent();
        String color = params.getPersentColorSKO();
        DEF_TYPE type = DEF_TYPE.TYPE_SKO;

        if (!selected) {
            return addBpToList(null, SKOValue, noCorrection,
                    persent, color, type);
        }

        return addBpToList(frList.get(2).getBpList(), SKOValue, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет среднего квадрат. значения и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckQuadraticSignal(boolean selected, boolean noCorrection) {
        double persent = params.getQuadraticMeanPersent();
        String color = params.getPersentColorQuadratic();
        DEF_TYPE type = DEF_TYPE.TYPE_QUADRATIC;

        if (!selected) {
            return addBpToList(null, quadraticMeanValue_delta, noCorrection,
                    persent, color, type);
        }

        return addBpToList(frList.get(1).getBpList(), quadraticMeanValue_delta, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет среднего арифм. значения и проверка брака
     *
     * @param selected     считаем ли
     * @param noCorrection с учетом бракованных пикселей или нет
     * @return среднее значение по ФПУ
     */
    private double calculateAndCheckArifmSignal(boolean selected,
                                                boolean noCorrection) {
        double persent = params.getArifmeticMeanPersent();
        String color = params.getPersentColorArifm();
        DEF_TYPE type = DEF_TYPE.TYPE_ARIFMETIC;

        if (!selected) {
            return addBpToList(null, arifmeticMeanValue_delta, noCorrection,
                    persent, color, type);
        }

        return addBpToList(frList.get(0).getBpList(), arifmeticMeanValue_delta, noCorrection,
                persent, color, type);
    }

    /**
     * Расчет среднего значения и ско
     */
    private void calculateMeanValuesAndSKO() {

        takeStat();

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                arifmeticMeanValue_0[h][w] = dataArrayStat_0[h][w].getMean() * MASHTAB / 1000.0;
                quadraticMeanValue_0[h][w] = dataArrayStat_0[h][w].getQvadraricMean() * MASHTAB / 1000.0;
                SKOValue[h][w] = dataArrayStat_0[h][w].getStdDev() * MASHTAB / 1000;

                arifmeticMeanValue_1[h][w] = dataArrayStat_1[h][w].getMean() * MASHTAB / 1000.0;
                quadraticMeanValue_1[h][w] = dataArrayStat_1[h][w].getQvadraricMean() * MASHTAB / 1000.0;
            }
        }
        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                arifmeticMeanValue_delta[h][w] = arifmeticMeanValue_1[h][w] - arifmeticMeanValue_0[h][w];
                quadraticMeanValue_delta[h][w] = quadraticMeanValue_1[h][w] - quadraticMeanValue_0[h][w];
            }
        }
    }

    /**
     * Инициализация параметров
     */
    private void initParams() {
        int[][] dta = controller.getController().getSelExp().getDataArray0().get(0);
        sizeY = dta.length;
        sizeX = dta[0].length;
        noCorrection = controller.getCbWithBP().isSelected();

        arifmeticMeanValue_0 = new double[sizeY][sizeX];
        arifmeticMeanValue_1 = new double[sizeY][sizeX];
        quadraticMeanValue_0 = new double[sizeY][sizeX];
        quadraticMeanValue_1 = new double[sizeY][sizeX];
        SKOValue = new double[sizeY][sizeX];
        arifmeticMeanValue_delta = new double[sizeY][sizeX];
        quadraticMeanValue_delta = new double[sizeY][sizeX];

        voltWatka = new double[sizeY][sizeX];
        porogSensivity = new double[sizeY][sizeX];
        porogSensivityStar = new double[sizeY][sizeX];
        dDetectivity = new double[sizeY][sizeX];
        dDetectivityStar = new double[sizeY][sizeX];
        netd = new double[sizeY][sizeX];
        eExposure = new double[sizeY][sizeX];
        params = controller.getController().getSelExp().getParams();


        dataArrayStat_0 = new StatisticsUtils[sizeY][sizeX];
        dataArrayStat_1 = new StatisticsUtils[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                dataArrayStat_0[h][w] = new StatisticsUtils();
            }
        }
        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                dataArrayStat_1[h][w] = new StatisticsUtils();
            }
        }

        arifmeticMean = 0;
        quadraticMean = 0;
        SKO = 0;
        vw = 0;
        porog = 0;
        porogStar = 0;
        detectivity = 0;
        detectivityStar = 0;
        NETD = 0;
        exposure = 0;
        frList = new ArrayList<>();
        scList = new ArrayList<>();
        badBigPoints = new ArrayList<>();

        frList.add(new Utils.Frame("Среднее арифметическое сигнала, В",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Среднее квадратичное сигнала, В",
                sizeX, sizeY));
        frList.add(new Utils.Frame("СКО сигнала (шум), В",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Вольтовая чувствительность, В\u00B7Вт\u00AF \u00B9",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Порог чувствительности, Вт\u00B7Гц-\u00BD",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Удельный порог чувствительности, Вт\u00B7Гц-\u00BD\u00B7см\u00AF \u00B9",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Обнаружительная способность, Вт\u00AF \u00B9\u00B7Гц\u00BD",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Удельная обнаруж. способность, Вт\u00AF \u00B9\u00B7Гц\u00BD\u00B7см",
                sizeX, sizeY));
        frList.add(new Utils.Frame("NETD, К",
                sizeX, sizeY));
        frList.add(new Utils.Frame("Пороговая облученность, Вт\u00B7см\u00AF \u00B2",
                sizeX, sizeY));
        vWHeterogeneity = null;
        pane = controller.getScrlPane();
        pane1 = controller.getScrlPane1();
        tempImage = controller.getController().getSelExp().getTempImage();
        bpInCenter = -1;
        bpAll = -1;
        withDefPx = controller.getCbWithBP().isSelected();
        buffToTXT = null;
        Platform.runLater(() -> {
            clearPane(pane);
            clearPane(pane1);
        });
    }

    /**
     * Создание узла с текстром ошибки
     *
     * @param exception
     * @return
     */
    private Node setErrorMSG(Throwable exception) {
        Pane pane = new Pane();
        pane.getChildren().add(new TextArea(exception.getMessage()));
        return pane;
    }

    /**
     * Набираем массив статистики
     */
    private void takeStat() {
        ArrayList<int[][]> dataArray0 = controller.getController().getSelExp().getDataArray0();
        ArrayList<int[][]> dataArray1 = controller.getController().getSelExp().getDataArray1();
        int count = dataArray0.size();
        for (int j = 0; j < count; j++) {
            int[][] frame0 = dataArray0.get(j);
            int[][] frame1 = dataArray1.get(j);
            for (int h = 0; h < sizeY; h++) {
                for (int w = 0; w < sizeX; w++) {
                    dataArrayStat_0[h][w].addValue(frame0[h][w]);
                    dataArrayStat_1[h][w].addValue(frame1[h][w]);
                }
            }
        }
    }

    /**
     * Отображение результатов
     */
    private void showResults() {

        Platform.runLater(() -> {
            fillTextFields();
            fillTextLabels();
            showGistAndImage(pane);
            if (controller.getCbPrint().isSelected()) {
                fillItogpane(pane1);
            }
        });
    }

    /**
     * Отрисовка гистограммы и малого квадрата
     *
     * @param pane
     */
    private void showGistAndImage(VBox pane) {


        if (controller.getCbArifmeticMean().isSelected()) {

            RaspredData raspred = makeRaspred(arifmeticMeanValue_delta, "0.000E00", noCorrection, params.getArifmeticMeanPersent());

            showGistAndImageBox(pane, "Среднее арифметическое сигнала, В",
                    "Число диодов", raspred, tempImage, frList.get(0).getBpList(), scList, controller);
        }

        if (controller.getCbQuadraticMean().isSelected()) {

            RaspredData raspred = makeRaspred(quadraticMeanValue_delta, "0.000E00", noCorrection, params.getQuadraticMeanPersent());

            showGistAndImageBox(pane, "Среднее квадратичное сигнала, В",
                    "Число диодов", raspred, tempImage, frList.get(1).getBpList(), scList, controller);
        }
        if (controller.getCbSKO().isSelected()) {

            RaspredData raspred = makeRaspred(SKOValue, "0.000E00", noCorrection, params.getSKOPersent());

            showGistAndImageBox(pane, "СКО сигнала (шум), В",
                    "Число диодов", raspred, tempImage, frList.get(2).getBpList(), scList, controller);
        }
        if (controller.getCbVw().isSelected()) {

            RaspredData raspred = makeRaspred(voltWatka, "0.000E00", noCorrection, params.getVwPersent());

            showGistAndImageBox(pane, "Вольтовая чувствительность, В\u00B7Вт\u00AF \u00B9",
                    "Число диодов", raspred, tempImage, frList.get(3).getBpList(), scList, controller);
        }
        if (controller.getCbPorog().isSelected()) {

            RaspredData raspred = makeRaspred(porogSensivity, "0.000E00", noCorrection, params.getPorogPersent());

            showGistAndImageBox(pane, "Порог чувствительности, Вт\u00B7Гц-\u00BD",
                    "Число диодов", raspred, tempImage, frList.get(4).getBpList(), scList, controller);
        }
        if (controller.getCbPorogStar().isSelected()) {

            RaspredData raspred = makeRaspred(porogSensivityStar, "0.000E00", noCorrection, params.getPorogStarPersent());

            showGistAndImageBox(pane, "Удельный порог чувствительности, Вт\u00B7Гц-\u00BD\u00B7см\u00AF \u00B9",
                    "Число диодов", raspred, tempImage, frList.get(5).getBpList(), scList, controller);
        }
        if (controller.getCbDetectivity().isSelected()) {

            RaspredData raspred = makeRaspred(dDetectivity, "0.000E00", noCorrection, params.getDetectivityPersent());

            showGistAndImageBox(pane, "Обнаружительная способность, Вт\u00AF \u00B9\u00B7Гц\u00BD",
                    "Число диодов", raspred, tempImage, frList.get(6).getBpList(), scList, controller);
        }
        if (controller.getCbDetectivityStar().isSelected()) {

            RaspredData raspred = makeRaspred(dDetectivityStar, "0.000E00", noCorrection, params.getDetectivityStarPersent());

            showGistAndImageBox(pane, "Удельная обнаруж. способность, Вт\u00AF \u00B9\u00B7Гц\u00BD\u00B7см",
                    "Число диодов", raspred, tempImage, frList.get(7).getBpList(), scList, controller);
        }
        if (controller.getCbNETD().isSelected()) {

            RaspredData raspred = makeRaspred(netd, "0.000E00", noCorrection, params.getNETDPersent());

            showGistAndImageBox(pane, "NETD, К",
                    "Число диодов", raspred, tempImage, frList.get(8).getBpList(), scList, controller);
        }
        if (controller.getCbExposure().isSelected()) {

            RaspredData raspred = makeRaspred(eExposure, "0.000E00", noCorrection, params.getExposurePersent());

            showGistAndImageBox(pane, "Пороговая облученность, Вт\u00B7см\u00AF \u00B2",
                    "Число диодов", raspred, tempImage, frList.get(9).getBpList(), scList, controller);
        }
    }

    /**
     * Заполнение полей брака
     * @param pane панель
     */
    private void fillItogpane(VBox pane) {
        Platform.runLater(() -> {
            showImageBox(pane, tempImage, badBigPoints, scList, controller);
            controller.getLbItog().setText(String.valueOf(bpAll));
            controller.getLbItog1().setText(String.valueOf(bpInCenter));
        });

    }

    /**
     * Переделать //todo супер переделка
     * @param color цвет пикселя.
     * @return
     */
    private List<BadBigPoint> bedPxToList(String color) {


        String lineseparator = System.getProperty("line.separator");

        List<BadBigPoint> list = null;

        //todo  30 секунд
        long k=0;

        for (Frame fr : frList) {
            if (!fr.getBpList().isEmpty()) {
                if (list == null) {
                    list = new ArrayList<>();
                }
                for (BadPoint bp :
                        fr.getBpList()) {
                    BadBigPoint badbBigPoint = new BadBigPoint(bp, convertcolor(color));
                    int indexOf = list.indexOf(badbBigPoint);
                    if (indexOf < 0) {
                        list.add(badbBigPoint);
                    } else {
                        list.get(indexOf).addToList(bp);
                    }
                }
            }
        }

        buffToTXT = extructTextLine(lineseparator, list);

        return list;
    }

    /**
     * Создание строки дефектных элементов для печати.
     * @param lineseparator
     * @param list
     * @return
     */
    private byte[] extructTextLine(String lineseparator, List<BadBigPoint> list) {
        StringBuilder tempStr = new StringBuilder();
        if (list != null) {
            int count=0;
            for (BadBigPoint bp : list) {
                tempStr.append(++count).append(".");
                tempStr.append(bp).append(lineseparator);
            }
        } else {
            tempStr.append("Нет дефектных элементов!");
        }

        return tempStr.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Заполнение полей брака.
     */
    private void fillTextLabels() {
        ///////////////////  full

        controller.getLbArifmeticMean().setText(frList.get(0).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(0).getBpList().size()));
        controller.getLbQuadraticMean().setText(frList.get(1).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(1).getBpList().size()));
        controller.getLbSKO().setText(frList.get(2).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(2).getBpList().size()));
        controller.getLbVw().setText(frList.get(3).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(3).getBpList().size()));
        controller.getLbPorog().setText(frList.get(4).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(4).getBpList().size()));
        controller.getLbPorogStar().setText(frList.get(5).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(5).getBpList().size()));
        controller.getLbDetectivity().setText(frList.get(6).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(6).getBpList().size()));
        controller.getLbDetectivityStar().setText(frList.get(7).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(7).getBpList().size()));
        controller.getLbNETD().setText(frList.get(8).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(8).getBpList().size()));
        controller.getLbExposure().setText(frList.get(9).getBpList().size() == 0 ? "--" : String.valueOf(frList.get(9).getBpList().size()));

        ////////////////////  32*32

        controller.getLbArifmeticMean1().setText(bpInCentral(frList, 0, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 0, 32)));
        controller.getLbQuadraticMean1().setText(bpInCentral(frList, 1, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 1, 32)));
        controller.getLbSKO1().setText(bpInCentral(frList, 2, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 2, 32)));
        controller.getLbVw1().setText(bpInCentral(frList, 3, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 3, 32)));
        controller.getLbPorog1().setText(bpInCentral(frList, 4, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 4, 32)));
        controller.getLbPorogStar1().setText(bpInCentral(frList, 5, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 5, 32)));
        controller.getLbDetectivity1().setText(bpInCentral(frList, 6, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 6, 32)));
        controller.getLbDetectivityStar1().setText(bpInCentral(frList, 7, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 7, 32)));
        controller.getLbNETD1().setText(bpInCentral(frList, 8, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 8, 32)));
        controller.getLbExposure1().setText(bpInCentral(frList, 9, 32) == 0 ? "--" : String.valueOf(bpInCentral(frList, 9, 32)));

    }

    /**
     * Заполнение текстовых полей.
     */
    private void fillTextFields() {

        controller.getTfArifmeticMean().setText(
                String.format(Locale.CANADA, "%.3e", arifmeticMean).toUpperCase());
        controller.getTfQuadraticMean().setText(
                String.format(Locale.CANADA, "%.3e", quadraticMean).toUpperCase());
        controller.getTfSKO().setText(
                String.format(Locale.CANADA, "%.3e", SKO).toUpperCase());

        if (controller.getCbVw().isSelected()) {

            controller.getTfVw().setText(vw == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", vw).toUpperCase());
            controller.getLbVw_raspr().setText(vw == -1 ? "--" :
                    String.format(Locale.CANADA, "%.2f", vWHeterogeneity).toUpperCase());
        }
        if (controller.getCbPorog().isSelected()) {
            controller.getTfPorog().setText(porog == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", porog).toUpperCase());
        }
        if (controller.getCbPorogStar().isSelected()) {
            controller.getTfPorogStar().setText(porogStar == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", porogStar).toUpperCase());
        }
        if (controller.getCbDetectivity().isSelected()) {
            controller.getTfDetectivity().setText(detectivity == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", detectivity).toUpperCase());
        }
        if (controller.getCbDetectivityStar().isSelected()) {
            controller.getTfDetectivityStar().setText(detectivityStar == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", detectivityStar).toUpperCase());
        }
        if (controller.getCbNETD().isSelected()) {
            controller.getTfNETD().setText(NETD == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", NETD).toUpperCase());
        }
        if (controller.getCbExposure().isSelected()) {
            controller.getTfExposure().setText(exposure == -1 ? "--" :
                    String.format(Locale.CANADA, "%.3e", exposure).toUpperCase());
        }
    }

    /**
     * Сохраняем полученные данные.
     */
    public boolean saveExpData() {

        ExpInfo exp = controller.getController().getSelExp();
        exp.setArifmeticMean(arifmeticMean);
        exp.setQuadraticMean(quadraticMean);
        exp.setSKO(SKO);
        exp.setVw(vw);
        exp.setPorog(porog);
        exp.setPorogStar(porogStar);
        exp.setDetectivity(detectivity);
        exp.setDetectivityStar(detectivityStar);
        exp.setNETD(NETD);
        exp.setExposure(exposure);
        exp.setFrList(frList);
        exp.setScList(scList);
        //addExpToList(exp);
        exp.setBpInCenter(bpInCenter);
        exp.setBpAll(bpAll);
        exp.setWithDefPx(withDefPx);
        exp.setSizeX(sizeX);
        exp.setSizeY(sizeY);
        exp.setBuffToTXT(buffToTXT);
        return true;
    }

    private void addExpToList(ExpInfo exp) {
        int numb = controller.getController().getExpCounter();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        exp.setExpName(numb + ": " + dateFormat.format(timestamp) + " | Int: " + params.getTempInt() + " | VOS: " + params.getTempVOS() + " | VR0: " + params.getTempVR0() + " | counts:" + params.getCountFrames());
        exp.setExpIndex(numb);
        controller.getController().getOptionsExp().add(exp);
        //  Platform.runLater(() -> controller.getController().getCbExpOptions().getSelectionModel().select(exp));
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        controller.getController().save();
        Button btnParams = controller.getController().getBtnParams();
        ParamsController paramsController = controller.getController().getParamsFxmlLoader().getController();
        Button btnStart = paramsController.getBtnStart();
        Platform.runLater(() -> {
            controller.setButtonsDisable(false, false, false);
            btnParams.setStyle("-fx-background-color: green");
            btnStart.setStyle("-fx-background-color: green");
            controller.getpIndicator2().setVisible(false);
        });

    }
    @Override
    protected void cancelled() {
        super.cancelled();
    }

    @Override
    protected void failed() {
        super.failed();
        LOG.error("Failed!");
        Button btnParams = controller.getController().getBtnParams();
        ParamsController paramsController = controller.getController().getParamsFxmlLoader().getController();
        Button btnStart = paramsController.getBtnStart();
        Platform.runLater(() -> {
            controller.getBtnSave().setDisable(true);
            btnParams.setStyle("-fx-background-color: red");
            btnStart.setStyle("-fx-background-color: red");
            controller.getController().getLab_exp_status().textProperty().unbind();
            controller.getController().getLab_exp_status().textProperty().setValue("");
            controller.getpIndicator2().setVisible(false);
        });
    }
    @Override
    public boolean cancel() {
        LOG.error("Canceled!");
        Button btnParams = controller.getController().getBtnParams();
        ParamsController paramsController = controller.getController().getParamsFxmlLoader().getController();
        Button btnStart = paramsController.getBtnStart();
        Platform.runLater(() -> {
            btnParams.setStyle("-fx-background-color: red");
            btnStart.setStyle("-fx-background-color: red");
            controller.getController().getLab_exp_status().textProperty().unbind();
            controller.getController().getLab_exp_status().textProperty().setValue("Отмена записи файла");
            controller.getpIndicator2().setVisible(false);
        });
        return super.cancel();
    }
    /**
     * Запись листа с матрицами
     *
     * @return
     */
    public List<double[][]> getList() {
        ArrayList<double[][]> doubles = new ArrayList<>();
        doubles.add(arifmeticMeanValue_delta);
        doubles.add(quadraticMeanValue_delta);
        doubles.add(SKOValue);
        doubles.add(voltWatka);
        doubles.add(porogSensivity);
        doubles.add(porogSensivityStar);
        doubles.add(dDetectivity);
        doubles.add(dDetectivityStar);
        doubles.add(netd);
        doubles.add(eExposure);
        return doubles;
    }
}