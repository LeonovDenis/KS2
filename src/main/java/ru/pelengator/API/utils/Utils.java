package ru.pelengator.API.utils;

import at.favre.lib.bytes.Bytes;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.decimal4j.util.DoubleRounder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.App;
import ru.pelengator.ParamsController;
import ru.pelengator.model.SampleBarChart;
import ru.pelengator.model.ExpInfo;
import ru.pelengator.service.DocMaker;
import ru.pelengator.service.ImagePanel;
import ru.pelengator.service.ParamsService;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

import static java.awt.RenderingHints.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.zip.CRC32;

public class Utils {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);


    /**
     * Расчет контрольной суммы архива.
     *
     * @return значение.
     */
    public static String calkCRC32() {
        File currentClass = getFileToClass(App.class);
        String absolutePath = currentClass.getAbsolutePath();
        String value = Long.toHexString(calculate(absolutePath)).toUpperCase();
        return value;
    }

    /**
     * Создание объекта FIle по пути.
     *
     * @param clazz класс в архиве.
     * @return строкапути.
     */
    public static File getFileToClass(Class clazz) {
        File currentClass = null;
        try {
            currentClass = new File(URLDecoder.decode(clazz
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

        }
        return currentClass;
    }

    /**
     * Загрузка DLL файла библиотеки во временную директорию.
     *
     * @param name имя библиотеки драйвера USB.
     * @return путь расположения файла dll библиотеки.
     */
    public static String loadJarDll(String name) {
        InputStream in = App.class.getResourceAsStream(name);//загрузка файла
        byte[] buffer = new byte[1024];
        int read = -1;
        File temp = null;
        FileOutputStream fos = null;
        try {
            temp = File.createTempFile(name, "");//создание временного файла
            fos = new FileOutputStream(temp);
            //копирование файла библиотеки
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return temp.getAbsolutePath();//ссылка на временный файл
    }

    public static String separator = System.getProperty("file.separator");

    /**
     * Поиск файла в директории.
     *
     * @param path путь.
     * @param find расширение файла.
     * @return
     */
    public static String findFileInPath(String path, String find) {
        File f = new File(path);
        //список файлов в текущей папке
        System.out.println(f.getAbsolutePath());
        String[] list = f.list();
        //проверка на совпадение
        System.out.println(Arrays.toString(list));
        if (list != null) {
            for (String file : list) {
                if (file.endsWith(find)) {
                    String PATH = path + separator + file;
                    return PATH;
                }
            }
        }
        return "null";
    }


    /**
     * Расчет контрольной суммы по методу CRC-32.
     *
     * @param szPath путь к файлу.
     * @return значение суммы.
     */
    public static long calculate(String szPath) {
        CRC32 cs = new CRC32();
        long value = 0;
        byte[] buf = new byte[8000];
        int nLength = 0;

        try (FileInputStream fis =
                     new FileInputStream(szPath)) {
            while (true) {
                nLength = fis.read(buf);
                if (nLength < 0)
                    break;
                cs.update(buf, 0, nLength);
            }
        } catch (IOException e) {
            //ignore
        }
        value = cs.getValue();
        return value;
    }

    /**
     * Сохранение файла на диск.
     *
     * @param fileName путь файла.
     * @param data     массив данных.
     * @throws IOException
     */
    public static void saveFileToDisk(String fileName, byte[] data) throws IOException {

        FileOutputStream stream = new FileOutputStream(fileName, false);
        FileChannel channel = stream.getChannel();
        FileLock lock = null;
        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            stream.close();
            channel.close();

        }
        stream.write(data);
        lock.release();
        stream.close();
        channel.close();
    }

    /**
     * Запись файла в байтовый массив
     * @param fileName имя файла (путь)
     * @throws IOException
     */
    public static byte[] loadFileFromDisk(String fileName) throws IOException {

        FileInputStream stream = new FileInputStream(fileName);
        FileChannel channel = stream.getChannel();
        long size = channel.size();
        byte[] data= new byte[(int) size];

        stream.read(data);
        stream.close();
        channel.close();

        return data;
    }


    /**
     * Конвертер байтового массива в интовый с изменением порядка байт. Значения по 2 байта.
     *
     * @param source Байтовый необработанный массив.
     * @return Интовый обработанный массив.
     */
    public static Bytes convertBytesArrayToIntArray(Bytes source) {
        byte[] bbArray = source.array();
        Bytes intArray = Bytes.from(new int[0]);
        for (int i = 0; i < source.length(); i = i + 2) {
            char c = Bytes.from(new byte[]{bbArray[i], bbArray[i + 1]}).reverse().toChar();
            intArray = intArray.append((int) c);
        }
        return intArray;
    }

    /**
     * Замена порядка строк.
     * Выполнение квантования.
     *
     * @param src
     */
    public static void convertImageRGB(BufferedImage src) {

        int width = src.getWidth();
        int height = src.getHeight();
        int[][] tempData = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempData[y][x] = src.getRGB(x, y) & 0xffffff;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                src.setRGB(x, y, qvantFilterRGB(tempData[y][x]));
            }
        }
    }

    /**
     * Квантователь. Присвает цвет значению.
     * Двухбайтовае значение квантует на 1021 значениецвета.
     * <br>
     * <br>
     * Границы цвета: синий, голубой, салатовый,желтый, красный.
     *
     * @param sourceValue отсчеты АЦП [16384]
     * @return Цвет, один из 1021
     */
    public static int qvantFilterRGB(int sourceValue) {


        int BITBYTE = 255;
        double koef = ACP / (BITBYTE * 4.0 + 1.0);
        sourceValue = (int) (sourceValue / koef);
        int a = 0xff000000;
        int r = 0;
        int g = 0;
        int b = 0;

        if (sourceValue <= BITBYTE) {
            new Color(0, 0, BITBYTE);
            r = 0;
            g = sourceValue;
            b = BITBYTE;
        } else if (sourceValue > BITBYTE && sourceValue <= BITBYTE * 2) {
            new Color(0, BITBYTE, BITBYTE);
            r = 0;
            g = BITBYTE;
            b = BITBYTE - (sourceValue - BITBYTE);
        } else if (sourceValue > BITBYTE * 2 && sourceValue <= BITBYTE * 3) {
            new Color(0, BITBYTE, 0);
            r = sourceValue - BITBYTE * 2;
            g = BITBYTE;
            b = 0;
        } else if (sourceValue > BITBYTE * 3 && sourceValue <= BITBYTE * 4) {
            new Color(BITBYTE, BITBYTE, 0);
            r = BITBYTE;
            g = BITBYTE - (sourceValue - BITBYTE * 3);
            b = 0;
        } else {
            new Color(BITBYTE, 0, 0);
            r = BITBYTE;
            g = 0;
            b = 0;
        }

        return a | (r << 16) | (g << 8) | b;
    }

    /**
     * Квантователь для картинки.256 оттенков.
     *
     * @param src преобразуемое изображение.
     */
    public static void convertImageGray(BufferedImage src) {

        int width = src.getWidth();
        int height = src.getHeight();
        int[][] tempData = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempData[y][x] = src.getRGB(x, y) & 0xffffff;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                src.setRGB(x, y, qvantFilterGray(tempData[y][x]));
            }
        }
    }

    /**
     * Квантователь для оттенков серого.
     *
     * @param sourceValue
     * @return
     */
    public static int qvantFilterGray(int sourceValue) {


        int BITBYTE = 255;
        double koef = ACP / (BITBYTE + 1.0);
        sourceValue = (int) (sourceValue / koef);

        if (sourceValue < 0) {
            sourceValue = 0;
        }
        if (sourceValue > BITBYTE) {
            sourceValue = BITBYTE;
        }

        int a = 0xff000000;
        int r = sourceValue;
        int g = sourceValue;
        int b = sourceValue;

        return a | (r << 16) | (g << 8) | b;
    }


    /**
     * Конвертер изображения в массив.
     *
     * @param src BufferedImage.
     * @return int[][]
     */
    public static int[][] convertImageToArray(BufferedImage src) {

        int width = src.getWidth();
        int height = src.getHeight();
        int[][] tempData = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempData[y][x] = src.getRGB(x, y) & 0xffffff;
            }
        }
        return tempData;
    }

    /**
     * Клонирование BufferedImage.
     *
     * @param source BufferedImage.
     * @return
     */
    public static BufferedImage copyImage(BufferedImage source) {
        ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Количество отсчетов АЦП (16384.0)
     */
    public static float ACP = (float) (Math.pow(2, 14));

    /**
     * Масштаб
     */
    public static float MASHTAB = (5000 / ACP);


    /**
     * Подготовка массива данных для представления в качестве гистограммы распределения.
     *
     * @param dataArrays Массив для обработки.
     * @return массив вхождений для отображения.
     */

    public static RaspredData makeRaspred(double[][] dataArrays, String DEFAULT_FORMAT, boolean noCorrection, double persent) {

        double[][] tempData = new double[dataArrays.length][dataArrays[0].length];
        if (!noCorrection) {
            double[] mean = makeMaxMeanMin(dataArrays, true, 0);
            tempData = zamenaData(dataArrays, persent, mean[1]);
        }


        ArrayList<Double> doubleArrayList = new ArrayList<>();
        for (int i = 0; i < dataArrays.length; i++) {
            for (int j = 0; j < dataArrays[0].length; j++) {

                if (noCorrection) {
                    doubleArrayList.add(dataArrays[i][j]);
                } else {
                    if (tempData[i][j] != FALSE) {
                        doubleArrayList.add(tempData[i][j]);
                    }
                }
            }
        }
        Double[] dataArray = doubleArrayList.toArray(new Double[0]);

        double maxValue;
        int maxValue2 = 0;
        double minValue;
        double meanValue;
        int length = dataArray.length;

        StatisticsUtilsDouble statisticsUtils = new StatisticsUtilsDouble();
        for (int i = 0; i < length; i++) {
            statisticsUtils.addValue(dataArray[i]);
        }
        meanValue = statisticsUtils.getMean();
        maxValue = statisticsUtils.getMax();
        minValue = statisticsUtils.getMin();

        if (length <= 1) {
            return null;
        } else {

            double tempVal = 3.322D * Math.log10(length);
            double round = DoubleRounder.round(tempVal, 0);// округляем
            int countOtrezkov = 1 + (int) (round);//количество отрезков statisticsUtils-массив
            double delta = Math.abs((maxValue - minValue) / countOtrezkov);
            int[] tempArray = new int[countOtrezkov];
            //отработка вхождений
            for (int i = 0; i < length; i++) {
                boolean entered = false;
                for (int j = 0; j < countOtrezkov; j++) {

                    if (((minValue + delta * j) <= dataArray[i]) && (dataArray[i] < (minValue + delta * (j + 1)))) {
                        tempArray[j] += 1;
                        entered = true;
                    }
                }
                if (!entered) {
                    tempArray[countOtrezkov - 1] += 1;
                }
            }
            for (int i = 0; i < countOtrezkov; i++) {
                if (tempArray[i] > maxValue2) {
                    maxValue2 = tempArray[i];
                }
            }
            NumberFormat FORMATTER;
            FORMATTER = new DecimalFormat(DEFAULT_FORMAT);
            Map<String, Number> map = new LinkedHashMap<>();
            map.put(FORMATTER.format(minValue).replace(",", "."), -1);

            for (int i = 0; i < countOtrezkov; i++) {
                double verhnyaGranicaOtrezka = minValue + delta * (i + 1);
                map.put(FORMATTER.format(verhnyaGranicaOtrezka).replace(",", "."), tempArray[i]);
            }

            return new RaspredData(tempArray, map, maxValue2);
        }
    }

    /**
     * Класс данных для постройки распределения
     */
    public static class RaspredData {
        int[] array;
        int maxValue;
        Map<String, Number> map;

        public RaspredData(int[] array, Map<String, Number> map, int maxValue) {
            this.array = array;
            this.map = map;
            this.maxValue = maxValue;
        }

        public Map<String, Number> getMap() {
            return map;
        }

        public int getMaxValue() {
            return maxValue;
        }
    }

    /**
     * Стандартная гистограмма.
     *
     * @param nameXaxis подпись по оси X.
     * @param nameYaxis подписьпо оси Y.
     * @param raspred   данные для отображения.
     * @return Гистограмму распределения.
     */
    public static BarChart<String, Number> getBar_chart(String nameXaxis,
                                                        String nameYaxis,
                                                        RaspredData raspred) {
        CategoryAxis categoryAxis;
        int up;
        ObservableList<String> cat = FXCollections.observableArrayList();
        ObservableMap<String, Number> category = null;
        if (raspred == null) {
            categoryAxis = new CategoryAxis();
            up = 1;
        } else {
            category = FXCollections.observableMap((raspred.getMap()));
            up = raspred.getMaxValue();
            Set<String> strings = category.keySet();
            for (String s :
                    strings) {
                cat.add(s);
            }
            categoryAxis = new CategoryAxis(cat);
        }


        NumberAxis numberAxis = new NumberAxis();
        BarChart<String, Number> stringNumberBarChart = new BarChart<>(categoryAxis, numberAxis);

        numberAxis.setAutoRanging(false);
        numberAxis.setUpperBound(up);
        numberAxis.setTickUnit(up / 4);
        numberAxis.setLabel(nameYaxis);

        categoryAxis.setAutoRanging(false);
        categoryAxis.setGapStartAndEnd(true);
        categoryAxis.setTickLabelRotation(-90);

        categoryAxis.setTickLabelGap(5);
        stringNumberBarChart.setAnimated(false);
        stringNumberBarChart.setAlternativeColumnFillVisible(false);
        stringNumberBarChart.setAlternativeRowFillVisible(false);

        stringNumberBarChart.setHorizontalGridLinesVisible(true);
        stringNumberBarChart.setVerticalGridLinesVisible(false);

        stringNumberBarChart.setBarGap(1);
        stringNumberBarChart.setLegendVisible(false);
        stringNumberBarChart.setCategoryGap(-3);
        categoryAxis.setLabel(nameXaxis);

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (raspred != null) {
            for (int i = 0; i < cat.size(); i++) {
                if (i != 0) {
                    series.getData().add(new XYChart.Data(cat.get(i), category.get(cat.get(i))));
                } else {
                    series.getData().add(new XYChart.Data(cat.get(i), 0));
                }
                if (i != cat.size() - 1) {
                    series1.getData().add(new XYChart.Data(cat.get(i), category.get(cat.get(i + 1))));
                } else {
                    series1.getData().add(new XYChart.Data(cat.get(i), 0));
                }


            }
            stringNumberBarChart.getData().addAll(series, series1);

            /**
             * Вызов подробного графика при клике на основной
             *
             */
            ObservableMap<String, Number> finalCategory = category;
            stringNumberBarChart.setOnMouseClicked(event -> {
                new SampleBarChart().start("Подробный график",
                        "Распределение", nameXaxis, nameYaxis, finalCategory);
            });

        }
        stringNumberBarChart.setPrefSize(535, 320);
        stringNumberBarChart.setMinSize(535, 320);

        for (Node n : stringNumberBarChart.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: #000000;");
        }
        for (Node n : stringNumberBarChart.lookupAll(".default-color1.chart-bar")) {
            n.setStyle("-fx-bar-fill: #000000;");
        }

        return stringNumberBarChart;
    }

    /**
     * Заполнение изображения дефектными пикселями
     *
     * @param src    пустое изображение
     * @param bpList перечень дефектных пикселей
     * @return
     */
    public static BufferedImage fillTempImage(BufferedImage src, ArrayList<BadPoint> bpList) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Graphics2D g2Loc = ge.createGraphics(src);

        g2Loc.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        g2Loc.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_OFF);
        g2Loc.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        int iw = src.getWidth();
        int ih = src.getHeight();

        for (int w = 0; w < iw; w++) {
            for (int h = 0; h < ih; h++) {
                src.setRGB(w, h, Color.white.getRGB());

            }
        }

        int rw = 33;
        int rh = 33;
        int rx = (int) ((iw - rw) / 2.0) - 0;
        int ry = (int) ((ih - rh) / 2.0) - 0;

        g2Loc.setColor(new Color(0, 0, 0, 255));

        g2Loc.drawRect(rx, ry, rw, rh);


        for (BadPoint bp :
                bpList) {
            src.setRGB(bp.getX(), bp.getY(), bp.getColor().getRGB());
        }
        g2Loc.dispose();
        return copyImage(src);
    }

    /**
     * Отрисовка динамического квадрата.
     *
     * @param g2       ссылка на русурс.
     * @param wNature  реальное разрешение по ширине.
     * @param hNature  реальное разрешение по высоте.
     * @param rectW    ширина квадрата.
     * @param rectH    высота квадрата.
     * @param x1       начальное положение по Х.
     * @param y1       начальное положение по У.
     * @param sizeKoef коэфициент пикселя.
     */
    public static void drawRect(Graphics2D g2, int wNature, int hNature, int rectW, int rectH,
                                int x1, int y1, double sizeKoef) {

        int rx = (int) (x1 + ((wNature - rectW) / 2.0) / sizeKoef);
        int ry = (int) (y1 + ((hNature - rectH) / 2.0) / sizeKoef);


        g2.setColor(new Color(0, 0, 0, 255));
        g2.drawRect(rx, ry, (int) (rectW / sizeKoef), (int) (rectH / sizeKoef));
    }


    /**
     * Заполнение пустого изображения дефектными пикселями
     *
     * @param src    исходное пустоее изображение
     * @param bpList перечень дефектов
     * @return
     */
    public static BufferedImage fillTempImage(BufferedImage src, List<BadBigPoint> bpList) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Graphics2D g2Loc = ge.createGraphics(src);

        g2Loc.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        g2Loc.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_OFF);
        g2Loc.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        int iw = src.getWidth();
        int ih = src.getHeight();

        for (int w = 0; w < iw; w++) {
            for (int h = 0; h < ih; h++) {
                src.setRGB(w, h, Color.white.getRGB());

            }
        }

        int rw = 33;
        int rh = 33;
        int rx = (int) ((iw - rw) / 2.0) - 0;
        int ry = (int) ((ih - rh) / 2.0) - 0;

        g2Loc.setColor(new Color(0, 0, 0, 255));
        g2Loc.drawRect(rx, ry, rw, rh);
        for (BadBigPoint bp :
                bpList) {
            src.setRGB(bp.getX(), bp.getY(), bp.getColor().getRGB());
        }
        g2Loc.dispose();
        return copyImage(src);
    }

    /**
     * Кадр с дефектами.
     */
    public static class Frame {
        int sizeY;
        int sizeX;
        String name;
        ArrayList<BadPoint> bpList;

        public Frame(String name, int sizeX, int sizeY) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.name = name;
            bpList = new ArrayList<>();
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeX() {
            return sizeX;
        }

        public ArrayList<BadPoint> getBpList() {
            return bpList;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Типы дефектов.
     */
    public enum DEF_TYPE {

        TYPE_ARIFMETIC("Среднее арифметическое значение", "Sa"),
        TYPE_QUADRATIC("Среднее квадратическое значение", "Sq"),
        TYPE_SKO("Шум", "Ush"),
        TYPE_VW("Вольтовая чувствительность", "Su"),
        TYPE_POROG("Порог чувствительности", "F"),
        TYPE_POROG_STAR("Порог удельной чувствительности", "F*"),
        TYPE_DETECTIVITY("Обнаружительная способность", "D"),
        TYPE_DETECTIVITY_STAR("Удельная обнаружительная способность", "D*"),
        TYPE_NETD("Разность температур эквивалентная шуму", "NETD"),
        TYPE_EXPOSURE("Пороговая облученность", "E");

        DEF_TYPE(String value, String shotValue) {
            this.value = value;
            this.shotValue = shotValue;
        }

        public final String value;
        public final String shotValue;

        public String getValue() {
            return this.value;
        }

        public String getShotValue() {
            return shotValue;
        }
    }

    /**
     * Класс общих дефектов.
     */
    public static class BadBigPoint {
        private Color color;
        private List<DEF_TYPE> list;
        private int X;
        private int Y;
        private int size;
        private static int centerCount = 0;

        public BadBigPoint(BadPoint badPoint, Color color) {
            if (this.list == null) {
                this.list = new ArrayList<>();
            }
            list.add(badPoint.getType());

            this.X = badPoint.getX();
            this.Y = badPoint.getRealY();
            this.size = badPoint.getSize();
            this.color = color;

        }

        /**
         * Добавка типа дефекта в список
         *
         * @param badPoint
         */
        public void addToList(BadPoint badPoint) {
            list.add(badPoint.getType());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BadBigPoint)) return false;
            BadBigPoint that = (BadBigPoint) o;
            return X == that.X && Y == that.Y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(X, Y);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BadPoint{[").append(X).append(":").append(Y).append("]:");
            for (DEF_TYPE def : list) {
                stringBuilder.append(def.shotValue).append(";");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public int getX() {
            return X;
        }

        public int getY() {
            return size - Y;
        }

    }

    /**
     * Класс дефекта.
     */
    public static class BadPoint {

        private Color color;
        private DEF_TYPE type;
        private int X;
        private int Y;
        private double value;
        private int size;

        public BadPoint(int x, int y, DEF_TYPE type, Color color, double value, int size) {
            X = x;
            Y = y;
            this.value = value;
            this.type = type;
            this.color = color;
            this.size = size - 1;
        }

        public int getX() {
            return X;
        }

        public int getY() {
            return Y;
        }

        public int getRealY() {

            return size - Y;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public DEF_TYPE getType() {
            return type;
        }

        @Override
        public String toString() {
            return "BadPoint{" +
                    "[ " + X +
                    ": " + (size - Y) +
                    "], " + type.value +
                    ", value= " +
                    String.format(Locale.CANADA, "%.3e", value).toUpperCase() +
                    '}';
        }
    }

    /**
     * Получение максимального, среднего и минимального значения выборки.
     *
     * @param dataArrays   входные данные.
     * @param noCorrection исключение пустых значений.
     * @param FALSE        пустое значение.
     * @return
     */
    public static double[] makeMaxMeanMin(double[][] dataArrays, boolean noCorrection, double FALSE) {

        ArrayList<Double> doubleArrayList = new ArrayList<>();

        for (int i = 0; i < dataArrays.length; i++) {
            for (int j = 0; j < dataArrays[0].length; j++) {

                if (noCorrection) {
                    doubleArrayList.add(dataArrays[i][j]);
                } else {
                    if (dataArrays[i][j] != FALSE) {
                        doubleArrayList.add(dataArrays[i][j]);
                    }
                }
            }
        }

        Double[] dataArray = doubleArrayList.toArray(new Double[0]);

        double maxValue;
        double minValue;
        double meanValue;

        int length = dataArray.length;
        StatisticsUtilsDouble statisticsUtils = new StatisticsUtilsDouble();
        for (int i = 0; i < length; i++) {
            statisticsUtils.addValue(dataArray[i]);
        }
        meanValue = statisticsUtils.getMean();
        maxValue = statisticsUtils.getMax();
        minValue = statisticsUtils.getMin();

        return new double[]{maxValue, meanValue, minValue};
    }

    /**
     * Расчет неоднородности вольтовой чувствительности.
     *
     * @param voltWatka    исходные данные
     * @param noCorrection исключать ли пустые пиксили
     * @param persent      процент отбраковки
     * @return
     */
    public static Double calculateHeterogeneity(double[][] voltWatka, boolean noCorrection, double persent) {


        double[][] tempData = new double[voltWatka.length][voltWatka[0].length];
        if (!noCorrection) {
            double[] mean = makeMaxMeanMin(voltWatka, true, 0);
            tempData = zamenaData(voltWatka, persent, mean[1]);
        }


        ArrayList<Double> doubleArrayList = new ArrayList<>();
        for (int i = 0; i < voltWatka.length; i++) {
            for (int j = 0; j < voltWatka[0].length; j++) {

                if (noCorrection) {
                    doubleArrayList.add(voltWatka[i][j]);
                } else {
                    if (tempData[i][j] != FALSE) {
                        doubleArrayList.add(tempData[i][j]);
                    }
                }
            }
        }
        Double[] dataArray = doubleArrayList.toArray(new Double[0]);


        int length = dataArray.length;

        StatisticsUtilsDouble statisticsUtils = new StatisticsUtilsDouble();
        for (int i = 0; i < length; i++) {
            statisticsUtils.addValue(dataArray[i]);
        }

        double mean = statisticsUtils.getMean();
        double stdDev = statisticsUtils.getStdDev();

        return 100D * stdDev / mean;
    }

    /**
     * Расчет облученности ПОИ от АЧТ.
     * <p>
     * Ее=(eps*sigma*T4*Dist2)/(4*L2)
     *
     * @param potok   значение потока.
     * @param areaFPU площать элемента.
     * @return
     */
    public static double exposure(double potok, double areaFPU) {
        return (potok / areaFPU) * 1.0E-6;
    }

    /**
     * Расчет эффективной величины потока с АЧТ с двумя температурами.
     *
     * @param diamACHT диаметр диафрагмы АЧТ, мм.
     * @param l        расстояние между диафрагмой АЧТ и плоскостью фоточувствительного элемента испытуемого
     *                 образца, см.
     * @param betta    коэффициент , отн. ед..
     * @param T1       температура вторая, К.
     * @param T0       температура первая, К.
     * @return поток излучения, Вт. При некорректных исходных данных возвращает -1.
     */
    public static double potokTemp(double diamACHT, double l, double betta, double T1, double T0) {
        if (diamACHT <= 0 || l <= 0 || betta <= 0 || T1 <= 0 || T0 <= 0 || T0 >= T1) {
            return -1;
        }
        double plank = 5.669E-8;// постоянная Стефана-Больцмана, Вт/(м2 ·К4);
        double eps = 0.95;//коэффициент излучения полости АЧТ, отн. ед
        double areaFCHe = 9.0E-10;//эффективная фоточувствительная площадь испытуемого образца, м2 ;
        double areaACHT = 1.0E-6 * Math.PI * Math.pow(diamACHT, 2) / 4;//площадь отверстия диафрагмы АЧТ, м2
        l = l * 1.0E-2;//перевод в метры

        double Fe = (plank * (((eps * Math.pow(T1, 4)) - (eps * Math.pow(T0, 4))) * areaACHT * areaFCHe)) / (Math.PI * Math.pow(l, 2));
        return Fe * betta;
    }

    /**
     * Расчет эффективной величины потока с АЧТ с двумя диафрагмами.
     *
     * @param diamACHT1 диаметр диафрагмы АЧТ, мм.
     * @param diamACHT0 диаметр диафрагмы АЧТ, мм.
     * @param l         расстояние между диафрагмой АЧТ и плоскостью фоточувствительного элемента испытуемого
     *                  образца, см.
     * @param betta     коэффициент , отн. ед..
     * @param T         температура ачт, К.
     * @return поток излучения, Вт. При некорректных исходных данных возвращает -1.
     */
    public static double potokDiam(double diamACHT1, double diamACHT0, double l, double betta, double T) {
        if (diamACHT1 <= 0 || l <= 0 || betta <= 0 || T <= 0 || diamACHT0 >= diamACHT1) {
            return -1;
        }
        double plank = 5.669E-8;// постоянная Стефана-Больцмана, Вт/(м2 ·К4);
        double eps = 0.95;//коэффициент излучения полости АЧТ, отн. ед
        double areaFCHe = 9.0E-10;//эффективная фоточувствительная площадь испытуемого образца, м2 ;
        double areaACHT1 = 1.0E-6 * Math.PI * Math.pow(diamACHT1, 2) / 4;//площадь отверстия диафрагмы АЧТ, м2
        double areaACHT0 = 1.0E-6 * Math.PI * Math.pow(diamACHT0, 2) / 4;
        l = l * 1.0E-2;//перевод в метры

        double Fe = (plank * ((eps * Math.pow(T, 4)) * (areaACHT1 - areaACHT0) * areaFCHe)) / (Math.PI * Math.pow(l, 2));
        return Fe * betta;
    }

    /**
     * Расчет итогового потока разницы температур.
     *
     * @param T0        температура АЧТ
     * @param plank0    постоянная Планка
     * @param epsilin0  коэф. поглощения
     * @param areaACHT0 площадь АЧТ
     * @param L0        Расстояние между источником и приёмником
     * @param betta0    поэффициент поправки
     * @param areaFPU0  площадь элемента
     * @param T1        температура АЧТ
     * @param plank1    постоянная Планка
     * @param epsilin1  коэф. поглощения
     * @param areaACHT1 площадь АЧТ
     * @param L1        Расстояние между источником и приёмником
     * @param betta1    поэффициент поправки
     * @param areaFPU1  площадь элемента
     * @return
     */
    public static double potok(double T0, double plank0, double epsilin0, double areaACHT0, double L0, double betta0, double areaFPU0,
                               double T1, double plank1, double epsilin1, double areaACHT1, double L1, double betta1, double areaFPU1) {

        double F0 = betta0 * ((plank0 * epsilin0 * Math.pow(T0, 4) * areaFPU0 * areaACHT0) / (Math.PI * Math.pow(L0, 2)));
        double F1 = betta1 * ((plank1 * epsilin1 * Math.pow(T1, 4) * areaFPU1 * areaACHT1) / (Math.PI * Math.pow(L1, 2)));

        return F1 - F0;
    }

    /**
     * Расчет потока одной температуры.
     *
     * @param T
     * @param plank
     * @param epsilin
     * @param areaACHT
     * @param L
     * @param betta
     * @param areaFPU
     * @return
     */
    public static double potok(double T, double plank, double epsilin, double areaACHT, double L, double betta, double areaFPU) {

        double F = betta * ((plank * epsilin * Math.pow(T, 4) * areaFPU * areaACHT) / (Math.PI * Math.pow(L + 0.000_000_001, 2)));

        return F;
    }

    /**
     * Подсчет облученности
     * @param T
     * @param plank
     * @param epsilin
     * @param areaACHT
     * @param L
     * @param betta
     * @return Вт/см2
     */
    public static double obluch(double T, double plank, double epsilin, double areaACHT, double L, double betta) {

        double F =1.0E-04 * betta * ((plank * epsilin * Math.pow(T, 4) * areaACHT) / (Math.PI * Math.pow(L, 2)));
        return F;
    }


    /**
     * Расчет вольт-ваттной характеристики.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param potok       Поток излучения Вт.
     * @return Массив  В/Вт.
     */
    public static double[][] voltWatka(double[][] MeanValue_1, double[][] MeanValue_0, double potok) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArrayVw = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                dataArrayVw[h][w] = (MeanValue_1[h][w] - MeanValue_0[h][w]) / (potok);
            }
        }
        return dataArrayVw;
    }

    /**
     * Расчет обнаружительной способности.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param SKOValue    Шум в кадре.
     *                    значение в вольтах.
     * @param potok       Поток излучения Вт.
     * @param fEfect      Эквивалентная шумовая полоса частот Гц.
     * @return Вт-1*Гц-1/2.
     */
    public static double[][] detectivity(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue,
                                         double potok, double fEfect) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArrayDetectivity = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                dataArrayDetectivity[h][w] =
                        (((MeanValue_1[h][w] - MeanValue_0[h][w])) * (Math.sqrt(fEfect))) /
                                (potok * (SKOValue[h][w] + 0.0_000_000_000_000_001));
            }
        }
        return dataArrayDetectivity;
    }

    /**
     * Расчет Удельной обнаружительной способности.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param SKOValue    Шум в кадре.
     *                    значение в вольтах.
     * @param potok       Поток излучения Вт.
     * @param fEfect      Эквивалентная шумовая полоса частот Гц.
     * @param areaFPU     Эффективная площадь фотоприёмника.
     *                    значение в м.
     * @return Вт-1*Гц-1/2*см.
     */
    public static double[][] detectivityStar(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue,
                                             double potok, double fEfect, double areaFPU) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;

        double[][] dataArrayDetectivityStar = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                dataArrayDetectivityStar[h][w] =
                        (((MeanValue_1[h][w] - MeanValue_0[h][w])) * (Math.sqrt(areaFPU * 1.0E04 * fEfect))) /
                                (potok * (SKOValue[h][w] + 0.0_000_000_000_000_001));
            }
        }
        return dataArrayDetectivityStar;
    }


    /**
     * Перевод массива с АЦП отсчетами в Вольты.
     * с масштабированием.
     *
     * @param massiv
     * @return
     */
    public static double[][] acpToVolt(int[][] massiv) {

        int sizeY = massiv.length;
        int sizeX = massiv[0].length;

        double[][] data = new double[sizeY][sizeX];
        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {
                data[h][w] = (massiv[h][w] * MASHTAB) / (1000.0);
            }
        }

        return data;
    }


    /**
     * Расчет порога чувствительности.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param SKOValue    Шум в кадре
     *                    значение в вольтах.
     * @param potok       Поток излучения Вт.
     * @param fEfect      Эквивалентная шумовая полоса частот Гц.
     * @return Вт*Гц-1/2.
     */
    public static double[][] porogSensivity(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue,
                                            double potok, double fEfect) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArraySensivity = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {

                dataArraySensivity[h][w] =
                        (potok * SKOValue[h][w]) /
                                (((MeanValue_1[h][w] - MeanValue_0[h][w]) + 0.0_000_000_000_000_001)
                                        * (Math.sqrt(fEfect)));
            }
        }
        return dataArraySensivity;
    }

    /**
     * Расчет удельного порога чувствительности.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольта.х
     * @param SKOValue    Шум в кадре
     *                    значение в вольтах.
     * @param potok       Поток излучения Вт.
     * @param fEfect      Эквивалентная шумовая полоса частот Гц.
     * @param areaFPU     Эффективная площадь фотоприёмника.
     *                    значение в м.
     * @return Вт*Гц-1/2*см-1.
     */
    public static double[][] porogSensivityStar(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue,
                                                double potok, double fEfect, double areaFPU) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArraySensivityStar = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {

                dataArraySensivityStar[h][w] =
                        (potok * SKOValue[h][w]) /
                                (((MeanValue_1[h][w] - MeanValue_0[h][w]) + 0.0_000_000_000_000_001) *
                                        (Math.sqrt(areaFPU * 1.0E04 * fEfect)));
            }
        }
        return dataArraySensivityStar;
    }

    /**
     * Расчет разности температур, эквивалентной шума (NETD).
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param SKOValue    Шум в кадре.
     *                    значение в вольтах.
     * @param T1          большая температура, К.
     * @param T0          меньшая температура, К.
     * @return К.
     */
    public static double[][] NETD(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue,
                                  double T1, double T0) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArrayNETD = new double[sizeY][sizeX];

        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {

                dataArrayNETD[h][w] =
                        (SKOValue[h][w] * (T1 - T0)) / ((MeanValue_1[h][w] - MeanValue_0[h][w]) + 0.0_000_000_000_000_001);
            }
        }
        return dataArrayNETD;
    }

    /**
     * Расчет пороговой облученности.
     *
     * @param MeanValue_1 Кадры при большем потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param MeanValue_0 Кадры при меньшнм потоке.
     *                    Среднееквадратичное значение в вольтах.
     * @param SKOValue    Шум в кадре.
     *                    значение в вольтах.
     * @param Exposure    облученность Вт/см2.
     * @return Вт/см2.
     */
    public static double[][] exposure(double[][] MeanValue_1, double[][] MeanValue_0, double[][] SKOValue, double Exposure) {

        if (MeanValue_1.length != MeanValue_0.length) {
            return null;
        }
        int sizeY = MeanValue_1.length;
        int sizeX = MeanValue_1[0].length;
        double[][] dataArrayexposure = new double[sizeY][sizeX];


        for (int h = 0; h < sizeY; h++) {
            for (int w = 0; w < sizeX; w++) {

                dataArrayexposure[h][w] =
                        (SKOValue[h][w] * (Exposure)) / ((MeanValue_1[h][w] - MeanValue_0[h][w]) + 0.0_000_000_000_000_001);
            }
        }
        return dataArrayexposure;
    }

    /**
     * Площадь круга через диаметр
     * @param diametr мм
     *
     * @return м2
     */
    public static double sCircle(double diametr) {

        double sArea =(1.0E-06*(Math.PI * Math.pow(diametr, 2)) )/ 4.0;

        return sArea;
    }

    /**
     * Проверка и распарсивание целого числа.
     *
     * @param event
     * @return Положительное целое значение. -1 при ошибке парса. -2 если число отрицательное.
     */
    public static int parseIntText(ActionEvent event, boolean select) {
        TextField source = (TextField) event.getSource();
        String text = source.getText().trim();
        if (select) {
            source.selectAll();
        } else {
            source.getParent().requestFocus();
        }
        int i = 0;
        try {
            i = Integer.parseInt(text);
        } catch (Exception e) {
            LOG.error("Int value not valid");
            setError(source, "Error");
            return -1;
        }
        if (i < 0) {
            setError(source, "Error");
            return -2;

        }
        return i;
    }

    /**
     * Проверка и распарсивание дробного числа.
     *
     * @param event
     * @return Положительное дробное значение. -1 при ошибке парса. -2 если число отрицательное.
     */
    public static double parseDoubleText(ActionEvent event) {
        TextField source = (TextField) event.getSource();
        String text = source.getText().trim().replace(",", ".");
        source.getParent().requestFocus();
        double d = 0;
        try {
            d = Double.parseDouble(text);
        } catch (Exception e) {
            LOG.error("Double value not valid");
            setError(source, "Error");
            return -1;
        }
        if (d < 0) {
            setError(source, "Error");
            return -2;

        }
        return d;
    }

    /**
     * Вывод в поле ресурса сообщения об ошибке ввода.
     *
     * @param source ресурс.
     * @param Error  текст ошибки.
     */
    public static void setError(TextField source, String Error) {
        Platform.runLater(() -> source.setText(Error));
    }

    /**
     * Проверка правильности ip. шаблон
     *
     * @param ipAddress
     * @return true, если ip адрес имеет правильную форму
     */
    public static boolean ipv4Check(String ipAddress) {

        try {
            if (ipAddress != null && !ipAddress.isEmpty()) {
                String[] ip = ipAddress.split("\\.");
                if (ip.length != 4)
                    return false;

                for (int i = 0; i <= ip.length - 1; i++) {
                    int j = Integer.parseInt(ip[i]);
                    if (j < 0 || j > 255) {
                        return false;
                    }
                }
                if (ipAddress.endsWith(".")) {
                    return false;
                }
                if (ipAddress.startsWith(".")) {
                    return false;
                }

            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }


    /**
     * Обработка  надписей кнопок.
     *
     * @param but кнопка.
     * @param TXT сообщение.
     */
    public static void checkBT(Button but, String TXT) {
        checkBT(true, but, TXT, "");
    }

    /**
     * Обработка  надписей кнопок.
     *
     * @param res     случай.
     * @param but     кнопка.
     * @param goodTXT при tru.
     * @param badTXT  при false.
     */
    public static void checkBT(boolean res, Button but, String goodTXT, String badTXT) {
        String text = but.getText();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    but.setText(text);
                });
            }
        }, 1000);

        Platform.runLater(() -> {
            if (res) {
                but.setText(goodTXT);
            } else {
                but.setText(badTXT);
            }
            timer.purge();
        });
    }

    public static double FALSE = -111.0;

    /**
     * Добавка элементов в список.
     *
     * @param bpList       лист дефектов
     * @param data         данные
     * @param noCorrection true -если с учетом дефектов, false - если без учета
     * @param persent      процент отклонения от среднего
     * @param color        цет дефектного пикселя
     * @param type         тип дефекта
     * @return
     */
    public static double addBpToList(ArrayList<BadPoint> bpList, double[][] data,
                                     boolean noCorrection, double persent,
                                     String color, DEF_TYPE type) {


        double[] lines = makeMaxMeanMin(data, true, FALSE);
        double mean = lines[1];
        if (persent != 0) {
            int sizeY = data.length;
            int sizeX = data[0].length;

            for (int h = 0; h < sizeY; h++) {
                for (int w = 0; w < sizeX; w++) {
                    if (
                            Math.abs(mean - data[h][w]) >= Math.abs(mean * (persent * 0.01))
                    ) {
                        if (bpList != null) {
                            Color awtColor = convertcolor(color);
                            BadPoint badPoint = new BadPoint(w, h, type,
                                    awtColor, data[h][w], sizeY);
                            bpList.add(badPoint);
                        }
                    }
                }
            }
        }

        if (noCorrection) {
            //ignore
        } else {
            double[][] tempData = zamenaData(data, persent, mean);
            mean = makeMaxMeanMin(tempData, false, FALSE)[1];
        }
        return mean;
    }

    /**
     * Конвертер типов цвета.
     *
     * @param color
     * @return
     */
    public static Color convertcolor(String color) {
        javafx.scene.paint.Color webColor = javafx.scene.paint.Color.web(color);
        Color awtColor = new java.awt.Color((float) webColor.getRed(),
                (float) webColor.getGreen(),
                (float) webColor.getBlue(),
                (float) webColor.getOpacity());
        return awtColor;
    }

    /**
     * Замена  дефектного значения конкретным.
     *
     * @param data    ресурс.
     * @param persent процентовка.
     * @param mean    среднее значение.
     * @return отфильтрованный массив.
     */
    public static double[][] zamenaData(double[][] data, double persent, double mean) {
        double[][] tempData = new double[data.length][data[0].length];
        //заменить исходные данные
        for (int h = 0; h < data.length; h++) {
            for (int w = 0; w < data[0].length; w++) {
                if (
                        Math.abs(mean - data[h][w]) >= Math.abs(mean * (persent * 0.01))
                ) {
                    tempData[h][w] = FALSE;
                } else {
                    tempData[h][w] = data[h][w];
                }
            }
        }
        return tempData;
    }

    /**
     * Очистка перечня элементов панели
     *
     * @param pane
     */
    public static void clearPane(VBox pane) {
        if (pane.getChildren().size() > 0) {
            pane.getChildren().clear();
        }
    }

    /**
     * Создание гистограммы и квадрата в панели
     *
     * @param pane        панель
     * @param Xname       подпись по Х
     * @param Yname       подпись по У
     * @param raspredData данные
     * @param tempImage   пустой снимок
     * @param bpList      перечень днфектов
     * @param scList      лист дефектов
     * @param controller  ссылка на контролер
     * @return
     */
    public static HBox showGistAndImageBox(VBox pane, String Xname, String Yname,
                                           RaspredData raspredData, BufferedImage tempImage, ArrayList<BadPoint> bpList,
                                           ArrayList<BufferedImage> scList, ParamsController controller) {
        String selectedDist = controller.getController().getCbDimOptions().getSelectionModel().getSelectedItem();

        String[] splitedSize = selectedDist.split("\\*");
        int w = Integer.parseInt(splitedSize[0]);
        int h = Integer.parseInt(splitedSize[1]);


        BarChart<String, Number> bar_chart = getBar_chart(Xname, Yname, raspredData);

        ImageView imageView = new ImageView();
        BufferedImage badImage2 = fillTempImage(tempImage, bpList);
        BufferedImage bufferedImageToOrder = saveImg(badImage2, false);
        BufferedImage bufferedImageTolist = saveImg(badImage2, true);
        scList.add(bufferedImageToOrder);

        Image imageToFX2 = SwingFXUtils
                .toFXImage(bufferedImageTolist, null);

        imageView.imageProperty().set(imageToFX2);
        imageView.fitHeightProperty().bind(pane.widthProperty().divide(3.0));
        imageView.fitWidthProperty().bind(pane.widthProperty().divide(3.0));

        imageView.setOnMouseClicked(event -> {
            showBigImage(badImage2, w, h);
        });

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(imageView);


        pane.setAlignment(Pos.BASELINE_CENTER);
        badImage2.flush();
        bufferedImageTolist.flush();
        bufferedImageToOrder.flush();

        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);
        separator.setPrefSize(pane.getWidth(), 10);
        separator.getHalignment();
        separator.setPadding(new Insets(20, 5, 5, 5));

        HBox hBox = new HBox();
        hBox.getChildren().add(bar_chart);

        pane.getChildren().add(hBox);
        HBox hBox1 = new HBox();
        hBox1.setPadding(new Insets(0, 0, 0, 60));
        hBox1.getChildren().add(stackPane);

        pane.getChildren().add(hBox1);
        HBox hBox2 = new HBox();
        hBox2.getChildren().add(separator);

        pane.getChildren().add(hBox2);
        return null;
    }

    /**
     * Создание квадратов.
     *
     * @param pane
     * @param tempImage
     * @param bpList
     * @param scList
     * @param controller
     * @return
     */
    public static HBox showImageBox(VBox pane, BufferedImage tempImage, List<BadBigPoint> bpList,
                                    ArrayList<BufferedImage> scList, ParamsController controller) {
        String selectedDist = controller.getController().getCbDimOptions().getSelectionModel().getSelectedItem();

        String[] splitedSize = selectedDist.split("\\*");
        int w = Integer.parseInt(splitedSize[0]);
        int h = Integer.parseInt(splitedSize[1]);

        ImageView imageView = new ImageView();

        BufferedImage badImage2 = fillTempImage(tempImage, bpList);

        BufferedImage bufferedImageToOrder = saveImg(badImage2, false);
        BufferedImage bufferedImageTolist = saveImg(badImage2, true);

        scList.add(bufferedImageToOrder);

        Image imageToFX2 = SwingFXUtils
                .toFXImage(bufferedImageTolist, null);

        imageView.imageProperty().set(imageToFX2);

        imageView.fitHeightProperty().bind(pane.widthProperty().divide(3.0));
        imageView.fitWidthProperty().bind(pane.widthProperty().divide(3.0));

        imageView.setOnMouseClicked(event -> {
            showBigImage(badImage2, w, h);
        });
        badImage2.flush();
        bufferedImageTolist.flush();
        bufferedImageToOrder.flush();

        pane.getChildren().add(imageView);
        return null;
    }


    /**
     * Отображение увеличенного рисунка.
     *
     * @param iw
     * @param w
     * @param h
     */
    public static void showBigImage(ImageView iw, int w, int h) {

        Stage stage = new Stage();
        Image image = iw.getImage();
        SwingNode swingNode = new SwingNode();
        ImagePanel imagePanel = new ImagePanel(new Dimension(w, h), true);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        imagePanel.setImage(bufferedImage);
        swingNode.setContent(imagePanel);
        BorderPane pane = new BorderPane(swingNode);
        Scene scene = new Scene(pane, 500, 500);
        stage.setScene(scene);
        stage.setTitle("Scaled");
        stage.show();
    }

    /**
     * Построение окна с квадратом.
     *
     * @param iw
     * @param w
     * @param h
     */
    public static void showBigImage(BufferedImage iw, int w, int h) {

        Stage stage = new Stage();
        Image image = SwingFXUtils.toFXImage(iw, null);
        SwingNode swingNode = new SwingNode();
        ImagePanel imagePanel = new ImagePanel(new Dimension(w, h), true);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        imagePanel.setImage(bufferedImage);
        SwingUtilities.invokeLater(() -> swingNode.setContent(imagePanel));
        Pane pane = new BorderPane(swingNode);
        Scene scene = new Scene(pane, 400, 400);
        stage.widthProperty().addListener((observableValue, number, t1) -> imagePanel.repaintPanel());
        stage.heightProperty().addListener((observableValue, number, t1) -> imagePanel.repaintPanel());
        stage.setScene(scene);
        stage.setTitle("Scaled");
        stage.show();

    }

    /**
     * Обрисовка картинки осями.
     *
     * @param image
     * @param bgColor
     * @return
     */
    public static BufferedImage saveImg(BufferedImage image, boolean bgColor) {

        int pw = 325;
        int ph = 325;
        int iw = image.getWidth();
        int ih = image.getHeight();
        BufferedImage resizedImage = null;
        BufferedImage bi = new BufferedImage(pw, ph, image.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();

        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g2.setBackground(Color.WHITE);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, pw, ph);

        double s = Math.max((double) (iw) / pw, (double) (ih) / ph);
        double niw = (iw / s) - 90;
        double nih = (ih / s) - 90;
        double dx = (pw - niw) / 2;
        double dy = (ph - nih) / 2;
        int w = (int) niw;
        int h = (int) nih;
        int x = (int) dx;
        int y = (int) dy;

        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = genv.getDefaultScreenDevice().getDefaultConfiguration();
        Graphics2D gr = null;
        try {

            resizedImage = gc.createCompatibleImage(pw, ph);
            gr = resizedImage.createGraphics();

            gr.setComposite(AlphaComposite.Src);

            if (!bgColor) {
                gr.setBackground(Color.WHITE);
            } else {
                gr.setColor(new Color(242, 242, 243));

            }


            gr.fillRect(0, 0, pw, ph);

            int sx1, sx2, sy1, sy2; // source rectangle coordinates
            int dx1, dx2, dy1, dy2; // destination rectangle coordinates

            dx1 = x;
            dy1 = y;
            dx2 = x + w;
            dy2 = y + h;

            sx1 = 0;
            sy1 = 0;
            sx2 = iw;
            sy2 = ih;
            gr.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
            int stroke = 2;
            int line = 10;
            gr.setStroke(new BasicStroke(stroke));
            gr.setColor(Color.BLACK);
            Font font = new Font("sans-serif", Font.BOLD, 16);
            gr.setFont(font);
            FontMetrics metrics = gr.getFontMetrics(font);
            gr.drawRect(dx1 - stroke / 2, dy1 - stroke / 2, dx2 - dx1 + stroke, dy2 - dy1 + stroke);

            double iHy = ih / 4.0;
            double iHx = ih / 4.0;
            int iy = (dy2 - dy1) / 4;
            int ix = (dx2 - dx1) / 4;
            for (int j = 0; j < 5; j++) {
                int valueY = (ih - 1 - (int) (iHy * j));
                int valueX = (iw - 1 - (int) (iHx * j));

                if (valueY < 5) {
                    valueY = 0;
                    valueX = 0;
                    //вертикаль
                    gr.drawString(String.valueOf(valueY), dx1 - stroke / 2 - line - (metrics.stringWidth(String.valueOf(valueY))) - 5,
                            dy1 - 1 - 1 - stroke + iy * j + metrics.getHeight() / 2);
                    gr.drawLine(dx1 - stroke / 2 - line, dy1 - stroke / 2 + iy * j, dx1 - stroke / 2, dy1 - stroke / 2 + iy * j);

                    //горизонталь
                    gr.drawString(String.valueOf(valueX),
                            (int) (dx2 + (stroke / 2) - ix * j - metrics.stringWidth(String.valueOf(valueX)) / 2.0),
                            dy2 + stroke + line + 5 + metrics.getHeight() / 2);
                    gr.drawLine(dx2 - 1 + stroke / 2 - ix * j, dy2 + stroke / 2 + line, dx2 - 1 + stroke / 2 - ix * j, dy2 + stroke / 2);

                } else {
                    //вертикаль
                    gr.drawString(String.valueOf(valueY), dx1 - stroke / 2 - line - (metrics.stringWidth(String.valueOf(valueY))) - 5,
                            dy1 - 1 - stroke + iy * j + metrics.getHeight() / 2);
                    gr.drawLine(dx1 - stroke / 2 - line, dy1 - 1 + stroke / 2 + iy * j, dx1 - stroke / 2, dy1 - 1 + stroke / 2 + iy * j);

                    //горизонталь
                    gr.drawString(String.valueOf(valueX),
                            (int) (dx2 - 1 - (stroke / 2) - ix * j - metrics.stringWidth(String.valueOf(valueX)) / 2.0),
                            dy2 + stroke + line + 5 + metrics.getHeight() / 2);

                    gr.drawLine(dx2 - 1 - stroke / 2 - ix * j, dy2 + stroke / 2 + line, dx2 - 1 - stroke / 2 - ix * j, dy2 + stroke / 2);


                }


            }
        } finally {
            if (gr != null) {
                gr.dispose();
            }
        }
        g2.drawImage(resizedImage, 0, 0, null);
        g2.dispose();
        return bi;
    }

    /**
     * Сохранение отчета
     *
     * @param exp     эксперимента
     * @param src     узел в которомвсе картинки
     * @param pdfFile пдф файл
     * @return
     */
    public static boolean saveOrder(ExpInfo exp, Node src, File pdfFile) {
        LOG.trace("Startsaving PDF file");
        DocMaker docMaker = new DocMaker(exp, src, pdfFile);
        boolean b = docMaker.save();
        if (!b) {
            return false;
        }
        return true;
    }

    /**
     * Сохранение TXT файла с перечнем дефектов.
     *
     * @param expInfo эксперимент.
     * @param pdfFile путь к файлу.
     * @throws IOException
     */
    public static void saveTxt(ExpInfo expInfo, File pdfFile) throws IOException {
        byte[] buff = expInfo.getBuffToTXT();
        LOG.trace("Saving Txt file");
        String PATH = pdfFile.getAbsolutePath();
        String tempPATH = PATH.substring(0, PATH.length() - 4);
        String FINALPATH = tempPATH + "_bpList.txt";

        saveFileToDisk(FINALPATH, buff);

    }

    /**
     * Сохранение Exel файла
     *
     * @param service
     * @param pdfFile
     */
    public static void saveExel(ParamsService service, File pdfFile) {
        LOG.trace("Saving Exel file");
        XlsSaveClass xlsSaveClass = new XlsSaveClass();
        xlsSaveClass.saveXlFile(service, pdfFile);
    }

    /**
     * Подсчет дефектов в цинтральной зоне.
     *
     * @param frList   перечень кадров .
     * @param position позиция в перечне.
     * @param areaSize размер стороны квадрата.
     * @return
     */
    public static int bpInCentral(ArrayList<Utils.Frame> frList, int position, int areaSize) {
        Frame frame = frList.get(position);
        ArrayList<BadPoint> bpList = frame.getBpList();
        int w = frame.getSizeX();
        int h = frame.getSizeY();
        int count = 0;
        int nizIndexX = (w - areaSize) / 2;
        int verhIndexX = (nizIndexX + areaSize - 1);
        int nizIndexY = (h - areaSize) / 2;
        int verhIndexY = (nizIndexY + areaSize - 1);

        for (BadPoint bp :
                bpList) {
            if (bp.getX() >= nizIndexX && bp.getX() <= verhIndexX
                    && bp.getY() >= nizIndexY && bp.getY() <= verhIndexY) {
                count++;
            }
        }
        return count;
    }

    /**
     * Подсчет дефектности в центральной области
     *
     * @param frList   перечень
     * @param areaSize размер зоны
     * @param w        размер матрицы по ширине
     * @param h        размер матрицы по высоте
     * @return
     */
    public static int bbpInCentral(List<BadBigPoint> frList, int areaSize, int w, int h) {

        int count = 0;
        for (BadBigPoint bp :
                frList) {

            int nizIndexX = (w - areaSize) / 2;
            int verhIndexX = (nizIndexX + areaSize - 1);
            int nizIndexY = (h - areaSize) / 2;
            int verhIndexY = (nizIndexY + areaSize - 1);

            if (bp.getX() >= nizIndexX && bp.getX() <= verhIndexX &&
                    bp.getY() >= nizIndexY && bp.getY() <= verhIndexY) {
                count++;
            }
        }
        return count;
    }

    /**
     * Масштабирование рисунка
     *
     * @param src        источник BufferedImage
     * @param targetSize максимальная выстота (портрет) или ширина (альбом)
     * @return масштабированная версия BufferedImage
     */
    private static BufferedImage resize(BufferedImage src, int targetSize) {
        if (targetSize <= 0) {
            return src; //невозможно масштабировать
        }
        int targetWidth = targetSize;
        int targetHeight = targetSize;
        float ratio = ((float) src.getHeight() / (float) src.getWidth());
        if (ratio <= 1) { //определение ориентации
            targetHeight = (int) Math.ceil((float) targetWidth * ratio);
        } else { //портрет
            targetWidth = Math.round((float) targetHeight / ratio);
        }
        BufferedImage bi = new BufferedImage(targetWidth, targetHeight, src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();

        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return bi;
    }

}
