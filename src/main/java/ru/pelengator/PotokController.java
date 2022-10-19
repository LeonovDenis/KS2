package ru.pelengator;

import at.favre.lib.bytes.Bytes;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.service.PotokService;

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
     * Итоговый поток излучения.
     */
    @FXML
    private Label lab_potok;
    /**
     * Итоговая облученность.
     */
    @FXML
    private Label lab_exposure;
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
    /**
     * Сервис расчета потока.
     */
    private PotokService service;
    /**
     * Ссылка на главный контроллер.
     */
    private Controller mainController;
    /**
     * Постоянная Больцмана.
     */
    @FXML
    private Label lb_bolts;
    @FXML
    private Label lb_areaACHT;
    @FXML
    private Label lb_areaFPU;
    @FXML
    private Label lb_areaExp;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.trace("Init potok controller");
        resetButtons();

        String str = "Постоянная Стефана-Больцмана, Вт\u00B7м\u00AF \u00B2\u00B7К\u00AF \u2074";
        lb_bolts.setText(str);

        str = "Площадь отверстия диафрагмы АЧТ, м\u00B2";
        lb_areaACHT.setText(str);

        str = "Эффективная фоточувствительная площадь испытуемого образца, м\u00B2";
        lb_areaFPU.setText(str);

        str = "Итоговая облученность, Вт\u00B7см\u00AF \u00B2";
        lb_areaExp.setText(str);

        btnClose.setOnAction(event -> {
            LOG.trace("Btn close pressed");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            Button source = (Button) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        });
        showNeedCalc(true);

        /**
         * Кнопка страрт.
         */
        btnStart.setOnAction(event -> {
            LOG.trace("Btn start pressed");
            service.restart();//Стартуем сервис
            showNeedCalc(false);
            setButtonsDisable(true, false);//блок кнопок
        });

        btnReset.setOnAction(event -> {
            LOG.trace("Btn reset pressed");
            if (service.getState() == Worker.State.RUNNING) {
                service.cancel();
            }
            initService();
            showNeedCalc(false);
            Platform.runLater(() -> {
                lab_potok0.setText("--");
                lab_potok1.setText("--");
                lab_potok.setText("--");
                lab_exposure.setText("--");
            });
            showNeedCalc(true);
            setButtonsDisable(false, true);//блок кнопок
        });
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
    }

    public void initController(Controller controller) {
        LOG.trace("Init controller");
        mainController = controller;
        initService();
        showEthernet();

        tfZakaz.textProperty().bindBidirectional(controller.getParams().zakazProperty());
        tfDogovor.textProperty().bindBidirectional(controller.getParams().dogovorProperty());
        tfMetodika.textProperty().bindBidirectional(controller.getParams().metodikaProperty());
        tfNomer_0.textProperty().bindBidirectional(controller.getParams().nomer_0Property());
        tfNomer.textProperty().bindBidirectional(controller.getParams().nomerProperty());
        tfCopy.textProperty().bindBidirectional(controller.getParams().copyProperty());
        tfOtk.textProperty().bindBidirectional(controller.getParams().otkProperty());

        tfData.textProperty().bindBidirectional(controller.getParams().dataProperty());
        String dataWord=constructDataWord(controller);
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
        tfFrameCount.setText(String.valueOf(controller.getParams().getCountFrames()));
        tfTemp0.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getTemp0()).toUpperCase());
        tfTemp1.setText(String.format(Locale.CANADA, "%.1f", controller.getParams().getTemp1()).toUpperCase());
        tfAreaACHT0.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getAreaACHT0()).toUpperCase());
        tfAreaACHT1.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getAreaACHT1()).toUpperCase());
        tfAreaFPU0.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getAreaFPU0()).toUpperCase());
        tfAreaFPU1.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getAreaFPU1()).toUpperCase());
        tfRasst0.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getRasstACHTfpu0()).toUpperCase());
        tfRasst1.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getRasstACHTfpu1()).toUpperCase());
        tfEps0.setText(String.format(Locale.CANADA, "%.3f", controller.getParams().getEpsilin0()).toUpperCase());
        tfEps1.setText(String.format(Locale.CANADA, "%.3f", controller.getParams().getEpsilin1()).toUpperCase());
        tfPlank0.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getPlank0()).toUpperCase());
        tfPlank1.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getPlank1()).toUpperCase());
        tfBetta0.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getBetta0()).toUpperCase());
        tfBetta1.setText(String.format(Locale.CANADA, "%.3e", controller.getParams().getBetta1()).toUpperCase());
        tfFefect.setText(String.format(Locale.CANADA, "%.3e",
                1.0 / ((1.0E-06) * (2.0) * (controller.getParams().getTempInt()))).toUpperCase());

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
     * Заполнение командного слова DataWord
     * @return 2 байта
     */
    private String constructDataWord(Controller controller) {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //|Start  |Mode   |GC   | –  |PW(1-0)|I(2-0) |DE(6-0)     |TS(7-0)          |RO(2-0)    |OM1    | – | – |RST|OE |//
        //| 1     | 0     |ку   | 0  | 1 1   | 0 0 0 |смещение    | 0 0 0 0 0 0 0 0 | 0 0 0     | 0     | 0 | 0 | 0 | 1 |//
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        byte[]defValue=new byte[]{(byte) 0x8C,0x00,0x00,0x01};
        Bytes dataWord = Bytes.wrap(defValue, ByteOrder.BIG_ENDIAN);
        BitSet bitSet = dataWord.toBitSet();
        //коэфициент смещения
        if(controller.getParams().isTempKU()){
           bitSet.set(5,true);//1- если ку 3
        }
        defValue = bitSet.toByteArray();

        //VR0
        byte vR0 =(byte) controller.getParams().getTempVR0();
        defValue[1]=vR0;

        return "0x"+Bytes.wrap(defValue).encodeHex(true);
    }

    public Controller getMainController() {
        return mainController;
    }

    /**
     * Установка эффективной частоты.
     *
     * @param event
     */
    @FXML
    private void setFefect(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setfEfect(d);
        showNeedCalc(true);
    }

    /**
     * Установка числа отсчетов.
     *
     * @param event
     */
    @FXML
    private void setCountFrames(ActionEvent event) {
        int i = parseIntText(event, false);
        mainController.setCountFrames(event);
        showNeedCalc(true);
    }

    /**
     * Установка температуры ачт 0.
     *
     * @param event
     */
    @FXML
    private void setTemp0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setTemp0(d);
        showNeedCalc(true);
    }

    /**
     * Установка температуры ачт 1.
     *
     * @param event
     */
    @FXML
    private void setTemp1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setTemp1(d);
        showNeedCalc(true);
    }

    /**
     * Установка планка 0.
     *
     * @param event
     */
    @FXML
    private void setPlank0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setPlank0(d);
        showNeedCalc(true);
    }

    /**
     * Установка планка 1.
     *
     * @param event
     */
    @FXML
    private void setPlank1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setPlank1(d);
        showNeedCalc(true);
    }

    /**
     * Установка коэф излучения 0.
     *
     * @param event
     */
    @FXML
    private void setEps0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setEpsilin0(d);
        showNeedCalc(true);
    }

    /**
     * Установка коэф излучения 1.
     *
     * @param event
     */
    @FXML
    private void setEps1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setEpsilin1(d);
        showNeedCalc(true);
    }

    /**
     * Установка площади диафр 0.
     *
     * @param event
     */
    @FXML
    private void setAreaACHT0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setAreaACHT0(d);
        showNeedCalc(true);
    }

    /**
     * Установка площади диафр 1.
     *
     * @param event
     */
    @FXML
    private void setAreaACHT1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setAreaACHT1(d);
        showNeedCalc(true);
    }

    /**
     * Установка расстояния 0.
     *
     * @param event
     */
    @FXML
    private void setRasst0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setRasstACHTfpu0(d);
        showNeedCalc(true);
    }

    /**
     * Установка расстояния 1.
     *
     * @param event
     */
    @FXML
    private void setRasst1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setRasstACHTfpu1(d);
        showNeedCalc(true);
    }

    /**
     * Установка коэф. поправки 0.
     *
     * @param event
     */
    @FXML
    private void setBetta0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setBetta0(d);
        showNeedCalc(true);
    }

    /**
     * Установка коэф. поправки 1.
     *
     * @param event
     */
    @FXML
    private void setBetta1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setBetta1(d);
        showNeedCalc(true);
    }

    /**
     * Установка площади фпу 0.
     *
     * @param event
     */
    @FXML
    private void setAreaFPU0(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setAreaFPU0(d);
        showNeedCalc(true);
    }

    /**
     * Установка площади фпу 1.
     *
     * @param event
     */
    @FXML
    private void setAreaFPU1(ActionEvent event) {
        double d = parseDoubleText(event);
        mainController.getParams().setAreaFPU1(d);
        showNeedCalc(true);
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

    public Label getLab_exposure() {
        return lab_exposure;
    }
}
