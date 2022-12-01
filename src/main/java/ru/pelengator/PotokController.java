package ru.pelengator;

import at.favre.lib.bytes.Bytes;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorException;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.service.PotokService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.*;

import static ru.pelengator.API.utils.Utils.*;

public class PotokController implements Initializable {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PotokController.class);
    /**
     * Кнопка выбора необходимости сохранения Exel.
     */
    @FXML
    private ToggleButton tbExel;
    /**
     * Кнопка выбора необходимости сохранения TXT.
     */
    @FXML
    private ToggleButton tbTxt;
    /**
     * Кнопка выбора необходимости сохранения PDF.
     */
    @FXML
    private ToggleButton tbPdf;
    /**
     * Количество отсчетов.
     */
    @FXML
    private TextField tfFrameCount;
    /**
     * Температура 0.
     */
    @FXML
    private TextField tfTemp0;
    /**
     * Температура 1.
     */
    @FXML
    private TextField tfTemp1;
    /**
     * Постоянная Больцмана 0.
     */
    @FXML
    private TextField tfPlank0;
    /**
     * Постоянная Больцмана 1.
     */
    @FXML
    private TextField tfPlank1;
    /**
     * Коэффициент излучения 0.
     */
    @FXML
    private TextField tfEps0;
    /**
     * Коэффициент излучения 1.
     */
    @FXML
    private TextField tfEps1;
    /**
     * Площадь отверстия диафрагмы АЧТ 0.
     */
    @FXML
    private TextField tfAreaACHT0;
    /**
     * Площадь отверстия диафрагмы АЧТ 1.
     */
    @FXML
    private TextField tfAreaACHT1;
    /**
     * Расстояние между диафрагмой АЧТ и плоскостью фоточувствительного элемента испытуемого образца 0.
     */
    @FXML
    private TextField tfRasst0;
    /**
     * Расстояние между диафрагмой АЧТ и плоскостью фоточувствительного элемента испытуемого образца 1.
     */
    @FXML
    private TextField tfRasst1;
    /**
     * Коэффициент поправки 0.
     */
    @FXML
    private TextField tfBetta0;
    /**
     * Коэффициент поправки 1.
     */
    @FXML
    private TextField tfBetta1;
    /**
     * Эффективная фоточувствительная площадь испытуемого образца 0.
     */
    @FXML
    private TextField tfAreaFPU0;
    /**
     * Эффективная фоточувствительная площадь испытуемого образца 1.
     */
    @FXML
    private TextField tfAreaFPU1;
    /**
     * Эквивалентная шумовуая полоса пропускания.
     */
    @FXML
    private TextField tfFefect;
    /**
     * Действующее значение потока излучения 0.
     */
    @FXML
    private Label lab_potok0;
    /**
     * Действующее значение потока излучения 1.
     */
    @FXML
    private Label lab_potok1;
    /**
     * Облученность 0
     */
    @FXML
    private Label lab_exposure0;
    /**
     * Облученность 1
     */
    @FXML
    private Label lab_exposure1;

    /**
     * Итоговый поток излучения.
     */
    @FXML
    private Label lab_potok;
    /**
     * Итоговая облученность.
     */
    @FXML
    private Label lab_exposure;

    /////////////////////////////////////////ВРЕМЕННО///////////////////////////////////////////////
    /**
     * Кнопка загрузки прошивки
     */
    @FXML
    private Button btnLoad;
    /**
     * Окно деления посылки
     */
    @FXML
    private TextField lb_partSize;
    /////////////////////////////////////////ВРЕМЕННО///////////////////////////////////////////////

    /**
     * Кнопка закрытия окна.
     */
    @FXML
    private Button btnClose;
    /**
     * Кнопка старта эксперимента.
     */
    @FXML
    private Button btnStart;

    /**
     * Кнопка сброса эксперимента
     */
    @FXML
    private Button btnReset;
    /**
     * Текстовое поле прогрессбара.
     */
    @FXML
    private Label lab_status;
    /**
     * Прогрессбар.
     */
    @FXML
    private ProgressBar pb_status;

    //размерности
    @FXML
    private Label ib_N;
    @FXML
    private Label ib_T;
    @FXML
    private Label ib_Sig;
    @FXML
    private Label ib_Eps;
    @FXML
    private Label ib_D;
    @FXML
    private Label ib_L;
    @FXML
    private Label ib_Betta;
    @FXML
    private Label ib_S;
    @FXML
    private Label ib_f;
    @FXML
    private Label ib_Fe;
    @FXML
    private Label ib_Ee;
    @FXML
    private Label ib_Fe1;
    @FXML
    private Label ib_Ee1;
    ////размерности
    @FXML
    private ToggleButton tb_Duks;
    //////////// Вторая вкладка
    @FXML
    private TextField tfZakaz;
    @FXML
    private TextField tfDogovor;
    @FXML
    private TextField tfMetodika;
    @FXML
    private TextField tfNomer_0;
    @FXML
    private TextField tfNomer;
    @FXML
    private TextField tfCopy;
    @FXML
    private TextField tfOtk;
    @FXML
    private TextField tfData;
    @FXML
    private TextField TXT_0_0;
    @FXML
    private TextField TXT_0_1;
    @FXML
    private TextField TXT_0_2;
    @FXML
    private TextField TXT_0_3;
    @FXML
    private TextField TXT_0_4;
    @FXML
    private TextField TXT_0_5;
    @FXML
    private TextField TXT_0_6;
    @FXML
    private TextField TXT_0_7;
    @FXML
    private TextField TXT_0_8;
    @FXML
    private TextField TXT_0_9;
    @FXML
    private Label tx1;
    @FXML
    private Label tx2;
    @FXML
    private Label tx3;
    @FXML
    private Label tx4;
    @FXML
    private Label tx5;
    @FXML
    private Label tx6;
    @FXML
    private TextField tfComPort;
    @FXML
    private TextField tfVideoPort;
    @FXML
    private Label lbPorts;
    @FXML
    private Label lbSlath;

    @FXML
    private Label lbShowRestart;

    /**
     * Сервис расчета потока.
     */
    private PotokService service;
    /**
     * Ссылка на главный контроллер.
     */
    private Controller mainController;

    private ObservableList<TextField> fieldOptions = FXCollections.observableArrayList();

    private boolean isFieldsValid = false;

    private ArrayList<String> values = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        LOG.debug("Init potok controller");

        fillOptions();
        resetButtons();
        fillDimensions();
        addButtonOnAction();

        disableFields();
        showNeedCalc(true);
    }

    /**
     * Блокировка полей
     */
    private void disableFields() {

        fieldOptions.get(3).setDisable(true);
        fieldOptions.get(4).setDisable(true);
        fieldOptions.get(6).setDisable(true);
        fieldOptions.get(14).setDisable(true);
        fieldOptions.get(15).setDisable(true);

    }

    /**
     * Создание списка полей
     */
    private void fillOptions() {
        fieldOptions.addAll(tfFrameCount,
                tfTemp0, tfTemp1,
                tfPlank0, tfPlank1,
                tfEps0, tfEps1,
                tfAreaACHT0, tfAreaACHT1,
                tfRasst0, tfRasst1,
                tfBetta0, tfBetta1,
                tfAreaFPU0, tfAreaFPU1,
                tfFefect);
    }

    /**
     * Проверка значения. Подкрашивание поля
     *
     * @param uzel поле
     * @return true в случае валидного значения
     */
    private boolean proverkaZnacheniy(TextField uzel) {
        boolean isValid = false;
        String text = uzel.getText().trim().toUpperCase();
        String replacedText = text.trim().replace(",", ".");
        if (!text.equals(replacedText)) {
            uzel.setText(replacedText);
            text = replacedText;
        }
        try {
            if (uzel.getId().equals("tfFrameCount")) {
                int value = Integer.parseInt(text);
                if (value > 1) {
                    isValid = true;
                }
            } else {
                double value = Double.parseDouble(text);
                if (value > 0) {
                    isValid = true;
                }
            }

        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {

            if (isValid) {
                String tempText = uzel.getText().toUpperCase();
                uzel.setText(tempText);
                uzel.setStyle("-fx-padding: 2;" +
                        "-fx-border-color: green ;-fx-border-width: 0.5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-color: lightGreen;" +
                        "-fx-border-style: solid inside");
            } else {
                uzel.setStyle("-fx-padding: 2;" +
                        "-fx-border-color: red ;-fx-border-width: 0.5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-color: lightRed;" +
                        "-fx-border-style: solid inside");
            }

        }
        return isValid;
    }


    /**
     * Описание кнопок
     */
    private void addButtonOnAction() {
        btnClose.setOnAction(event -> {
            LOG.trace("Btn close pressed");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            saveValuesToParams();
            Button source = (Button) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        });

        /**
         * Кнопка страрт.
         */
        btnStart.setOnAction(event -> {
            LOG.trace("Btn start pressed");
            isFieldsValid = checkingFields();
            if (isFieldsValid) {
                service.restart();//Стартуем сервис
                showNeedCalc(false);
                setButtonsDisable(true, false);//блок кнопок
            }
        });

        btnReset.setOnAction(event -> {
            LOG.trace("Btn reset pressed");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            initService();
            showNeedCalc(false);
            resetItogFields();
            showNeedCalc(true);
            setButtonsDisable(false, true);//блок кнопок
        });

        /////временно////////
        btnLoad.setOnAction(event ->
        {
            LOG.debug("Btn load pressed");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            loadFile(event);
        });
        ////временно/////////

        tb_Duks.selectedProperty().addListener((observable, oldValue, newValue) -> autoConfig(newValue));

    }

    private void autoConfig(boolean b) {
        fieldOptions.get(1).setText(b ? "300.0" : "25.0");
        fieldOptions.get(2).setText(b ? "300.0" : "240.0");
        fieldOptions.get(5).setText(b ? "1.0" : "0.95");
        fieldOptions.get(6).setText(b ? "1.0" : "0.95");
        fieldOptions.get(7).setText(b ? "0.390" : "5.0");
        fieldOptions.get(8).setText(b ? "0.448" : "5.0");
        fieldOptions.get(9).setText(b ? "1857.2" : "500.0");
        fieldOptions.get(10).setText(b ? "1857.2" : "500.0");
        fieldOptions.get(11).setText(b ? "0.1125" : "0.325");
        fieldOptions.get(12).setText(b ? "0.1136" : "0.325");
        fieldOptions.get(13).setText(b ? "900" : "900");//todo рассчитать размер линзы и коэф. увеличения
        fieldOptions.get(14).setText(b ? "900" : "900");
        resetItogFields();
    }


    public void saveValuesToParams() {
        int i = 0;
        if (isFieldsValid) {
            mainController.getParams().setCountFrames(Integer.parseInt(values.get(i++)));

            mainController.getParams().setTemp0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setTemp1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setPlank0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setPlank1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setEpsilin0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setEpsilin1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setAreaACHT0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setAreaACHT1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setRasstACHTfpu0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setRasstACHTfpu1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setBetta0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setBetta1(Double.parseDouble(values.get(i++)));
            mainController.getParams().setAreaFPU0(Double.parseDouble(values.get(i++)));
            mainController.getParams().setAreaFPU1(Double.parseDouble(values.get(i++)));

            mainController.getParams().setfEfect(Double.parseDouble(values.get(i++)));
        }
    }

    private void resetItogFields() {
        Platform.runLater(() -> {
            lab_potok0.setText("--");
            lab_potok1.setText("--");
            lab_potok.setText("--");
            lab_exposure.setText("--");
            lab_exposure0.setText("--");
            lab_exposure1.setText("--");
        });
    }

    /**
     * Заполнение полей размерностей
     */
    private void fillDimensions() {
        ib_N.setText("кадр");
        ib_T.setText("\u2103");
        ib_Sig.setText("Вт\u00B7м\u00AF \u00B2\u00B7К\u00AF \u2074");
        ib_Eps.setText("отн. ед.");
        ib_D.setText("мм");
        ib_L.setText("мм");
        ib_Betta.setText("отн. ед.");
        ib_S.setText("мкм\u00B2");
        ib_f.setText("Гц");
        ib_Fe.setText("Вт");
        ib_Ee.setText("Вт\u00B7см\u00AF \u00B2");
        ib_Fe1.setText("Вт");
        ib_Ee1.setText("Вт\u00B7см\u00AF \u00B2");
    }

    /**
     * Показ панели ethernet, вслучае работы по сети.
     */
    private void showEthernet() {
        ArrayList<Parent> controls = new ArrayList<>();
        controls.add(tfComPort);
        controls.add(tfVideoPort);
        controls.add(lbPorts);
        controls.add(lbSlath);
        controls.add(btnLoad);
        controls.add(lb_partSize);
        String netName = mainController.getParams().getSelNetworkInterface().getName();
        for (Parent c :
                controls) {
            if (netName.startsWith("USB")) {
                c.setVisible(false);
            } else {
                c.setVisible(true);
            }
        }
    }

    /**
     * Блокировка кнопок.
     *
     * @param startBut кнопка старт.
     * @param resetBut кнопка ресет.
     */
    public void setButtonsDisable(boolean startBut, boolean resetBut) {
        btnStart.setDisable(startBut);
        btnReset.setDisable(resetBut);
    }

    /**
     * Сброс блокировки кнопок.
     */
    public void resetButtons() {
        setButtonsDisable(false, true);
    }

    /**
     * Инициализация сервиса.
     */
    public void initService() {
        service = new PotokService(this);
        mainController.getPb_exp().visibleProperty().bind(pb_status.visibleProperty());
        mainController.getPb_exp().progressProperty().bind(pb_status.progressProperty());
        mainController.getLab_exp_status().textProperty().bind(lab_status.textProperty());

        pb_status.visibleProperty().bind(service.runningProperty());
        pb_status.progressProperty().bind(service.progressProperty());
        lab_status.textProperty().bind(service.messageProperty());

        for (TextField field : fieldOptions) {
            field.setStyle(null);
        }

    }

    public void initController(Controller controller) {
        LOG.debug("Init controller");

        mainController = controller;
        initService();
        showEthernet();
        fillToolTips();

        tfEps1.textProperty().bindBidirectional(tfEps0.textProperty());
        tfAreaFPU1.textProperty().bindBidirectional(tfAreaFPU0.textProperty());

        //////вторая закладка+//////
        tfZakaz.textProperty().bindBidirectional(controller.getParams().zakazProperty());
        tfDogovor.textProperty().bindBidirectional(controller.getParams().dogovorProperty());
        tfMetodika.textProperty().bindBidirectional(controller.getParams().metodikaProperty());
        tfNomer_0.textProperty().bindBidirectional(controller.getParams().nomer_0Property());
        tfNomer.textProperty().bindBidirectional(controller.getParams().nomerProperty());
        tfCopy.textProperty().bindBidirectional(controller.getParams().copyProperty());
        tfOtk.textProperty().bindBidirectional(controller.getParams().otkProperty());

        tfData.textProperty().bindBidirectional(controller.getParams().dataProperty());
        String dataWord = constructDataWord(controller);
        tfData.setText(dataWord);

        TXT_0_0.textProperty().bindBidirectional(controller.getParams().TXT_0_0Property());
        TXT_0_1.textProperty().bindBidirectional(controller.getParams().TXT_0_1Property());
        TXT_0_2.textProperty().bindBidirectional(controller.getParams().TXT_0_2Property());
        TXT_0_3.textProperty().bindBidirectional(controller.getParams().TXT_0_3Property());
        TXT_0_4.textProperty().bindBidirectional(controller.getParams().TXT_0_4Property());
        TXT_0_5.textProperty().bindBidirectional(controller.getParams().TXT_0_5Property());
        TXT_0_6.textProperty().bindBidirectional(controller.getParams().TXT_0_6Property());
        TXT_0_7.textProperty().bindBidirectional(controller.getParams().TXT_0_7Property());
        TXT_0_8.textProperty().bindBidirectional(controller.getParams().TXT_0_8Property());
        TXT_0_9.textProperty().bindBidirectional(controller.getParams().TXT_0_9Property());
        tbExel.selectedProperty().bindBidirectional(controller.getParams().tbExelProperty());
        tbTxt.selectedProperty().bindBidirectional(controller.getParams().tbTxtProperty());
        tbPdf.selectedProperty().bindBidirectional(controller.getParams().tbPdfProperty());
        tfComPort.setText(String.valueOf(controller.getParams().getDetPortCommand()));
        tfVideoPort.setText(String.valueOf(controller.getParams().getDetPortVideo()));

        //первая заклаадка
        tfFrameCount.setText(String.valueOf(controller.getParams().getCountFrames()));
        tfTemp0.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getTemp0()).toUpperCase());
        tfTemp1.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getTemp1()).toUpperCase());

        tfAreaACHT0.setText(String.format(Locale.CANADA, "%.3f", controller.getParams().getAreaACHT0()).toUpperCase());
        tfAreaACHT1.setText(String.format(Locale.CANADA, "%.3f", controller.getParams().getAreaACHT1()).toUpperCase());
        tfAreaFPU0.setText(String.format(Locale.CANADA, "%.0f", controller.getParams().getAreaFPU0()).toUpperCase());
        tfAreaFPU1.setText(String.format(Locale.CANADA, "%.0f", controller.getParams().getAreaFPU1()).toUpperCase());
        tfRasst0.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getRasstACHTfpu0()).toUpperCase());
        tfRasst1.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getRasstACHTfpu1()).toUpperCase());
        tfEps0.setText(String.format(Locale.CANADA, "%.2f", controller.getParams().getEpsilin0()).toUpperCase());
        tfEps1.setText(String.format(Locale.CANADA, "%.2f", controller.getParams().getEpsilin1()).toUpperCase());
        tfPlank0.setText(String.format(Locale.CANADA, "%.2e", controller.getParams().getPlank0()).toUpperCase());
        tfPlank1.setText(String.format(Locale.CANADA, "%.2e", controller.getParams().getPlank1()).toUpperCase());
        tfBetta0.setText(String.format(Locale.CANADA, "%.4f", controller.getParams().getBetta0()).toUpperCase());
        tfBetta1.setText(String.format(Locale.CANADA, "%.4f", controller.getParams().getBetta1()).toUpperCase());

        tfFefect.setText(String.format(Locale.CANADA, "%.2e",
                1.0 / ((1.0E-06) * (2.0) * (controller.getParams().getTempInt()))).toUpperCase());

        /////первая закладка


        String str = "Вольтовая чувствительность, В\u00B7Вт\u00AF \u00B9";
        tx1.setText(str);
        //точка   1/2
        str = "Порог чувствительности, Вт\u00B7Гц-\u00BD";
        tx2.setText(str);
        //точка  1/2  точка   минус  первая
        str = "Удельный порог чувствительности, Вт\u00B7Гц-\u00BD\u00B7см\u00AF \u00B9";
        tx3.setText(str);

        str = "Обнаружительная способность, Вт\u00AF \u00B9\u00B7Гц\u00BD";
        tx4.setText(str);

        str = "Удельная обнаруж. способность, Вт\u00AF \u00B9\u00B7Гц\u00BD\u00B7см";
        tx5.setText(str);

        str = "Пороговая облученность, Вт\u00B7см\u00AF \u00B2";
        tx6.setText(str);
    }

    /**
     * Заполнение подсказок
     */
    private void fillToolTips() {
        int i = 0;
        fieldOptions.get(i++).setTooltip(new Tooltip("Количество отсчетов (кадров) в эксперименте"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Температура источника излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Температура источника излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Постоянная Стефана-Больцмана"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Постоянная Стефана-Больцмана"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Коэффициент черноты"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Коэффициент черноты"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Диаметр диафрагмы источника излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Диаметр диафрагмы источника излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Расстояние между диафрагмой излучателя" +
                " и плоскостью фоточувствительного элемента испытуемого образца"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Расстояние между диафрагмой излучателя" +
                " и плоскостью фоточувствительного элемента испытуемого образца"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Коэффициент, учитывающий потери ИК излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Коэффициент, учитывающий потери ИК излучения"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Площадь фоточувствительного элемента матрицы"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Площадь фоточувствительного элемента матрицы"));
        fieldOptions.get(i++).setTooltip(new Tooltip("Эквивалентная шумовая полоса пропускания"));
        lab_potok0.setTooltip(new Tooltip("Действующее значение потока излучения"));
        lab_potok1.setTooltip(new Tooltip("Действующее значение потока излучения"));
        lab_potok.setTooltip(new Tooltip("Итоговый поток излучения"));
        lab_exposure0.setTooltip(new Tooltip("Действующее значение облученности"));
        lab_exposure1.setTooltip(new Tooltip("Действующее значение облученности"));
        lab_exposure.setTooltip(new Tooltip("Итоговая облученность"));


    }

    /**
     * Проверка полей и выдача разрешения на запуск сервиса
     *
     * @return разрешение
     */
    public boolean checkingFields() {

        for (TextField field : fieldOptions) {
            if (!proverkaZnacheniy(field)) {
                LOG.error("TextField error {}", field.getId());
                return false;
            } else {
                values.add(field.getText());
            }
        }
        return true;
    }

    /**
     * Заполнение командного слова DataWord
     *
     * @return 2 байта
     */
    private String constructDataWord(Controller controller) {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //|Start  |Mode   |GC   | –  |PW(1-0)|I(2-0) |DE(6-0)     |TS(7-0)          |RO(2-0)    |OM1    | – | – |RST|OE |//
        //| 1     | 0     |ку   | 0  | 1 1   | 0 0 0 |смещение    | 0 0 0 0 0 0 0 0 | 0 0 0     | 0     | 0 | 0 | 0 | 1 |//
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        byte[] defValue = new byte[]{(byte) 0x8C, 0x00, 0x00, 0x01};
        Bytes dataWord = Bytes.wrap(defValue, ByteOrder.BIG_ENDIAN);
        BitSet bitSet = dataWord.toBitSet();
        //коэфициент смещения
        if (controller.getParams().isTempKU()) {
            bitSet.set(5, true);//1- если ку 3
        }
        defValue = bitSet.toByteArray();

        //VR0
        byte vR0 = (byte) controller.getParams().getTempVR0();
        defValue[1] = vR0;

        return "0x" + Bytes.wrap(defValue).encodeHex(true);
    }

    public Controller getMainController() {
        return mainController;
    }

    /**
     * По нажатию ентера выделение текста и запуск сервиса
     *
     * @param event
     */
    @FXML
    private void startServiceOnTap(ActionEvent event) {
        TextField source = (TextField) event.getSource();
        source.selectAll();
        btnStart.fire();
    }

    /**
     * Установка IP.
     *
     * @param event
     */
    private void setIP(ActionEvent event) {
        TextField source = (TextField) event.getSource();
        String text = source.getText().trim();
        source.getParent().requestFocus();
        boolean b = ipv4Check(text);
        if (b) {
            mainController.getParams().setDetIP(text);
            showRestart();
        } else {
            LOG.error("IP not match");
            setError(source, "Error");
        }
    }

    /**
     * Установка командного порта.
     *
     * @param event
     */
    @FXML
    private void setComPort(ActionEvent event) {

        TextField source = (TextField) event.getSource();
        int i = parseIntText(event, false);
        if (0 < i && i < 64000) {
            mainController.getParams().setDetPortCommand(i);
            showRestart();
        } else {
            LOG.error("Command port not match");
            setError(source, "Error");
        }
    }

    /**
     * Установка видео порта.
     *
     * @param event
     */
    @FXML
    private void setVideoPort(ActionEvent event) {
        TextField source = (TextField) event.getSource();
        int i = parseIntText(event, false);
        if (0 < i && i < 64000) {
            mainController.getParams().setDetPortVideo(i);
            showRestart();
        } else {
            LOG.error("Video port not match");
            setError(source, "Error");
        }
    }

    /**
     * Показ подсказки по необходимости рестарта.
     */
    private void showRestart() {
        lbShowRestart.setVisible(true);
    }

    /**
     * Отображение необходимости перерасчета.
     *
     * @param b true-отобразить, false -убрать отображение.
     */
    private void showNeedCalc(boolean b) {
        if (b) {
            needCalkTask(true);
        } else {
            needCalkTask(false);
        }
    }

    /**
     * Задание на мигание.
     */
    static private TimerTask timerTask;
    /**
     * Сам таймер
     */
    static private Timer tm;
    /**
     * Отображение мигания.
     */
    static private boolean needCalkShow = false;

    /**
     * Задание на мигание
     *
     * @param b
     */
    private void needCalkTask(boolean b) {
        if (b && !needCalkShow) {
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
            needCalkShow = true;
        } else if (!b && needCalkShow) {
            tm.cancel();
            tm = null;
            timerTask = null;
            Platform.runLater(() -> {
                btnStart.setText("Рассчитано");
            });
            needCalkShow = false;
        }
    }


    public Label getLab_potok0() {
        return lab_potok0;
    }

    public Label getLab_potok1() {
        return lab_potok1;
    }

    public Label getLab_potok() {
        return lab_potok;
    }

    public Label getLab_exposure0() {
        return lab_exposure0;
    }

    public Label getLab_exposure1() {
        return lab_exposure1;
    }

    public Label getLab_exposure() {
        return lab_exposure;
    }


    public ObservableList<TextField> getFieldOptions() {
        return fieldOptions;
    }

    private void loadFile(ActionEvent event) {

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выбрать файл для загрузки");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("BIN", "*.bin"),
                new FileChooser.ExtensionFilter("FS", "*.fs"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File loadFile = fileChooser.showOpenDialog(stage);

        if (loadFile != null) {

            try {
                byte[] tempByteData = loadFileFromDisk(loadFile.getAbsolutePath());


                sendDataArray(tempByteData, pb_status, lab_status);

            } catch (IOException e) {
                LOG.error(e.getMessage());
                throw new DetectorException(e);
            }
        }
    }

    /**
     * Отправка массива данных
     *
     * @param tempByteData сам файл
     * @param pb_status    ссылка на прогресс бар
     * @param lab_status   ссылка на текстовое поле
     */
    private void sendDataArray(byte[] tempByteData, ProgressBar pb_status, Label lab_status) {

        //стоп прослушка кадров
        if (!mainController.isPaused()) {
            mainController.stopDetector(null);
        }
        String partS = lb_partSize.textProperty().get();

        int partSize = 0;
        try {
            partSize = Integer.parseInt(partS);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        if (partSize <= 0) {
            partSize = 1024;
        }
        int finalPartSize = partSize;


        Platform.runLater(() -> {
            lb_partSize.setText(String.valueOf(finalPartSize));
            btnLoad.setStyle(null);
            btnLoad.setText("Загрузка..");
        });


        Thread thread = new Thread(() -> {
            mainController.getPb_exp().visibleProperty().unbind();
            mainController.getPb_exp().progressProperty().unbind();
            mainController.getLab_exp_status().textProperty().unbind();

            pb_status.visibleProperty().unbind();
            pb_status.progressProperty().unbind();
            lab_status.textProperty().unbind();


            int allLength = tempByteData.length;
            int length = 0;
            int parts = 0;

            ByteArrayInputStream wrappedData = new ByteArrayInputStream(tempByteData);

            FT_STATUS ft_status = null;

            setStatus(true, "Старт отправки драйвера...", 0.0);

            boolean start = true;//статус отправки первого пакета
            while (wrappedData.available() > 0) {

                int size = wrappedData.available() < finalPartSize ? wrappedData.available() : finalPartSize;

                byte[] buff = new byte[size];
                int read = wrappedData.read(buff, 0, size);

                length = length + read;
                parts++;
                //  LOG.debug("Trying to send Array... {} bytes. Msg #{}", size, parts);
                ft_status = ((DetectorDevice.ChinaSource) mainController.getSelDetector().getDevice()).setID(buff, allLength, start);
                //    LOG.debug("MSG # {} sended. Status: {}", parts, ft_status);
                if (ft_status != FT_STATUS.FT_OK) {
                    setStatus(true, "Нет ответа на пакет № " + parts + ". Отправка прервана.", (1.0 * length) / allLength);
                    wrappedData.readAllBytes();
                } else {
                    setStatus(true, "Отправка пакета № " + parts, (1.0 * length) / allLength);
                }
                start = false;
            }

            //       LOG.debug("Send Array Finished. {} bytes, {} msges", length, parts);
            if (ft_status == FT_STATUS.FT_OK) {
                setStatus(true, "Отправлено " + parts + " пакетов. " + allLength + " байт.", (1.0 * length) / allLength);
            }

            FT_STATUS finalFt_status = ft_status;

            Platform.runLater(() -> {
                String txt = "";
                String style = "";
                switch (finalFt_status) {
                    case FT_OK:

                        mainController.getPb_exp().visibleProperty().bind(pb_status.visibleProperty());
                        mainController.getPb_exp().progressProperty().bind(pb_status.progressProperty());
                        mainController.getLab_exp_status().textProperty().bind(lab_status.textProperty());

                        pb_status.visibleProperty().bind(service.runningProperty());
                        pb_status.progressProperty().bind(service.progressProperty());
                        lab_status.textProperty().bind(service.messageProperty());


                        txt = "Загружено";
                        style = "-fx-background-color: green";
                        break;
                    case FT_BUSY:
                        txt = "Ошибка";
                        style = "-fx-background-color: red";
                        break;
                }

                btnLoad.setText(txt);
                btnLoad.setStyle(style);

                if (mainController.isPaused()) {
                    mainController.stopDetector(null);
                }
            });

        });

        thread.setDaemon(true);
        thread.start();

    }

    private void setStatus(boolean isVisible, String msg, double persent) {
        Platform.runLater(() -> {
            pb_status.setVisible(isVisible);
            lab_status.setVisible(isVisible);
            pb_status.progressProperty().set(persent);
            lab_status.setText(msg);
        });
    }


}
