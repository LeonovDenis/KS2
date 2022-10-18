package ru.pelengator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.decimal4j.util.DoubleRounder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.*;
import ru.pelengator.API.devises.china.ChinaDriver;
import ru.pelengator.API.devises.china.ChinaDriverEthernet;
import ru.pelengator.API.driver.FT_STATUS;
import ru.pelengator.API.transformer.MyChinaGrayTramsformer;
import ru.pelengator.API.transformer.MyChinaRgbImageTransformer;
import ru.pelengator.API.transformer.comFilters.JHFlipFilter;
import ru.pelengator.API.transformer.comFilters.JHNormalizeFilter;
import ru.pelengator.model.NetworkInfo;
import ru.pelengator.model.DetectorInfo;
import ru.pelengator.model.ExpInfo;
import ru.pelengator.model.StatData;
import ru.pelengator.model.StendParams;
import ru.pelengator.service.DataService;


import static ru.pelengator.API.transformer.comFilters.JHFlipFilter.*;
import static ru.pelengator.API.utils.Utils.*;
import static ru.pelengator.API.driver.ethernet.NetUtils.findInterfaces;

public class Controller implements Initializable, DetectorDiscoveryListener {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

    /**
     * Чекбокс на центральную зону.
     */
    @FXML
    private CheckBox cbKvadrat;
    /**
     * Размер центральной зоны по ширине.
     */
    @FXML
    private TextField tfKvadratWidth;
    /**
     * Размер центральной зоны по высоте.
     */
    @FXML
    private TextField tfKvadratHeight;


    /**
     * Скролпейн.
     */
    @FXML
    private ScrollPane myPane;
    /**
     * Сценарий остановки картинки.
     */
    @FXML
    private Button btnStartStop;
    /**
     * Кнопка расчета потока от источника света.
     */
    @FXML
    private Button btnPotok;
    /**
     * Кнопка сбора кадров.
     */
    @FXML
    private Button btnGetData;
    /**
     * Кнопка расчета параметров.
     */
    @FXML
    private Button btnParams;
    /**
     * Список подключенных детекторов.
     */
    @FXML
    private ComboBox<DetectorInfo> cbDetectorOptions;
    /**
     * Комбобокс выбора сетевого драйвера.
     */
    @FXML
    private ComboBox<NetworkInfo> cbNetworkOptions;
    /**
     * Центральное окно с картинкой.
     */
    @FXML
    private BorderPane bpDetectorPaneHolder;
    /**
     * Панель ско.
     */
    @FXML
    private VBox pnFlash;
    /**
     * Поле вывода картинки.
     */
    @FXML
    private SwingNode snDetectorCapturedImage;
    /**
     * Панель, обслуживающая детектор.
     */
    private DetectorPanel detectorPanel;
    /**
     * Кнопка питания.
     */
    @FXML
    private CheckBox chPower;
    /**
     * Поле инта.
     */
    @FXML
    private TextField tfInt;
    /**
     * Поле VOS.
     */
    @FXML
    private TextField tfVOS;
    /**
     * Поле VR0.
     */
    @FXML
    private TextField tfVR0;
    /**
     * Доступные разрешения.
     */
    @FXML
    private ComboBox<String> cbDimOptions;
    /**
     * Доступные усиления.
     */
    @FXML
    private ComboBox<String> cbCCCOptions;
    /**
     * Задержка опроса платы.
     */
    @FXML
    private TextField tfSpeedPlata;
    /**
     * Поле текущего FPS.
     */
    @FXML
    private TextField tfFPS;
    /**
     * Панелька гистограммы.
     */
    @FXML
    private Pane pnGist;
    /**
     * Среднее значение отклонения сигнала.
     */
    @FXML
    private Label lbSKO;
    /**
     * Среднее значение сигнала.
     */
    @FXML
    private Label lbAverageSignal;
    /**
     * Максимальный сигнал.
     */
    @FXML
    private Label lbMax;
    /**
     * Минимальный сигнал.
     */
    @FXML
    private Label lbMin;
    /**
     * Гистограмма верх.
     */
    @FXML
    private ImageView iwGist;
    /**
     * Полоса.
     */
    @FXML
    private ImageView ivPolosa;//w=300; h=25;
    /**
     * Гистограмма низ. Распределение по строкам.
     */
    @FXML
    private ImageView iwGistSKO_H;
    /**
     * Гистограмма низ. Распределение по столбцам.
     */
    @FXML
    private ImageView iwGistSKO_V;
    /**
     * Прогрессбар.
     */
    @FXML
    private ProgressBar pb_exp;
    /**
     * Дежурная строка.
     */
    @FXML
    private Label lab_exp_status;
    /**
     * Режим рисования DRAW_NONE
     */
    @FXML
    private ToggleButton tb_none;
    /**
     * Режим рисования DRAW_FILL
     */
    @FXML
    private ToggleButton tb_fill;
    /**
     * Режим рисования DRAW_FIT
     */
    @FXML
    private ToggleButton tb_fit;
    /**
     * Группа ддля режимов рисования.
     */
    private final ToggleGroup growModeGroup = new ToggleGroup();
    /**
     * Режим зеркалирования
     */
    @FXML
    private ToggleButton tb_mirror;
    /**
     * Режим показа отладочной информации
     */
    @FXML
    private ToggleButton tb_debug;
    /**
     * Сглаживание картинки.
     */
    @FXML
    private ToggleButton tb_antialising;
    /**
     * Поворот изображения на угол кратный 90 градусов.
     */
    @FXML
    private ChoiceBox<String> cb_flip;
    @FXML
    private ToggleButton tb_rgb;
    @FXML
    private ToggleButton tb_gray;
    @FXML
    private ToggleButton tb_norm;
    @FXML
    private Label lb_online;

