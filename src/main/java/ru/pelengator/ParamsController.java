package ru.pelengator;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.service.ParamsService;
import ru.pelengator.service.SaveFilesService;

import java.io.File;
import java.net.URL;
import java.util.*;

import static ru.pelengator.API.util.Utils.*;

public class ParamsController implements Initializable {

    /**
     * Логгер
     */
    private static final Logger LOG = LoggerFactory.getLogger(ParamsController.class);


    /**
     * Лейбл размера окна
     */
    @FXML
    private Label lbFullWindow;
    @FXML
    private Label lbFullWindow1;
    /**
     * Среднее арифметическое сигнала
     */
    @FXML
    private TextField tfArifmeticMean;
    /**
     * Среднее квадратическое сигнала
     */
    @FXML
    private TextField tfQuadraticMean;
    /**
     * СКО сигнала
     */
    @FXML
    private TextField tfSKO;
    /**
     * Вольтовая чувствительность
     */
    @FXML
    private TextField tfVw;
    /**
     * Порог чувствительности
     */
    @FXML
    private TextField tfPorog;
    /**
     * Удельный порог чувствительноси
     */
    @FXML
    private TextField tfPorogStar;
    /**
     * Обнаружительная способность
     */
    @FXML
    private TextField tfDetectivity;
    /**
     * Удельная обнаружительная способность
     */
    @FXML
    private TextField tfDetectivityStar;
    /**
     * NETD
     */
    @FXML
    private TextField tfNETD;
    /**
     * Пороговая облученность
     */
    @FXML
    private TextField tfExposure;

    @FXML
    private TextField tfArifmeticMeanPersent;//Процент брака по параметру
    @FXML
    private TextField tfQuadraticMeanPersent;
    @FXML
    private TextField tfSKOPersent;
    @FXML
    private TextField tfVwPersent;
    @FXML
    private TextField tfPorogPersent;
    @FXML
    private TextField tfPorogStarPersent;
    @FXML
    private TextField tfDetectivityPersent;
    @FXML
    private TextField tfDetectivityStarPersent;
    @FXML
    private TextField tfNETDPersent;
    @FXML
    private TextField tfExposurePersent;

    @FXML
    private Label lbItog;
    @FXML
    private Label lbItog1;
    @FXML
    private Label lbArifmeticMean;//Результирующие значения
    @FXML
    private Label lbArifmeticMean1;//Результирующие значения
    @FXML
    private Label lbQuadraticMean;
    @FXML
    private Label lbQuadraticMean1;
    @FXML
    private Label lbSKO;
    @FXML
    private Label lbSKO1;
    @FXML
    private Label lbVw;
    @FXML
    private Label lbVw1;
    @FXML
    private Label lbPorog;
    @FXML
    private Label lbPorog1;
    @FXML
    private Label lbPorogStar;
    @FXML
    private Label lbPorogStar1;
    @FXML
    private Label lbDetectivity;
    @FXML
    private Label lbDetectivity1;
    @FXML
    private Label lbDetectivityStar;
    @FXML
    private Label lbDetectivityStar1;
    @FXML
    private Label lbNETD;
    @FXML
    private Label lbNETD1;
    @FXML
    private Label lbExposure;
    @FXML
    private Label lbExposure1;

    @FXML
    private TextField lbVw_raspr;

    @FXML
    private ColorPicker cpArifmeticMean;//Колорпикер для битых пикселей
    @FXML
    private ColorPicker cpQuadraticMean;
    @FXML
    private ColorPicker cpSKO;
    @FXML
    private ColorPicker cpVw;
    @FXML
    private ColorPicker cpPorog;
    @FXML
    private ColorPicker cpPorogStar;
    @FXML
    private ColorPicker cpDetectivity;
    @FXML
    private ColorPicker cpDetectivityStar;
    @FXML
    private ColorPicker cpNETD;
    @FXML
    private ColorPicker cpExposure;
    @FXML
    private ColorPicker cpItog;

