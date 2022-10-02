package ru.pelengator.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorEvent;
import ru.pelengator.API.DetectorListener;
import ru.pelengator.API.utils.StatisticsUtils;
import ru.pelengator.API.utils.Utils;
import ru.pelengator.Controller;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.pelengator.API.utils.Utils.*;

/**
 * Сервис сбора данных.
 */
public class DataService extends Service<Void> implements DetectorListener {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataService.class);
    /**
     * Меньший поток.
     */
    private ArrayList<int[][]> dataArray_0;
    /**
     * Больший поток.
     */
    private ArrayList<int[][]> dataArray_1;
    /**
     * Данные в вольтах.
     */
    private double[][] dataArrayMeanValue_0;
    private double[][] dataArrayMeanValue_1;
    /**
     * Статика в АЦП.
     */
    private StatisticsUtils[][] dataArrayStat_0;
    private StatisticsUtils[][] dataArrayStat_1;
    /**
     * Дельта среднего значения по потокам. В вольтах.
     */
    private double totalMeanValue;
    /**
     * Размер кадра.
     */
    private int widthSize;//ширина
    private int heigthSize;//высота
    /**
     * Число отсчетов.
     */
    private int count;
    /**
     * Промежуточный кадр.
     */
    private BufferedImage tempImage;
    /**
     * Ссылка на контроллер.
     */
    private Controller controller;
    /**
     * Флаг набора части кадров.
     */
    private AtomicBoolean flag_part = new AtomicBoolean(false);
    /**
     * Флаг паузы в наборе.
     */
    private AtomicBoolean flag_pause = new AtomicBoolean(false);
    /**
     * Флаг повтора набора кадров.
     */
    private AtomicBoolean flag_2ndTry = new AtomicBoolean(true);

    /**
     * Конструктор.
     *
     * @param controller
     */
    public DataService(Controller controller) {
        this.controller = controller;
    }

    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                LOG.trace("Staring task");
                updateMessage("Старт Сервиса");
                updateProgress(0.0, 1);
                updateMessage("Инициализация");
                initParams();
                //////////////////////////////Первый поток////////////////
                updateMessage("Задание на выборку: " + count + " значений.");

                do {
                    addListnr();
                    dataArray_0.clear();
                    while (dataArray_0.size() < count) {
                        updateMessage("Набор кадров... 1-я часть " + dataArray_0.size() + "/" + count);
                        updateProgress((0.5D / count) * dataArray_0.size(), 1);
                    }
                    removeListener();
                    updateMessage("Собрана 1-я часть.");
                    updateProgress(0.5D, 1);

                    //Окно подтверждения готовности второго потока
                    showAndWait(flag_pause, flag_2ndTry);
                    do {
                        TimeUnit.SECONDS.sleep(1);
                    } while (!flag_pause.get());
                    flag_pause.set(false);
                } while (flag_2ndTry.get());
                ////////////////////////////////Второй поток////////////////
                flag_part.set(true);
                flag_2ndTry.set(true);
                do {
                    dataArray_1.clear();
                    addListnr();
                    updateMessage("Задание на выборку: " + count + " значений");
                    //набор массива кадров
                    while (dataArray_1.size() < count) {
                        updateMessage("Набор кадров... 2-я часть " + dataArray_1.size() + "/" + count);
                        updateProgress((0.5D / count) * dataArray_1.size() + 0.5D, 1);
                    }
                    removeListener();

                    updateMessage("Собрана 2-я часть." + (dataArray_1.size()) + "/" + count);
                    showAndWait(flag_pause, flag_2ndTry);
                    do {
                        TimeUnit.SECONDS.sleep(1);
                    } while (!flag_pause.get());
                    flag_pause.set(false);
                } while (flag_2ndTry.get());

                ////////////////////////////////////конец////////////////
                updateProgress(0.98, 1);
                updateMessage("Проверка потоков на корректность.");

                takeStat();
                takeAverage();
                proofFail();

                updateMessage("Сохранение данных");
                saveExpData();
                updateMessage("Данные сохранены");
                updateMessage("");
                updateProgress(1, 1);
                return null;
            }


        };
    }

    /**
     * Показ окна подтверждения выхода на следующие условия.
     *
     * @param flag_pause  флаг паузы между двумя выборками.
     * @param flag_2ndTry флаг повтора операции.
     */
    private void showAndWait(AtomicBoolean flag_pause, AtomicBoolean flag_2ndTry) {

        ButtonType repeatBtn = new ButtonType("Повторить набор", ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType goNextBtn = new ButtonType("Продолжить", ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonType cancelBtn = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText(null);
            alert.setContentText("Данные набраны. Продолжаем?");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(repeatBtn, goNextBtn, cancelBtn);
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            alert.setX(bounds.getMaxX() / 2 - alert.getDialogPane().getWidth() / 2);
            alert.setY(bounds.getMaxY() / 2 + alert.getDialogPane().getHeight() * 2);
            Optional<ButtonType> buttonType = alert.showAndWait();

            switch (buttonType.get().getText()) {
                case ("Повторить набор"): {
                    flag_pause.set(true);
                    break;
                }
                case ("Продолжить"): {
                    flag_2ndTry.set(false);
                    flag_pause.set(true);
                    break;
                }
                case ("Отмена"): {
                    cancel();
                    break;
                }
            }
        });
    }

    /**
     * Добавление в слугшатели на набор кадров.
     */
    private void addListnr() {
        controller.getSelDetector().addDetectorListener(this);
    }


    /**
     * Инициализация параметров.
     */
    private void initParams() {

        count = controller.getParams().getCountFrames();

        dataArray_0 = new ArrayList<>();
        dataArray_1 = new ArrayList<>();

        String selectedDist = controller.getCbDimOptions().getSelectionModel().getSelectedItem();

        String[] splitedSize = selectedDist.split("\\*");
        widthSize = Integer.parseInt(splitedSize[0]);
        heigthSize = Integer.parseInt(splitedSize[1]);

        dataArrayMeanValue_0 = new double[heigthSize][widthSize];
        dataArrayMeanValue_1 = new double[heigthSize][widthSize];

        dataArrayStat_0 = new StatisticsUtils[heigthSize][widthSize];
        dataArrayStat_1 = new StatisticsUtils[heigthSize][widthSize];

        for (int h = 0; h < heigthSize; h++) {
            for (int w = 0; w < widthSize; w++) {
                dataArrayStat_0[h][w] = new StatisticsUtils();
            }
        }
        for (int h = 0; h < heigthSize; h++) {
            for (int w = 0; w < widthSize; w++) {
                dataArrayStat_1[h][w] = new StatisticsUtils();
            }
        }
        totalMeanValue = 0;

        flag_part.set(false);
        flag_pause.set(false);
        flag_2ndTry.set(true);
    }


    /**
     * Проверка на соответствие меньшему и большему потокам.
     */
    private void proofFail() {

        double[] meanValues_0 = makeMaxMeanMin(dataArrayMeanValue_0, true, 0);
        double totalMeanValue_0 = meanValues_0[1];
        double[] meanValues_1 = makeMaxMeanMin(dataArrayMeanValue_1, true, 0);
        double totalMeanValue_1 = meanValues_1[1];

        totalMeanValue = totalMeanValue_1 - totalMeanValue_0;
        /**
         * При нарушении последовательности измерений реверсируем данные.
         */
        if (totalMeanValue <= 0) {
            showAlert("Первый поток больше второго. Реверсирую данные!");
        }
    }


    /**
     * Вывод окна предупреждения.
     *
     * @param alertTxt Текст предупреждения.
     */
    private void showAlert(String alertTxt) {

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("WARNING");
            alert.setHeaderText(null);
            alert.setContentText(alertTxt);
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            alert.setX(bounds.getMaxX() / 2 - alert.getDialogPane().getWidth() / 2);
            alert.setY(bounds.getMaxY() / 2 + alert.getDialogPane().getHeight() * 2);
            alert.show();
        });
    }

    /**
     * Набираем массив среднего арифметического значения.
     */
    private void takeAverage() {
        //пробегаем по каналам
        for (int h = 0; h < heigthSize; h++) {
            for (int w = 0; w < widthSize; w++) {
                dataArrayMeanValue_0[h][w] = dataArrayStat_0[h][w].getMean();
            }
        }
        for (int h = 0; h < heigthSize; h++) {
            for (int w = 0; w < widthSize; w++) {
                dataArrayMeanValue_1[h][w] = dataArrayStat_1[h][w].getMean();
            }
        }
    }


    /**
     * Набираем массив статистики.
     */
    private void takeStat() {
        for (int j = 0; j < count; j++) {
            int[][] frame0 = dataArray_0.get(j);
            int[][] frame1 = dataArray_1.get(j);
            for (int h = 0; h < heigthSize; h++) {
                for (int w = 0; w < widthSize; w++) {
                    dataArrayStat_0[h][w].addValue(frame0[h][w]);
                    dataArrayStat_1[h][w].addValue(frame1[h][w]);
                }
            }
        }
    }

    /**
     * Сохраняем полученные данные.
     */
    public void saveExpData() {
        if (totalMeanValue <= 0) {
            controller.getSelExp().setDataArray0(dataArray_1);
            controller.getSelExp().setDataArray1(dataArray_0);
        } else {
            controller.getSelExp().setDataArray0(dataArray_0);
            controller.getSelExp().setDataArray1(dataArray_1);
        }
        controller.getSelExp().setTempImage(tempImage);
        tempImage = null;
    }


    @Override
    public void detectorOpen(DetectorEvent de) {

    }

    @Override
    public void detectorClosed(DetectorEvent de) {

    }

    @Override
    public void detectorDisposed(DetectorEvent de) {

    }

    @Override
    public void detectorImageObtained(DetectorEvent de) {

        BufferedImage image = de.getImage();
        if (tempImage == null) {
            tempImage = Utils.copyImage(image);
        }
        int[][] ints = convertImageToArray(image);
        if (flag_part.get()) {
            dataArray_1.add(ints);
        } else {
            dataArray_0.add(ints);
        }
    }

    /**
     * Отключение слушателя новых кадров.
     */
    private void removeListener() {
        controller.getSelDetector().removeDetectorListener(this);
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        Button btnGetData = controller.getBtnGetData();

        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: green");
            controller.getBtnParams().setDisable(false);
        });

        controller.save();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        removeListener();
        controller.save();

    }

    @Override
    protected void failed() {
        super.failed();
        LOG.error("DataServise Failed!");
        Button btnGetData = controller.getBtnGetData();
        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: red");
            controller.getLab_exp_status().textProperty().unbind();
            controller.getLab_exp_status().textProperty().setValue("");
        });
        controller.save();
    }

    @Override
    public boolean cancel() {
        LOG.error("DataServise Canceled!");
        Button btnGetData = controller.getBtnGetData();
        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: red");
            controller.getLab_exp_status().textProperty().unbind();
            controller.getLab_exp_status().textProperty().setValue("");
        });
        controller.save();
        return super.cancel();

    }
}