    @FXML
    private Button btnLookUp;


    /**
     * Пакет ресурсов.
     */
    private final Properties properties = new Properties();
    /**
     * Текущее разрешение картинки.
     */
    private volatile Dimension viewSize;
    /**
     * Формат целого числа.
     */
    private static final String DEFAULT_FORMAT = "0";
    private static final NumberFormat FORMATTER = new DecimalFormat(DEFAULT_FORMAT);
    /**
     * Флаг остановки вывода картинки.
     */
    private boolean paused = false;
    /**
     * Масштаб оцифровки АЦП.
     */
    private static final float MASHTAB = (5000 / (float) (Math.pow(2, 14)));
    /**
     * Параметры стенда.
     */
    private final StendParams params = new StendParams(this);

    /**
     * Запись параметров при выходе.
     */
    public void save() {
        params.save();
    }

    /**
     * Картинка для гистограммы верх.
     */
    private BufferedImage grabbedImage;
    /**
     * Картинка для гистограммы низ верт.
     */
    private BufferedImage grabbedImageV;
    /**
     * Картинка для гистограммы низ гориз
     */
    private BufferedImage grabbedImageH;
    /**
     * Свойства для отображения картинки гистограммы.
     */
    private final ObjectProperty<Image> gistImageProperty = new SimpleObjectProperty<Image>();
    /**
     * Свойства для отображения картинки гистограммы низ верт.
     */
    private final ObjectProperty<Image> gistImagePropertyV = new SimpleObjectProperty<Image>();
    /**
     * Свойства для отображения картинки гистограммы низ гориз.
     */
    private final ObjectProperty<Image> gistImagePropertyH = new SimpleObjectProperty<Image>();
    /**
     * Активный детектор.
     */
    private Detector selDetector = null;
    /**
     * Активный эксперимент.
     */
    private final ExpInfo selExp = new ExpInfo();
    /**
     * Активный интерфейс драйвера сети.
     */
    private NetworkInfo selNetworkInterface = null;
    /**
     * Флаг работы.
     */
    private boolean stopVideo = false;
    /**
     * FPS по умолчанию.
     */
    private final double FPSVideo = 25;
    /**
     * Подсказка в список детекторов.
     */
    private final String detectorListPromptText = "Выбрать";
    /**
     * Подсказка в список эксп.
     */
    private final String expListPromptText = "Нет данных";
    /**
     * Подсказка в список драйверов.
     */
    private final String networkListPromptText = "Выбрать драйвер";
    /**
     * Список для меню детекторов.
     */
    private final ObservableList<DetectorInfo> options = FXCollections.observableArrayList();
    ;
    /**
     * Список для меню экспериментов.
     */
    private final ObservableList<ExpInfo> optionsExp = FXCollections.observableArrayList();
    /**
     * Список для меню драйвера.
     */
    private final ObservableList<NetworkInfo> optionsNetwork = FXCollections.observableArrayList();
    /**
     * Список коэф. усиления.
     */
    private final ObservableList<String> optionsCCC = FXCollections.observableArrayList("1", "3");
    /**
     * Список для допустимых разрешений.
     */
    private final ObservableList<String> optionsDimension = FXCollections.observableArrayList("128*128", "92*90");
    /**
     * Список допустимых поворотов картинки.
     */
    private final ObservableList<String> optionsFlip = FXCollections.observableArrayList("Нет поворота", "Поворот: +90\u00B0", "Поворот: -90\u00B0", "Поворот: 180\u00B0");

    /**
     * Счетчик экспериментов.
     */
    private static int expCounter = 0;
    /**
     * Создание списка детекторов.
     */
    private static final AtomicBoolean isImageFrash = new AtomicBoolean(false);
    private static int detectorCounter = 0;

    private final DetectorImageTransformer imageTransformer = new MyChinaRgbImageTransformer();


    private boolean async = false;
    private boolean fpsLimited = false;

    /**
     * Инициализация всего и вся.
     *
     * @param arg0
     * @param arg1
     */
    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        setDrawButton();//инициализация кнопок режимов рисования

        setAllAnotherButtons();//инициализация кнопок управления стендом

        Detector.addDiscoveryListener(this);//добавка в слушатели