    @FXML
    private CheckBox cbArifmeticMean;//Чекбоксы функций
    @FXML
    private CheckBox cbQuadraticMean;
    @FXML
    private CheckBox cbSKO;
    @FXML
    private CheckBox cbVw;
    @FXML
    private CheckBox cbPorog;
    @FXML
    private CheckBox cbPorogStar;
    @FXML
    private CheckBox cbDetectivity;
    @FXML
    private CheckBox cbDetectivityStar;
    @FXML
    private CheckBox cbNETD;
    @FXML
    private CheckBox cbExposure;
    @FXML
    private CheckBox cbPrint;
    /**
     * Расчет сучетом или без учета битых пикселей
     */
    @FXML
    private CheckBox cbWithBP;
    /**
     * Панель вывода гистограмм и кадров
     */
    @FXML
    private VBox scrlPane;

    @FXML
    private VBox scrlPane1;
    /**
     * Поле для вывода данных
     */
    /**
     * Кнопка старта эксперимента
     */
    @FXML
    private Button btnStart;

    /**
     * Кнопка сброса эксперимента
     */
    @FXML
    private Button btnReset;

    /**
     * Кнопка записи данных эксперимента
     */
    @FXML
    private Button btnSave;

    /**
     * Текстовое поле прогрессбара
     */
    @FXML
    private Label lab_status;

    /**
     * Прогрессбар
     */
    @FXML
    private ProgressBar pb_status;
    /**
     * Прогрессиндикатор
     */
    @FXML
    private ProgressIndicator pIndicator;
    @FXML
    private ProgressIndicator pIndicator2;
    /**
     * Сервис расчетов
     */
    private ParamsService service;

    /**
     * Ссылка на контроллер
     */
    private Controller controller;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.trace("Инициализация");
        resetButtons();
        showCrane(true);


        String str = "Вольтовая чувствительность, В\u00B7Вт\u00AF \u00B9";
        cbVw.setText(str);
        //точка   1/2
        str = "Порог чувствительности, Вт\u00B7Гц-\u00BD";
        cbPorog.setText(str);
        //точка  1/2  точка   минус  первая
        str = "Удельный порог чувствительности, Вт\u00B7Гц-\u00BD\u00B7см\u00AF \u00B9";
        cbPorogStar.setText(str);

        str = "Обнаружительная способность, Вт\u00AF \u00B9\u00B7Гц\u00BD";
        cbDetectivity.setText(str);

        str = "Удельная обнаруж. способность, Вт\u00AF \u00B9\u00B7Гц\u00BD\u00B7см";
        cbDetectivityStar.setText(str);

        str = "Пороговая облученность, Вт\u00B7см\u00AF \u00B2";
        cbExposure.setText(str);

        cbPrint.selectedProperty().addListener((observable, oldValue, newValue) -> controller.getSelExp().setPrintBpList(newValue));

        /**
         * Кнопка страрт
         */
        btnStart.setOnAction(event -> {
            LOG.trace("Старт");
            // clearPane(scrlPane);
            // clearPane(scrlPane1);
            showCrane(false);
            service.restart();//Стартуем сервис
            setButtonsDisable(true, false, true);//блок кнопок
        });

        btnReset.setOnAction(event -> {
            LOG.trace("Стоп");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            initService();
            Platform.runLater(() -> {
                btnStart.setStyle(null);
                setMinus();
                showCrane(true);
                clearPane(scrlPane);
                clearPane(scrlPane1);
                setAllPersent20();
            });

            setButtonsDisable(false, true, true);//блок кнопок
        });