        btnLookUp.setVisible(false);
        /**
         * Выключение интерфейса управления.
         */
        myPane.setVisible(false);
        cbDetectorOptions.setDisable(true);
        /**
         * Создание списка.
         */
        int interfaceCounter = 0;
        /**
         * Заполнение списка интерфейсов.
         */
        optionsNetwork.add(new NetworkInfo(interfaceCounter++));
        for (NetworkInterface networkInterface : findInterfaces().toArray(new NetworkInterface[0])) {
            NetworkInfo networkInfo = new NetworkInfo(networkInterface.getName(), networkInterface, interfaceCounter);
            optionsNetwork.add(networkInfo);
            interfaceCounter++;
        }
        /**
         * Установка списка в комбобокс.
         */
        cbNetworkOptions.setItems(optionsNetwork);
        /**
         * Установка подсказки в комбобокс.
         */
        cbNetworkOptions.setPromptText(networkListPromptText);
        /**
         * Установка подсказки в комбобокс.
         */
        cbDetectorOptions.setPromptText(detectorListPromptText);
        /**
         * Создание списка экспериментов.
         */
        /**
         * Заполнение списка экспериментов.
         */
        ExpInfo expInfo = new ExpInfo();
        expInfo.setExpIndex(expCounter);
        expInfo.setExpName("Пустой эксперимент");
        optionsExp.add(expInfo);
        expCounter++;
        /**
         * Подключение слушателя на выбор элемента из списка.
         */
        cbNetworkOptions.getSelectionModel().selectedItemProperty().addListener((arg012, arg112, newValue) -> {
            if (newValue != null) {
                if (newValue.getName().equals("USB 3.0")) {
                    /**
                     * Регистрация драйвера детектора для USB 3.0.
                     */
                    Detector.setDriver(new ChinaDriver(params));
                    async = false;

                } else {
                    /**
                     * Регистрация драйвера детектора для ethernet.
                     */
                    Detector.setDriver(new ChinaDriverEthernet(params));
                    async = false;
                    fpsLimited=true;
                    btnLookUp.setVisible(true);
                }

                /**
                 * Разблокировка списка детекторов.
                 */
                if (cbDetectorOptions.isDisabled()) {
                    cbDetectorOptions.setDisable(false);
                } else {
                    closeDetector();
                }
                /**
                 * Передача индекса интерфейса в инициализатор.
                 */
                initializeNetwork(newValue.getIndex());
                /**
                 * Заполнение списка детекторов.
                 */
                fillDetectors();
                cbNetworkOptions.setDisable(true);
            }
        });

        /**
         * Подключение слушателя на выбор элемента из списка детекторов.
         */
        cbDetectorOptions.getSelectionModel().selectedItemProperty().addListener((arg01, arg11, newValue) -> {
            if (newValue != null) {
                String detectorName = newValue.getDetectorName();
                LOG.trace("Detector Index: " + newValue.getDetectorIndex() + ": Detector Name: " + detectorName + " choosed");
                /**
                 * Передача индекса детектора в инициализатор.
                 */
                saveDetIp(detectorName);
                btnLookUp.setVisible(false);
                initializeDetector(newValue.getDetectorIndex());
                if (!myPane.isVisible()) {
                    myPane.setVisible(true);
                }
            }
        });

        cbKvadrat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (detectorPanel != null) {
                detectorPanel.setAimDisplayed(newValue);
            }
        });


        /**
         * Подгонка размеров окна отображения картинки.
         */
        Platform.runLater(() -> setImageViewSize());
        /**
         * Инициализация списка коэф. усиления.
         */
        cbCCCOptions.setItems(optionsCCC);
        /**
         * Установка полей в прошлое состояние.
         */
        params.loadParams("props.properties");
        /**
         * Обработка смены коэф. усиления
         */
        cbCCCOptions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            if ("1".equals(newValue)) {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setССС(false);
                    params.setTempKU(false);
                }

            } else {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setССС(true);
                    params.setTempKU(true);
                }
            }
            resetBTNS();

        });
        /**
         * Установка списка разрешений.
         */
        cbDimOptions.setItems(optionsDimension);
        /**
         * Активация разрешения.
         */
        if (viewSize == DetectorResolution.CHINA.getSize()) {
            cbDimOptions.getSelectionModel().select(optionsDimension.get(0));
        } else if (viewSize == DetectorResolution.CHINALOW.getSize()) {
            cbDimOptions.getSelectionModel().select(optionsDimension.get(1));
        } else {
            cbDimOptions.getSelectionModel().select(optionsDimension.get(0));
        }
        boolean tempKU;

        if (tempKU = params.isTempKU()) {
            cbCCCOptions.getSelectionModel().select(tempKU ? 1 : 0);
        }
        /**
         * Обработка отклика на смену разрешения.
         */
        cbDimOptions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            if ("92*90".equals(newValue)) {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setDim(false);
                    params.setDimention(newValue);
                }
            } else {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setDim(true);
                    params.setDimention(newValue);
                }
            }

            resetBTNS();
        });

        /**
         * Обработка отклика на нажатие кнопки вкл.
         */
        chPower.selectedProperty().addListener((observable, oldValue, newValue) -> {

            /**
             * Запрос на статус
             */

            if (newValue) {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setPower(true);
                    params.setTempPower(true);
                    /**
                     *Установка стартовых параметров
                     */
                    extStartSession();
                }
            } else {
                if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
                    ((DetectorDevice.ChinaSource) selDetector.getDevice()).setPower(false);
                    params.setTempPower(false);
                }
            }
            resetBTNS();
        });
        btnLookUp.setOnAction(event -> Detector.getDiscoveryService().scan());

        Platform.runLater(() -> setImageViewSize());
        /**
         *Сброс строки состояния
         */
        pb_exp.setVisible(false);
        lab_exp_status.setText("");
    }

    /**
     * Запись ip выбранного детектора
     *
     * @param detectorName
     */
    private void saveDetIp(String detectorName) {
        String[] strings = detectorName.split(" ");
        boolean driverSelect = cbNetworkOptions.getSelectionModel().isSelected(0);
        if (!driverSelect) {
            params.setDetIP(strings[0]);
        }
    }

    private void setAllAnotherButtons() {
        tb_antialising.selectedProperty().addListener((observable, oldValue, newValue) -> detectorPanel.setAntialiasingEnabled(newValue));
        tb_mirror.selectedProperty().addListener((observable, oldValue, newValue) -> detectorPanel.setMirrored(newValue));
        tb_debug.selectedProperty().addListener((observable, oldValue, newValue) -> {
            detectorPanel.setFPSDisplayed(newValue);
            detectorPanel.setImageSizeDisplayed(newValue);
            detectorPanel.setDisplayDebugInfo(newValue);

        });

        cb_flip.setItems(optionsFlip);
        cb_flip.getSelectionModel().select(optionsFlip.get(0));
        cb_flip.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            switch (newValue) {
                case "Нет поворота":
                    DetectorPanel.setFlipper(null);
                    break;
                case "Поворот: +90\u00B0":
                    DetectorPanel.setFlipper(new JHFlipFilter(FLIP_90CW));
                    break;
                case "Поворот: -90\u00B0":
                    DetectorPanel.setFlipper(new JHFlipFilter(FLIP_90CCW));
                    break;
                case "Поворот: 180\u00B0":
                    DetectorPanel.setFlipper(new JHFlipFilter(FLIP_180));
                    break;
            }
        });


        tb_rgb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                detectorPanel.setImageTransformer(new MyChinaRgbImageTransformer());
                tb_gray.selectedProperty().setValue(false);
                BufferedImage bufferedImage = fillPolosa();
                ivPolosa.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            } else {
                tb_gray.selectedProperty().setValue(true);
            }

        });

        tb_gray.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                detectorPanel.setImageTransformer(new MyChinaGrayTramsformer());
                tb_rgb.selectedProperty().setValue(false);
                BufferedImage bufferedImage = fillPolosa();
                ivPolosa.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            } else {
                tb_rgb.selectedProperty().setValue(true);
            }
        });

        tb_norm.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                detectorPanel.setNormalayzer(new JHNormalizeFilter());
            } else {
                detectorPanel.setNormalayzer(null);
            }
        });
    }

    /**
     * Настройка кнопок рисования.
     */
    private void setDrawButton() {

        ArrayList<ToggleButton> toggleMode = new ArrayList<>();
        toggleMode.add(tb_none);
        toggleMode.add(tb_fill);
        toggleMode.add(tb_fit);
        int i = 0;
        for (ToggleButton tb :
                toggleMode) {
            tb.setToggleGroup(growModeGroup);
            tb.setUserData(i++);
        }
        growModeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if ((int) newValue.getUserData() == 0) {
                    detectorPanel.setDrawMode(DetectorPanel.DrawMode.NONE);
                } else if ((int) newValue.getUserData() == 1) {
                    detectorPanel.setDrawMode(DetectorPanel.DrawMode.FILL);
                } else {
                    detectorPanel.setDrawMode(DetectorPanel.DrawMode.FIT);
                }
            } else {
                growModeGroup.selectToggle(tb_none);
            }
        });
    }

    /**
     * Скан детекторов и заполнение списка.
     */
    private void fillDetectors() {

        /**
         * Заполнение списка детекторов.
         */
        Detector.getDetectors();

        /**
         * Установка списка в комбобокс.
         */
        cbDetectorOptions.setItems(options);
    }


    /**
     * Небольшая пауза.
     */
    private void waitNewImage() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Инициализация работы детектора.
     */
    private void extStartSession() {
        Thread thread = new Thread(() -> {
            boolean selectedFullScr = cbDimOptions.getSelectionModel().isSelected(0);
            waitNewImage();
            if (FT_STATUS.FT_OK != ((DetectorDevice.ChinaSource) selDetector.getDevice()).setDim(selectedFullScr)) {
                return;
            }
            waitNewImage();
            if (setInt() != FT_STATUS.FT_OK) {
                return;
            }
            waitNewImage();
            if (setReference() != FT_STATUS.FT_OK) {
                return;
            }
            waitNewImage();
            if (setVOS() != FT_STATUS.FT_OK) {
                return;
            }
            waitNewImage();
            if (setVR0() != FT_STATUS.FT_OK) {
                return;
            }
            boolean selectedCcc = cbCCCOptions.getSelectionModel().isSelected(1);
            waitNewImage();
            if (FT_STATUS.FT_OK != ((DetectorDevice.ChinaSource) selDetector.getDevice()).setССС(selectedCcc)) {
                return;
            }
            waitNewImage();
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Заполнение (отрисовка) полосы.
     *
     * @return
     */
    private BufferedImage fillPolosa() {
        float[] floats = new float[50];
        for (int i = 0; i < 50; i++) {
            floats[i] = 195;
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage bi = gc.createCompatibleImage(300, 10);
        Graphics2D g2 = ge.createGraphics(bi);
        g2.setBackground(new Color(204, 204, 204, 255));
        g2.clearRect(0, 0, 300, 51);

        for (int i = 0; i < 50; i++) {
            drawRect(g2, 100, i, 50);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.dispose();
        bi.flush();
        return bi;
    }

    /**
     * Выставление размеров (свойств) картинки относительно размера ImageView.
     */
    protected void setImageViewSize() {
        double height = pnGist.getHeight();
        double width = pnGist.getWidth();
        iwGist.setFitHeight(height);
        iwGist.setFitWidth(width);
        iwGist.prefHeight(height);
        iwGist.prefWidth(width);
        iwGist.setPreserveRatio(true);
    }

    /**
     * Инициализация детектора.
     *
     * @param detectorIndex индекс найденного детектора.
     */
    protected void initializeDetector(final int detectorIndex) {

        Task<Void> detectorIntilizer = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                /**
                 * Если нет активного детектора, тогда инициализировать выбранный.
                 */
                if (selDetector == null) {
                    initPanel(detectorIndex, FPSVideo);
                } else {
                    /**
                     * Если уже есть активный детектор, тогда закрыть активный и инициализировать выбранный.
                     */
                    closeDetector();
                    initPanel(detectorIndex, FPSVideo);
                }
                /**
                 * Старт  видеопотока.
                 */
                startDetectorStream();
                return null;
            }
        };
        new Thread(detectorIntilizer).start();
        /**
         * Активировать панель с кнопками.
         */
        btnGetData.setDisable(true);
        btnParams.setDisable(true);
    }

    /**
     * Инициализация панели.
     *
     * @param detectorIndex индекс детектора
     * @param FPS           ограничение кадровой частоты
     */
    private void initPanel(int detectorIndex, double FPS) {
        selDetector = Detector.getDetectors().get(detectorIndex);
        viewSize = selDetector.getViewSize();
        detectorPanel = new DetectorPanel(selDetector, viewSize, true, async);
        detectorPanel.setFPSLimited(fpsLimited);
        detectorPanel.setImageTransformer(imageTransformer);
        detectorPanel.setPause(params.getPAUSE());
        snDetectorCapturedImage.setContent(detectorPanel);
        initFPSservice();
        initStatService();
        BufferedImage bufferedImage = fillPolosa();
        ivPolosa.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

    }

    /**
     * Старт видеопотока.
     */
    protected void startDetectorStream() {
        /**
         * Флаг работы потока
         */
        stopVideo = false;

        if (paused) {
            detectorPanel.resume();
            initStatService();
            paused = false;
        }
    }

    /**
     * Отработка закрытия детектора.
     */
    private void closeDetector() {
        if (selDetector != null) {
            detectorPanel.stop();
        }
    }

    /**
     * Отработка кнопки старт/стоп.
     *
     * @param event
     */
    public void stopDetector(ActionEvent event) {
        if (!paused) {
            stopVideo = true;
            detectorPanel.pause();
            btnStartStop.setText("Старт");
            paused = true;
        } else {
            stopVideo = false;
            startDetectorStream();
            btnStartStop.setText("Стоп");
        }
    }

    /**
     * Отработка кнопки сброса.
     *
     * @param event
     */
    public void disposeDetector(ActionEvent event) {
        stopVideo = true;
        closeDetector();
    }

    /**
     * Отработка установки задержки опроса платы
     *
     * @param event
     */
    public void setPauseOnPlate(ActionEvent event) {
        TextField source = (TextField) event.getSource();
        try {
            String text = source.getText();
            int value = Integer.parseInt(text);
            detectorPanel.setPause(value);
            source.setText(text);
            params.setPAUSE(value);
        } catch (Exception e) {
            setError(source, "Error");
            LOG.error("Integer processing error", e);
        }

        source.selectAll();
        source.getParent().requestFocus();
    }

    /**
     * Сервис отображения FPS.
     */
    private void initFPSservice() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (!stopVideo) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        Platform.runLater(() -> {
                            lb_online.setVisible(!((DetectorDevice.ChinaSource) selDetector
                                    .getDevice()).isOnline());

                        });
                    } catch (Exception e) {
                        //ignore
                    }
                }
                return null;
            }
        };
        //Старт потока
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Отображение статистики.
     */
    private void initStatService() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                while (!stopVideo) {
                    try {
                        DetectorDevice.ChinaSource device = (DetectorDevice.ChinaSource) selDetector.getDevice();
                        if (device == null) {
                            return null;
                        }
                        int[][] frameData = device.getFrame();
                        if (frameData != null) {
                            StatData statData = new StatData(frameData);

                            grabbedImage = drawMainGist(statData.getDataArray());
                            grabbedImageH = drawLowGist(statData.getSKOArray(), true);
                            grabbedImageV = drawLowGist(statData.getSKOArrayHorisontal(), false);

                            Platform.runLater(() -> {
                                //Отображение статистики в полях
                                lbMax.setText(FORMATTER.format(statData.getMAX()));
                                lbMin.setText(FORMATTER.format(statData.getMin()));
                                lbSKO.setText(FORMATTER.format(statData.getSKO()));
                                lbAverageSignal.setText(FORMATTER.format(statData.getMean()));
                                //Отображение гистограмм
                                /**
                                 * Главная гистограмма.
                                 */
                                if (grabbedImage != null) {
                                    final Image gistIamgeToFX = SwingFXUtils
                                            .toFXImage(grabbedImage, null);
                                    gistImageProperty.set(gistIamgeToFX);
                                }
                                /**
                                 * Горизонтальная гистограмма.
                                 */
                                final Image gistHIamgeToFXH = SwingFXUtils
                                        .toFXImage(grabbedImageH, null);
                                gistImagePropertyH.set(gistHIamgeToFXH);
                                /**
                                 * Вертикальная гистограмма.
                                 */
                                final Image gistVIamgeToFXV = SwingFXUtils
                                        .toFXImage(grabbedImageV, null);
                                gistImagePropertyV.set(gistVIamgeToFXV);
                            });
                            if (grabbedImage != null) {
                                grabbedImage.flush();
                            }
                            grabbedImageH.flush();
                            grabbedImageV.flush();

                        }
                    } catch (Exception e) {
                        LOG.error("Error in staticService {}", e);
                    }
                }
                return null;
            }
        };
        //Старт потока
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        iwGist.imageProperty().bind(gistImageProperty);
        iwGistSKO_H.imageProperty().bind(gistImagePropertyH);
        iwGistSKO_V.imageProperty().bind(gistImagePropertyV);
    }


    /**
     * Отработка установки инта.
     *
     * @param event
     */
    public void setInt(ActionEvent event) {
        int i = parseIntText(event, true);
        if (i < 0) {
            return;
        }
        params.setTempInt(i);
        double fEfect = 1.0 / ((1.0E-06) * (2.0) * (i));
        params.setfEfect(fEfect);
        setInt();
        resetBTNS();

    }

    /**
     * Сброс цвета кнопок.
     */
    private void resetBTNS() {
        btnGetData.setDisable(true);
        btnParams.setDisable(true);
        btnGetData.setStyle("-fx-background-color:  orange");
        btnParams.setStyle("-fx-background-color:  orange");
        btnPotok.setStyle("-fx-background-color:  orange");
    }

    /**
     * Установка инта.
     */
    public FT_STATUS setInt() {
        if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
            FT_STATUS ft_status = ((DetectorDevice.ChinaSource) selDetector.getDevice()).setInt(params.getTempInt());
            return ft_status;
        }
        return null;
    }


    /**
     * Отработка установки VOS/скимменг.
     *
     * @param event
     */
    public void setVOS(ActionEvent event) {
        int i = parseIntText(event, true);
        if (i < 0) {
            return;
        }
        params.setTempVOS(i);
        setVOS();
        resetBTNS();
    }

    /**
     * Установка VOS.
     */
    public FT_STATUS setVOS() {
        if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
            FT_STATUS ft_status = ((DetectorDevice.ChinaSource) selDetector.getDevice()).setVOS(params.getTempVOS());
            return ft_status;
        }
        return null;
    }

    /**
     * Отработка установки VREF и VOUTREF.
     *
     * @param event
     */
    public void setReference(ActionEvent event) {

        int i = parseIntText(event, true);
        if (i < 0) {
            return;
        }
        params.setTempREF(i);
        setReference();
        resetBTNS();
    }

    /**
     * Установка VREF и VOUTREF.
     */
    public FT_STATUS setReference() {
        if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
            FT_STATUS ft_status = ((DetectorDevice.ChinaSource) selDetector.getDevice()).setReference(params.getTempREF());
            return ft_status;
        }
        return null;
    }


    /**
     * Отработка установки VR0/смещения.
     *
     * @param event
     */
    public void setVR0(ActionEvent event) {
        int i = parseIntText(event, true);
        if (i < 0) {
            return;
        }
        params.setTempVR0(i);
        setVR0();
        resetBTNS();
    }

    /**
     * Установка VR0/смещение.
     */
    public FT_STATUS setVR0() {
        if (selDetector.getDevice() instanceof DetectorDevice.ChinaSource) {
            FT_STATUS ft_status = ((DetectorDevice.ChinaSource) selDetector.getDevice()).setVR0(params.getTempVR0());
            return ft_status;
        }
        return null;
    }

    /**
     * Установка числа отсчетов.
     *
     * @param event
     */
    public void setCountFrames(ActionEvent event) {
        int i = parseIntText(event, false);
        if (i < 0) {
            return;
        }
        params.setCountFrames(i);
        resetBTNS();
    }

    /**
     * Установка размеров окна центральной части.
     *
     * @param event
     */
    public void setKvadratSize(ActionEvent event) {
        int i = parseIntText(event, false);
        if (i < 0) {
            return;
        }
        TextField source = (TextField) event.getSource();
        String id = source.getId();
        Dimension viewSize = selDetector.getViewSize();
        if (detectorPanel != null) {
            if (id.equals("tfKvadratHeight")) {
                detectorPanel.setAimHeight(i + 2 <= viewSize.getHeight() ? i : (int) viewSize.getHeight() - 1);
            } else {
                detectorPanel.setAimWidth(i + 2 <= viewSize.getWidth() ? i : (int) viewSize.getWidth() - 1);
            }
        }
    }

    /**
     * Отрисовка главной гистограммы распределения.
     *
     * @param data входной массив данных.
     * @return картинка.
     */
    private BufferedImage drawMainGist(float[] data) {

        int height = (int) pnGist.getHeight();
        int width = (int) pnGist.getWidth();
        if (height <= 0 || width <= 0) {
            return null;
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage bi = gc.createCompatibleImage(width, height);

        Graphics2D g2 = ge.createGraphics(bi);

        g2.setBackground(new Color(204, 204, 204, 255));
        g2.clearRect(0, 0, width, height);

        drowColomn(data, g2);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.dispose();
        bi.flush();
        return bi;
    }

    int maxVisota = -1;
    int maxVhogrenie = -1;
    int bochka = -1;
    int length = 0;

    /**
     * Отрисовка столбцов.
     *
     * @param data
     * @param g2
     */
    private void drowColomn(float[] data, Graphics2D g2) {
        maxVisota = -1;
        maxVhogrenie = -1;
        bochka = -1;
        length = data.length;
        int[] ints = takeCountInside(data, true, 195, 300);
        for (int i = 0; i < ints.length; i++) {

            drawRect(g2, ints[i], i, ints.length);
            if (ints[i] > maxVhogrenie) {
                maxVhogrenie = ints[i];
                bochka = i;
            }
        }
    }

    /**
     * Отрисовка примоугольника.
     *
     * @param g2
     * @param value
     * @param i
     * @param length
     */
    private void drawRect(Graphics2D g2, int value, int i, int length) {

        int shaG = 300 / length;
        float acp = detectorPanel.getImageTransformer().getRazryadnost();
        int color = detectorPanel.getImageTransformer()
                .convertValueToColor((int) ((acp / length) * (i)));
        g2.setColor(new Color(color));

        int visota = (int) (195 * value * 0.01);
        g2.fillRect(shaG * i, 195 - visota, shaG, visota);

    }

    ///////////////////

    /**
     * Расчет количества вхождений.
     *
     * @param dataArray      входные параметры.
     * @param enableMasshtab флагмасштабирования.
     * @param h              высота поля.
     * @param w              ширина.
     * @return
     */
    private int[] takeCountInside(float[] dataArray, boolean enableMasshtab, int h, int w) {

        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;

        for (int i = 0; i < dataArray.length; i++) {
            if (dataArray[i] > maxValue) {
                maxValue = dataArray[i];
            }
            if (dataArray[i] < minValue) {
                minValue = dataArray[i];
            }
        }

        int countOtrezkov = 50;
        float acp = detectorPanel.getImageTransformer().getRazryadnost();

        double delta = acp / countOtrezkov;
        int[] tempArray = new int[countOtrezkov];
        int length = dataArray.length;
        //отработка вхождений
        for (int i = 0; i < dataArray.length; i++) {
            boolean entered = false;
            for (int j = 0; j < countOtrezkov; j++) {

                if (((delta * j) <= dataArray[i]) && (dataArray[i] < (delta * (j + 1)))) {
                    tempArray[j] += 1;
                    entered = true;
                }
            }
            if (!entered) {
                tempArray[countOtrezkov - 1] += 1;
            }
        }
        float koef = (float) ((1.0 * h) / length);
        if (enableMasshtab) {
            for (int i = 0; i < countOtrezkov; i++) {
                tempArray[i] = (int) DoubleRounder.round((tempArray[i] * koef), 0);
            }
        }
        return tempArray;
    }

    /**
     * Отрисовка гистограмм распределения по вертикали и горизонтали.
     *
     * @param data         исходные данные.
     * @param TYPE_DIAGRAM true- вертикаль, false -горизонталь.
     * @return картинка.
     */
    private BufferedImage drawLowGist(float[] data, boolean TYPE_DIAGRAM) {

        if (!TYPE_DIAGRAM) {
            float[] tempData = new float[data.length];
            for (int i = 0; i < data.length; i++) {
                tempData[i] = data[data.length - 1 - i];
            }
            data = tempData;
        }

        int height = (int) pnFlash.getHeight() / 2;
        int width = (int) pnFlash.getWidth();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage bi = gc.createCompatibleImage(width, height);

        Graphics2D g2 = ge.createGraphics(bi);

        g2.setBackground(new Color(204, 204, 204, 255));
        g2.clearRect(0, 0, width, height);

        applySKO(data, g2, height, width, TYPE_DIAGRAM);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.dispose();
        bi.flush();
        return bi;
    }

    float maxVisotaSKO = -1;
    int maxVhogrenieSKO = -1;
    int bochkaSKO = -1;
    int lengthSKO = -1;

    /**
     * Расчет распределения для гистограмм строк и столбцов.
     *
     * @param data         данные.
     * @param g2           среда.
     * @param height       высота поля.
     * @param width        ширина поля.
     * @param TYPE_DIAGRAM тип диаграммы. true- вертикаль, false -горизонталь.
     */
    private void applySKO(float[] data, Graphics2D g2, int height, int width, boolean TYPE_DIAGRAM) {
        maxVisotaSKO = -1;
        maxVhogrenieSKO = -1;
        bochkaSKO = -1;
        lengthSKO = data.length;
        for (int i = 0; i < data.length; i++) {

            if (maxVisotaSKO < data[i]) {
                maxVisotaSKO = data[i];
                bochkaSKO = i;
            }
        }
        float koef = height / maxVisotaSKO;
        for (int i = 0; i < data.length; i++) {
            drawLitleColomn(g2, (int) (data[i] * koef), i, data.length, height, width);
        }
        drawMaxValueOnGist(g2, data.length - 1 - bochkaSKO, maxVisotaSKO, data.length,
                height, width, TYPE_DIAGRAM);
    }

    /**
     * Отрисовка маленьких столбцов c показом максимума.
     *
     * @param g2
     * @param value  значение.
     * @param i      номер столбца с максимальным значением.
     * @param length количество столбцов.
     * @param height высота поля.
     * @param width  ширина поля.
     */
    private void drawLitleColomn(Graphics2D g2, int value, int i, int length, int height, int width) {

        int shaG = width / length;
        if (i == bochkaSKO) {
            g2.setColor(new Color(0, 0, 0));
        } else {
            g2.setColor(new Color(68, 133, 3));
        }
        int visota = height - value;
        g2.fillRect(300 - 22 - shaG * i, visota, shaG, value);
    }

    /**
     * Отрисовка значения на истограмме.
     *
     * @param g2
     * @param bochka       номер столбца с максимальным значением.
     * @param maxVhogrenie максимальное значение.
     * @param size         количество столбцов.
     * @param height       высота.
     * @param width        ширина.
     * @param TYPE_DIAGRAM тип диаграммы: строка, столбец.
     */
    private void drawMaxValueOnGist(Graphics2D g2, int bochka, float maxVhogrenie, int size, int height, int width,
                                    boolean TYPE_DIAGRAM) {
        Font font = new Font("sans-serif", Font.BOLD, 10);
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int shaG = width / size;
        for (int i = 0; i < size + 1; i = i + 32) {
            String s;
            if (i == 0) {
                s = (i) + "";
            } else {
                s = (i - 1) + "";
            }
            int w = 22 + shaG * i - metrics.stringWidth(s) / 2;
            int h = height - 5;
            int sw = w;
            int sh = h;
            g2.setColor(Color.BLACK);
            g2.drawString(s, sw + 1, sh + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(s, sw, sh);
        }
        font = new Font("sans-serif", Font.BOLD, 14);
        g2.setFont(font);
        metrics = g2.getFontMetrics(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        String s;
        if (TYPE_DIAGRAM) {
            s = "Макс: " + (int) maxVhogrenie + " мВ " + (bochka) + " строка";
        } else {
            s = "Макс: " + (int) maxVhogrenie + " мВ " + (bochka) + " столбец";
        }
        int w = width;
        int h = height;
        int sw = (w - metrics.stringWidth(s)) - 25;
        int sh = (h - metrics.getHeight()) - 20;
        g2.setColor(Color.BLACK);
        g2.drawString(s, sw + 1, sh + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(s, sw, sh);
    }

    /**
     * Старт расчета потока.
     *
     * @param event
     */
    @FXML
    private void startPotok(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        potokFxmlLoader = new FXMLLoader(getClass().getResource("potokPage.fxml"));
        Parent root = potokFxmlLoader.load();
        PotokController potokController = potokFxmlLoader.getController();
        potokController.initController(this);
        Scene scene = new Scene(root);
        stage.setTitle("Расчет значения потока и ввод параметров стенда");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Старт расчета параметров.
     *
     * @param event
     */
    @FXML
    private void startParams(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        paramsFxmlLoader = new FXMLLoader(getClass().getResource("paramsPage.fxml"));
        Parent root = paramsFxmlLoader.load();
        ParamsController paramsController = paramsFxmlLoader.getController();
        paramsController.initController(this);
        Scene scene = new Scene(root);
        stage.setTitle("Расчет характеристик детектора");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private DataService dataService;

    /**
     * Старт сервиса набора данных.
     *
     * @param event
     */
    @FXML
    private void startGetData(ActionEvent event) {
        if (dataService == null) {
            initDataservice();
        }
        getPb_exp().visibleProperty().bind(dataService.runningProperty());
        getPb_exp().progressProperty().bind(dataService.progressProperty());
        getLab_exp_status().textProperty().bind(dataService.messageProperty());
        if (dataService.getState() == Worker.State.RUNNING) {
            dataService.cancel();
            dataService = null;
        } else {
            dataService.restart();
        }
    }

    /**
     * Инициализация сервиса сбора данных.
     */
    public void initDataservice() {
        dataService = new DataService(this);
    }

    FXMLLoader potokFxmlLoader;
    FXMLLoader paramsFxmlLoader;

    public FXMLLoader getParamsFxmlLoader() {
        return paramsFxmlLoader;
    }

    public ComboBox<String> getCbDimOptions() {
        return cbDimOptions;
    }

    public static float getMASHTAB() {
        return MASHTAB;
    }

    public ProgressBar getPb_exp() {
        return pb_exp;
    }

    public Label getLab_exp_status() {
        return lab_exp_status;
    }

    public Detector getSelDetector() {
        return selDetector;
    }

    public TextField getTfInt() {
        return tfInt;
    }

    public StendParams getParams() {
        return params;
    }

    public Button getBtnPotok() {
        return btnPotok;
    }

    public Button getBtnGetData() {
        return btnGetData;
    }

    public Button getBtnParams() {
        return btnParams;
    }

    public ExpInfo getSelExp() {
        return selExp;
    }

    public ObservableList<ExpInfo> getOptionsExp() {
        return optionsExp;
    }

    public static int getExpCounter() {
        return expCounter++;
    }

    public TextField getTfVOS() {
        return tfVOS;
    }

    public TextField getTfVR0() {
        return tfVR0;
    }

    public ComboBox<String> getCbCCCOptions() {
        return cbCCCOptions;
    }

    public TextField getTfSpeedPlata() {
        return tfSpeedPlata;
    }

    public Label getLb_online() {
        return lb_online;
    }

    /**
     * Инициализация сети.
     *
     * @param index индекс найденного интрерфейса.
     */
    protected void initializeNetwork(final int index) {
        selNetworkInterface = optionsNetwork.get(index);
        params.setSelNetworkInterface(selNetworkInterface);
    }

    @Override
    public void detectorFound(DetectorDiscoveryEvent event) {

        int size = options.size();
        Detector detector = event.getDetector();
        DetectorInfo detectorInfo = new DetectorInfo();
        detectorInfo.setDetectorIndex(size);
        detectorInfo.setDetectorName(detector.getName());

        Platform.runLater(() -> {
            options.add(detectorInfo);
            btnLookUp.setStyle("-fx-background-color:  green");
        });

    }


    @Override
    public void detectorGone(DetectorDiscoveryEvent event) {
        LOG.debug("Детектор ушел");
    }

}