        btnSave.setOnAction(event -> {
            LOG.trace("Сохранение в файл");
            saveAllInFile(event);
            setButtonsDisable(false, false, false);//блок кнопок
        });

    }

    /**
     * Сброс процентовки на 20%
     */
    private void setAllPersent20() {
        controller.getParams().setArifmeticMeanPersent(20.0);
        tfArifmeticMeanPersent.setText("20.0");
        controller.getParams().setQuadraticMeanPersent(20.0);
        tfQuadraticMeanPersent.setText("20.0");
        controller.getParams().setSKOPersent(20.0);
        tfSKOPersent.setText("20.0");
        controller.getParams().setVwPersent(20.0);
        tfVwPersent.setText("20.0");
        controller.getParams().setPorogPersent(20.0);
        tfPorogPersent.setText("20.0");
        controller.getParams().setPorogStarPersent(20.0);
        tfPorogStarPersent.setText("20.0");
        controller.getParams().setDetectivityPersent(20.0);
        tfDetectivityPersent.setText("20.0");
        controller.getParams().setDetectivityStarPersent(20.0);
        tfDetectivityStarPersent.setText("20.0");
        controller.getParams().setNETDPersent(20.0);
        tfNETDPersent.setText("20.0");
        controller.getParams().setExposurePersent(20.0);
        tfExposurePersent.setText("20.0");
    }


    /**
     * Сервис сохранения в файл
     */
    private SaveFilesService saveFilesService;

    /**
     * Сохранение в файл
     *
     * @param event эвент для внутрянки
     */
    private void saveAllInFile(ActionEvent event) {

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчет");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("All Files", "*.*")/*,
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")*/);

        File pdfFile = fileChooser.showSaveDialog(stage);


        if (saveFilesService == null) {
            saveFilesService = new SaveFilesService(this, pdfFile);
        }

        pb_status.visibleProperty().bind(saveFilesService.runningProperty());
        pb_status.progressProperty().bind(saveFilesService.progressProperty());
        lab_status.textProperty().bind(saveFilesService.messageProperty());

        if (saveFilesService.getState() == Worker.State.RUNNING) {
            saveFilesService.cancel();
        } else {
            saveFilesService.restart();
        }
        saveFilesService = null;
    }

    /**
     * Отработка колорпикера
     */
    private void addCBListeners() {
        cpArifmeticMean.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorArifm(newValue.toString()));
        cpQuadraticMean.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorQuadratic(newValue.toString()));
        cpSKO.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorSKO(newValue.toString()));
        cpVw.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorVw(newValue.toString()));
        cpPorog.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorPorog(newValue.toString()));
        cpPorogStar.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorPorogStar(newValue.toString()));
        cpDetectivity.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorDetectivity(newValue.toString()));
        cpDetectivityStar.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorDetectivityStar(newValue.toString()));
        cpNETD.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorNETD(newValue.toString()));
        cpExposure.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setPersentColorExposure(newValue.toString()));
        cpItog.valueProperty().addListener((observable, oldValue, newValue) ->
                controller.getParams().setItogColor(newValue.toString()));


    }


    /**
     * Блокировка кнопок
     *
     * @param startBut
     * @param resetBut
     * @param saveBut
     */
    public void setButtonsDisable(boolean startBut, boolean resetBut, boolean saveBut) {
        btnStart.setDisable(startBut);
        btnReset.setDisable(resetBut);
        btnSave.setDisable(saveBut);
        btnSave.setStyle("");

    }

    /**
     * Сброс кнопок
     */
    public void resetButtons() {
        setButtonsDisable(false, true, true);
    }


    /**
     * Инициализация сервиса
     */
    public void initService() {
        service = new ParamsService(this);
        controller.getPb_exp().visibleProperty().bind(pb_status.visibleProperty());
        controller.getPb_exp().progressProperty().bind(pb_status.progressProperty());
        controller.getLab_exp_status().textProperty().bind(lab_status.textProperty());

        pb_status.visibleProperty().bind(service.runningProperty());
        pb_status.progressProperty().bind(service.progressProperty());
        lab_status.textProperty().bind(service.messageProperty());
    }

    /**
     * Инициализация контроллера
     *
     * @param controller
     */
    public void initController(Controller controller) {
        LOG.trace("Инициализация контроллера");
        this.controller = controller;
        initService();
        lbFullWindow.setText(controller.getParams().getDimention());
        lbFullWindow1.setText(controller.getParams().getDimention());
        tfArifmeticMeanPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getArifmeticMeanPersent()));
        tfQuadraticMeanPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getQuadraticMeanPersent()));
        tfSKOPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getSKOPersent()));
        tfVwPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getVwPersent()));
        tfPorogPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getPorogPersent()));
        tfPorogStarPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getPorogStarPersent()));
        tfDetectivityPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getDetectivityPersent()));
        tfDetectivityStarPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getDetectivityStarPersent()));
        tfNETDPersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getNETDPersent()));
        tfExposurePersent.setText(String.format(Locale.CANADA, "%.2f",
                controller.getParams().getExposurePersent()));
        setMinus();
        cpArifmeticMean.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorArifm()));
        cpQuadraticMean.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorQuadratic()));
        cpSKO.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorSKO()));
        cpVw.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorVw()));
        cpPorog.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorPorog()));
        cpPorogStar.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorPorogStar()));
        cpDetectivity.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorDetectivity()));
        cpDetectivityStar.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorDetectivityStar()));
        cpNETD.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorNETD()));
        cpExposure.valueProperty().setValue(
                Color.web(controller.getParams().getPersentColorExposure()));
        cpItog.valueProperty().setValue(
                Color.web(controller.getParams().getItogColor()));

        cbWithBP.selectedProperty().bindBidirectional(controller.getParams().cbWithBPProperty());
        cbPrint.selectedProperty().bindBidirectional(controller.getParams().cbPrintProperty());
        addCBListeners();
    }

    /**
     * Установка минусиков
     */
    private void setMinus() {

        tfArifmeticMean.setText("--");
        tfQuadraticMean.setText("--");
        tfSKO.setText("--");
        tfVw.setText("--");
        tfPorog.setText("--");
        tfPorogStar.setText("--");
        tfDetectivity.setText("--");
        tfDetectivityStar.setText("--");
        tfNETD.setText("--");
        tfExposure.setText("--");
        lbVw_raspr.setText("--");
        ArrayList<Label> labels = new ArrayList<>(Arrays.asList(lbArifmeticMean, lbArifmeticMean1, lbQuadraticMean, lbQuadraticMean1, lbSKO,
                lbSKO1, lbVw, lbVw1, lbPorog, lbPorog1, lbPorogStar, lbPorogStar1, lbDetectivity, lbDetectivity1,
                lbDetectivityStar, lbDetectivityStar1, lbNETD, lbNETD1, lbExposure, lbExposure1, lbItog, lbItog1));
        for (Label l : labels) {
            l.setText("--");
        }

    }

    public Controller getController() {
        return controller;
    }

    //Отработка нажатий в полях
    @FXML
    public void setArifmeticMeanPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setArifmeticMeanPersent(d);
        showCrane(true);
    }

    @FXML
    public void setQuadraticMeanPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setQuadraticMeanPersent(d);
        showCrane(true);
    }

    @FXML
    public void setSKOPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setSKOPersent(d);
        showCrane(true);
    }

    @FXML
    public void setVwPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setVwPersent(d);
        showCrane(true);
    }

    @FXML
    public void setPorogPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setPorogPersent(d);
        showCrane(true);
    }

    @FXML
    public void setPorogStarPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setPorogStarPersent(d);
        showCrane(true);
    }

    @FXML
    public void setDetectivityPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setDetectivityPersent(d);
        showCrane(true);
    }

    @FXML
    public void setDetectivityStarPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setDetectivityStarPersent(d);
        showCrane(true);
    }

    @FXML
    public void setNETDPersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setNETDPersent(d);
        showCrane(true);
    }

    @FXML
    public void setExposurePersent(ActionEvent event) {
        double d = parseDoubleText(event);
        controller.getParams().setExposurePersent(d);
        showCrane(true);
    }


    /**
     * Отображение необходимости перерасчета
     *
     * @param b true-отобразить, false -убрать отображение
     */
    private void showCrane(boolean b) {

        if (b) {
            mayak(true);
        } else {
            mayak(false);
        }
    }

    /**
     * Задание на мигание
     */
    static private TimerTask timerTask;
    /**
     * Сам таймер
     */
    static private Timer tm;
    /**
     * Отображение мигания
     */
    static private boolean craneShow = false;

    private void mayak(boolean b) {
        if (b && !craneShow) {
            if (tm != null) {
                tm.cancel();
            }
            tm = new Timer();
            timerTask = new TimerTask() {
                boolean i = true;
                String txt1 = "Рассчитать";
                String txt = "Рассчитать".toUpperCase();

                @Override
                public void run() {
                    Platform.runLater(() -> {
                        btnStart.setText(i ? txt : txt1);
                    });
                    i = !i;
                }
            };
            tm.schedule(timerTask, 0, 1000);
            craneShow = true;
        } else if (!b && craneShow) {
            tm.cancel();
            tm = null;
            timerTask = null;
            Platform.runLater(() -> {
                btnStart.setText("Рассчитано");
            });
            craneShow = false;
        }
    }


    public TextField getTfArifmeticMean() {
        return tfArifmeticMean;
    }

    public void setTfArifmeticMean(TextField tfArifmeticMean) {
        this.tfArifmeticMean = tfArifmeticMean;
    }

    public TextField getTfQuadraticMean() {
        return tfQuadraticMean;
    }

    public void setTfQuadraticMean(TextField tfQuadraticMean) {
        this.tfQuadraticMean = tfQuadraticMean;
    }

    public TextField getTfSKO() {
        return tfSKO;
    }

    public void setTfSKO(TextField tfSKO) {
        this.tfSKO = tfSKO;
    }

    public TextField getTfVw() {
        return tfVw;
    }

    public void setTfVw(TextField tfVw) {
        this.tfVw = tfVw;
    }

    public TextField getTfPorog() {
        return tfPorog;
    }

    public void setTfPorog(TextField tfPorog) {
        this.tfPorog = tfPorog;
    }

    public TextField getTfPorogStar() {
        return tfPorogStar;
    }

    public void setTfPorogStar(TextField tfPorogStar) {
        this.tfPorogStar = tfPorogStar;
    }

    public TextField getTfDetectivity() {
        return tfDetectivity;
    }

    public void setTfDetectivity(TextField tfDetectivity) {
        this.tfDetectivity = tfDetectivity;
    }

    public TextField getTfDetectivityStar() {
        return tfDetectivityStar;
    }

    public void setTfDetectivityStar(TextField tfDetectivityStar) {
        this.tfDetectivityStar = tfDetectivityStar;
    }

    public TextField getTfNETD() {
        return tfNETD;
    }

    public void setTfNETD(TextField tfNETD) {
        this.tfNETD = tfNETD;
    }

    public TextField getTfExposure() {
        return tfExposure;
    }

    public void setTfExposure(TextField tfExposure) {
        this.tfExposure = tfExposure;
    }

    public TextField getTfArifmeticMeanPersent() {
        return tfArifmeticMeanPersent;
    }

    public void setTfArifmeticMeanPersent(TextField tfArifmeticMeanPersent) {
        this.tfArifmeticMeanPersent = tfArifmeticMeanPersent;
    }

    public TextField getTfQuadraticMeanPersent() {
        return tfQuadraticMeanPersent;
    }

    public void setTfQuadraticMeanPersent(TextField tfQuadraticMeanPersent) {
        this.tfQuadraticMeanPersent = tfQuadraticMeanPersent;
    }

    public TextField getTfSKOPersent() {
        return tfSKOPersent;
    }

    public void setTfSKOPersent(TextField tfSKOPersent) {
        this.tfSKOPersent = tfSKOPersent;
    }

    public TextField getTfVwPersent() {
        return tfVwPersent;
    }

    public void setTfVwPersent(TextField tfVwPersent) {
        this.tfVwPersent = tfVwPersent;
    }

    public TextField getTfPorogPersent() {
        return tfPorogPersent;
    }

    public void setTfPorogPersent(TextField tfPorogPersent) {
        this.tfPorogPersent = tfPorogPersent;
    }

    public TextField getTfPorogStarPersent() {
        return tfPorogStarPersent;
    }

    public void setTfPorogStarPersent(TextField tfPorogStarPersent) {
        this.tfPorogStarPersent = tfPorogStarPersent;
    }

    public TextField getTfDetectivityPersent() {
        return tfDetectivityPersent;
    }

    public void setTfDetectivityPersent(TextField tfDetectivityPersent) {
        this.tfDetectivityPersent = tfDetectivityPersent;
    }

    public TextField getTfDetectivityStarPersent() {
        return tfDetectivityStarPersent;
    }

    public void setTfDetectivityStarPersent(TextField tfDetectivityStarPersent) {
        this.tfDetectivityStarPersent = tfDetectivityStarPersent;
    }

    public TextField getTfNETDPersent() {
        return tfNETDPersent;
    }

    public void setTfNETDPersent(TextField tfNETDPersent) {
        this.tfNETDPersent = tfNETDPersent;
    }

    public TextField getTfExposurePersent() {
        return tfExposurePersent;
    }

    public void setTfExposurePersent(TextField tfExposurePersent) {
        this.tfExposurePersent = tfExposurePersent;
    }

    public Label getLbArifmeticMean1() {
        return lbArifmeticMean1;
    }

    public Label getLbItog() {
        return lbItog;
    }

    public void setLbItog(Label lbItog) {
        this.lbItog = lbItog;
    }

    public Label getLbItog1() {
        return lbItog1;
    }

    public void setLbItog1(Label lbItog1) {
        this.lbItog1 = lbItog1;
    }

    public void setLbArifmeticMean1(Label lbArifmeticMean1) {
        this.lbArifmeticMean1 = lbArifmeticMean1;
    }

    public Label getLbQuadraticMean1() {
        return lbQuadraticMean1;
    }

    public void setLbQuadraticMean1(Label lbQuadraticMean1) {
        this.lbQuadraticMean1 = lbQuadraticMean1;
    }

    public Label getLbSKO1() {
        return lbSKO1;
    }

    public void setLbSKO1(Label lbSKO1) {
        this.lbSKO1 = lbSKO1;
    }

    public Label getLbVw1() {
        return lbVw1;
    }

    public void setLbVw1(Label lbVw1) {
        this.lbVw1 = lbVw1;
    }

    public Label getLbPorog1() {
        return lbPorog1;
    }

    public void setLbPorog1(Label lbPorog1) {
        this.lbPorog1 = lbPorog1;
    }

    public Label getLbPorogStar1() {
        return lbPorogStar1;
    }

    public void setLbPorogStar1(Label lbPorogStar1) {
        this.lbPorogStar1 = lbPorogStar1;
    }

    public Label getLbDetectivity1() {
        return lbDetectivity1;
    }

    public void setLbDetectivity1(Label lbDetectivity1) {
        this.lbDetectivity1 = lbDetectivity1;
    }

    public Label getLbDetectivityStar1() {
        return lbDetectivityStar1;
    }

    public void setLbDetectivityStar1(Label lbDetectivityStar1) {
        this.lbDetectivityStar1 = lbDetectivityStar1;
    }

    public Label getLbNETD1() {
        return lbNETD1;
    }

    public void setLbNETD1(Label lbNETD1) {
        this.lbNETD1 = lbNETD1;
    }

    public Label getLbExposure1() {
        return lbExposure1;
    }

    public void setLbExposure1(Label lbExposure1) {
        this.lbExposure1 = lbExposure1;
    }

    public Label getLbArifmeticMean() {
        return lbArifmeticMean;
    }

    public void setLbArifmeticMean(Label lbArifmeticMean) {
        this.lbArifmeticMean = lbArifmeticMean;
    }

    public Label getLbQuadraticMean() {
        return lbQuadraticMean;
    }

    public void setLbQuadraticMean(Label lbQuadraticMean) {
        this.lbQuadraticMean = lbQuadraticMean;
    }

    public Label getLbSKO() {
        return lbSKO;
    }

    public void setLbSKO(Label lbSKO) {
        this.lbSKO = lbSKO;
    }

    public Label getLbVw() {
        return lbVw;
    }

    public void setLbVw(Label lbVw) {
        this.lbVw = lbVw;
    }

    public Label getLbPorog() {
        return lbPorog;
    }

    public void setLbPorog(Label lbPorog) {
        this.lbPorog = lbPorog;
    }

    public Label getLbPorogStar() {
        return lbPorogStar;
    }

    public void setLbPorogStar(Label lbPorogStar) {
        this.lbPorogStar = lbPorogStar;
    }

    public Label getLbDetectivity() {
        return lbDetectivity;
    }

    public void setLbDetectivity(Label lbDetectivity) {
        this.lbDetectivity = lbDetectivity;
    }

    public Label getLbDetectivityStar() {
        return lbDetectivityStar;
    }

    public void setLbDetectivityStar(Label lbDetectivityStar) {
        this.lbDetectivityStar = lbDetectivityStar;
    }

    public Label getLbNETD() {
        return lbNETD;
    }

    public void setLbNETD(Label lbNETD) {
        this.lbNETD = lbNETD;
    }

    public Label getLbExposure() {
        return lbExposure;
    }

    public void setLbExposure(Label lbExposure) {
        this.lbExposure = lbExposure;
    }

    public ColorPicker getCpArifmeticMean() {
        return cpArifmeticMean;
    }

    public void setCpArifmeticMean(ColorPicker cpArifmeticMean) {
        this.cpArifmeticMean = cpArifmeticMean;
    }

    public ColorPicker getCpQuadraticMean() {
        return cpQuadraticMean;
    }

    public void setCpQuadraticMean(ColorPicker cpQuadraticMean) {
        this.cpQuadraticMean = cpQuadraticMean;
    }

    public ColorPicker getCpSKO() {
        return cpSKO;
    }

    public void setCpSKO(ColorPicker cpSKO) {
        this.cpSKO = cpSKO;
    }

    public ColorPicker getCpVw() {
        return cpVw;
    }

    public void setCpVw(ColorPicker cpVw) {
        this.cpVw = cpVw;
    }

    public ColorPicker getCpPorog() {
        return cpPorog;
    }

    public void setCpPorog(ColorPicker cpPorog) {
        this.cpPorog = cpPorog;
    }

    public ColorPicker getCpPorogStar() {
        return cpPorogStar;
    }

    public void setCpPorogStar(ColorPicker cpPorogStar) {
        this.cpPorogStar = cpPorogStar;
    }

    public ColorPicker getCpDetectivity() {
        return cpDetectivity;
    }

    public void setCpDetectivity(ColorPicker cpDetectivity) {
        this.cpDetectivity = cpDetectivity;
    }

    public ColorPicker getCpDetectivityStar() {
        return cpDetectivityStar;
    }

    public void setCpDetectivityStar(ColorPicker cpDetectivityStar) {
        this.cpDetectivityStar = cpDetectivityStar;
    }

    public ColorPicker getCpNETD() {
        return cpNETD;
    }

    public void setCpNETD(ColorPicker cpNETD) {
        this.cpNETD = cpNETD;
    }

    public ColorPicker getCpExposure() {
        return cpExposure;
    }

    public void setCpExposure(ColorPicker cpExposure) {
        this.cpExposure = cpExposure;
    }

    public CheckBox getCbArifmeticMean() {
        return cbArifmeticMean;
    }

    public void setCbArifmeticMean(CheckBox cbArifmeticMean) {
        this.cbArifmeticMean = cbArifmeticMean;
    }

    public CheckBox getCbQuadraticMean() {
        return cbQuadraticMean;
    }

    public void setCbQuadraticMean(CheckBox cbQuadraticMean) {
        this.cbQuadraticMean = cbQuadraticMean;
    }

    public CheckBox getCbSKO() {
        return cbSKO;
    }

    public void setCbSKO(CheckBox cbSKO) {
        this.cbSKO = cbSKO;
    }

    public CheckBox getCbVw() {
        return cbVw;
    }

    public void setCbVw(CheckBox cbVw) {
        this.cbVw = cbVw;
    }

    public CheckBox getCbPorog() {
        return cbPorog;
    }

    public void setCbPorog(CheckBox cbPorog) {
        this.cbPorog = cbPorog;
    }

    public CheckBox getCbPorogStar() {
        return cbPorogStar;
    }

    public void setCbPorogStar(CheckBox cbPorogStar) {
        this.cbPorogStar = cbPorogStar;
    }

    public CheckBox getCbDetectivity() {
        return cbDetectivity;
    }

    public void setCbDetectivity(CheckBox cbDetectivity) {
        this.cbDetectivity = cbDetectivity;
    }

    public CheckBox getCbDetectivityStar() {
        return cbDetectivityStar;
    }

    public void setCbDetectivityStar(CheckBox cbDetectivityStar) {
        this.cbDetectivityStar = cbDetectivityStar;
    }

    public CheckBox getCbNETD() {
        return cbNETD;
    }

    public void setCbNETD(CheckBox cbNETD) {
        this.cbNETD = cbNETD;
    }

    public CheckBox getCbExposure() {
        return cbExposure;
    }

    public void setCbExposure(CheckBox cbExposure) {
        this.cbExposure = cbExposure;
    }

    public CheckBox getCbWithBP() {
        return cbWithBP;
    }

    public void setCbWithBP(CheckBox cbWithBP) {
        this.cbWithBP = cbWithBP;
    }

    public VBox getScrlPane() {
        return scrlPane;
    }

    public void setScrlPane(VBox scrlPane) {
        this.scrlPane = scrlPane;
    }


    public Button getBtnStart() {
        return btnStart;
    }

    public void setBtnStart(Button btnStart) {
        this.btnStart = btnStart;
    }

    public Button getBtnReset() {
        return btnReset;
    }

    public void setBtnReset(Button btnReset) {
        this.btnReset = btnReset;
    }

    public Button getBtnSave() {
        return btnSave;
    }

    public void setBtnSave(Button btnSave) {
        this.btnSave = btnSave;
    }

    public Label getLab_status() {
        return lab_status;
    }

    public void setLab_status(Label lab_status) {
        this.lab_status = lab_status;
    }

    public ProgressBar getPb_status() {
        return pb_status;
    }

    public void setPb_status(ProgressBar pb_status) {
        this.pb_status = pb_status;
    }

    public ParamsService getService() {
        return service;
    }

    public void setService(ParamsService service) {
        this.service = service;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public CheckBox getCbPrint() {
        return cbPrint;
    }

    public void setCbPrint(CheckBox cbPrint) {
        this.cbPrint = cbPrint;
    }

    public ProgressIndicator getpIndicator() {
        return pIndicator;
    }

    public ProgressIndicator getpIndicator2() {
        return pIndicator2;
    }

    public void setpIndicator2(ProgressIndicator pIndicator2) {
        this.pIndicator2 = pIndicator2;
    }

    public void setpIndicator(ProgressIndicator pIndicator) {
        this.pIndicator = pIndicator;
    }

    public SaveFilesService getSaveFilesService() {
        return saveFilesService;
    }

    public TextField getLbVw_raspr() {
        return lbVw_raspr;
    }

    public void setLbVw_raspr(TextField lbVw_raspr) {
        this.lbVw_raspr = lbVw_raspr;
    }

    public ColorPicker getCpItog() {
        return cpItog;
    }

    public void setCpItog(ColorPicker cpItog) {
        this.cpItog = cpItog;
    }

    public VBox getScrlPane1() {
        return scrlPane1;
    }

    public void setScrlPane1(VBox scrlPane1) {
        this.scrlPane1 = scrlPane1;
    }

    public void setSaveFilesService(SaveFilesService saveFilesService) {
        this.saveFilesService = saveFilesService;
    }
}
