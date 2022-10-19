package ru.pelengator.service;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.Chart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.utils.ImageUtils;
import ru.pelengator.model.ExpInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ru.pelengator.API.utils.Utils.bpInCentral;
import static ru.pelengator.App.loadFilePath;

/**
 * Заполнение форм в pdf файле.
 */
public class DocMaker {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocMaker.class);
    Map<String, Object> map;
    PDDocument pDDocument;
    PDAcroForm pDAcroForm;
    PDResources pDResources;
    String fileName = "protokol.pdf";
    File pdfFile;
    Node src;
    ExpInfo exp;

    public DocMaker(ExpInfo exp, Node src, File pdfFile) {

        map = createList(exp);
        this.pdfFile = pdfFile;
        this.src = src;
        this.exp = exp;
    }


    /**
     * Набивка полей.
     *
     * @return
     */
    private Map<String, Object> createList(ExpInfo exp) {

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("zakaz", exp.getParams().getZakaz());
        hashMap.put("dogovor", exp.getParams().getDogovor());
        hashMap.put("metodika", exp.getParams().getMetodika());
        hashMap.put("nomer_0", exp.getParams().getNomer_0());
        hashMap.put("nomer_1", exp.getParams().getNomer_0());
        hashMap.put("nomer_2", exp.getParams().getNomer_0());
        hashMap.put("nomer_3", exp.getParams().getNomer_0());
        hashMap.put("nomer", exp.getParams().getNomer());
        hashMap.put("copy", exp.getParams().getCopy());
        hashMap.put("otk", exp.getParams().getOtk());
        hashMap.put("data", exp.getParams().getData());

        //Дата
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-YYYY");
        String format = simpleDateFormat.format(new Timestamp(System.currentTimeMillis()));
        hashMap.put("time", format);


        hashMap.put("fps", String.valueOf(5.0));
        hashMap.put("int", String.valueOf(exp.getParams().getTempInt()));
        hashMap.put("ku", String.valueOf(exp.getParams().isTempKU() ? "3" : "1"));


        hashMap.put("matr", exp.getSizeX() + "*" + exp.getSizeY());
        hashMap.put("area", "900");
        hashMap.put("temp_0", "77");
        hashMap.put("temp_1", String.valueOf(exp.getParams().getTemp1()));
        hashMap.put("temp_2", String.valueOf(exp.getParams().getTemp0()));
        hashMap.put("out", "2");
        hashMap.put("count", String.valueOf(exp.getParams().getCountFrames()));

        ///////

        hashMap.put("VPD", "5.0");
        hashMap.put("VND", "GND");
        hashMap.put("VNEGOUT", "GND");
        hashMap.put("VPOSOUT", "5.0");
        hashMap.put("VDET_ADJ", "-");
        hashMap.put("VOUTREF", "1.6");
        hashMap.put("VREF", "1.6");
        hashMap.put("VOS", String.valueOf(exp.getParams().getTempVOS() / 1000.0));
        hashMap.put("VNEG", "GND");
        hashMap.put("VPOS", "5.0");

        ///////

        //1

        if (exp.getArifmeticMean() < 0) {
            hashMap.put("TXT_0_0", "---");
            hashMap.put("TXT_1_0", "---");
            hashMap.put("TXT_2_0", "---");
            hashMap.put("TXT_3_0", "---");
            hashMap.put("TXT_4_0", "---");
        } else {
            hashMap.put("TXT_0_0", exp.getParams().getTXT_0_0());
            hashMap.put("TXT_1_0", String.format(Locale.CANADA, "%.2e", exp.getArifmeticMean()).toUpperCase());
            hashMap.put("TXT_2_0", String.format(Locale.CANADA, "%.2f", exp.getParams().getArifmeticMeanPersent()));
            hashMap.put("TXT_3_0", String.valueOf(bpInCentral(exp.getFrList(), 0, 32)));
            hashMap.put("TXT_4_0", String.valueOf(exp.getFrList().get(0).getBpList().size()));
        }
        //2
        if (exp.getQuadraticMean() < 0) {
            hashMap.put("TXT_0_1", "---");
            hashMap.put("TXT_1_1", "---");
            hashMap.put("TXT_2_1", "---");
            hashMap.put("TXT_3_1", "---");
            hashMap.put("TXT_4_1", "---");
        } else {
            hashMap.put("TXT_0_1", exp.getParams().getTXT_0_1());
            hashMap.put("TXT_1_1", String.format(Locale.CANADA, "%.2e", exp.getQuadraticMean()).toUpperCase());
            hashMap.put("TXT_2_1", String.format(Locale.CANADA, "%.2f", exp.getParams().getQuadraticMeanPersent()));
            hashMap.put("TXT_3_1", String.valueOf(bpInCentral(exp.getFrList(), 1, 32)));
            hashMap.put("TXT_4_1", String.valueOf(exp.getFrList().get(1).getBpList().size()));
        }
        //3
        if (exp.getSKO() < 0) {
            hashMap.put("TXT_0_2", "---");
            hashMap.put("TXT_1_2", "---");
            hashMap.put("TXT_2_2", "---");
            hashMap.put("TXT_3_2", "---");
            hashMap.put("TXT_4_2", "---");
        } else {
            hashMap.put("TXT_0_2", exp.getParams().getTXT_0_2());
            hashMap.put("TXT_1_2", String.format(Locale.CANADA, "%.2e", exp.getSKO()).toUpperCase());
            hashMap.put("TXT_2_2", String.format(Locale.CANADA, "%.2f", exp.getParams().getSKOPersent()));
            hashMap.put("TXT_3_2", String.valueOf(bpInCentral(exp.getFrList(), 2, 32)));
            hashMap.put("TXT_4_2", String.valueOf(exp.getFrList().get(2).getBpList().size()));
        }

        //4
        if (exp.getVw() < 0) {
            hashMap.put("TXT_0_3", "---");
            hashMap.put("TXT_1_3", "---");
            hashMap.put("TXT_2_3", "---");
            hashMap.put("TXT_3_3", "---");
            hashMap.put("TXT_4_3", "---");
        } else {
            hashMap.put("TXT_0_3", exp.getParams().getTXT_0_3());
            hashMap.put("TXT_1_3", String.format(Locale.CANADA, "%.2e", exp.getVw()).toUpperCase());
            hashMap.put("TXT_2_3", String.format(Locale.CANADA, "%.2f", exp.getParams().getVwPersent()));
            hashMap.put("TXT_3_3", String.valueOf(bpInCentral(exp.getFrList(), 3, 32)));
            hashMap.put("TXT_4_3", String.valueOf(exp.getFrList().get(3).getBpList().size()));
        }
        //5
        if (exp.getPorog() < 0) {
            hashMap.put("TXT_0_4", "---");
            hashMap.put("TXT_1_4", "---");
            hashMap.put("TXT_2_4", "---");
            hashMap.put("TXT_3_4", "---");
            hashMap.put("TXT_4_4", "---");
        } else {
            hashMap.put("TXT_0_4", exp.getParams().getTXT_0_4());
            hashMap.put("TXT_1_4", String.format(Locale.CANADA, "%.2e", exp.getPorog()).toUpperCase());
            hashMap.put("TXT_2_4", String.format(Locale.CANADA, "%.2f", exp.getParams().getPorogPersent()));
            hashMap.put("TXT_3_4", String.valueOf(bpInCentral(exp.getFrList(), 4, 32)));
            hashMap.put("TXT_4_4", String.valueOf(exp.getFrList().get(4).getBpList().size()));
        }
        //6
        if (exp.getPorogStar() < 0) {
            hashMap.put("TXT_0_5", "---");
            hashMap.put("TXT_1_5", "---");
            hashMap.put("TXT_2_5", "---");
            hashMap.put("TXT_3_5", "---");
            hashMap.put("TXT_4_5", "---");
        } else {
            hashMap.put("TXT_0_5", exp.getParams().getTXT_0_5());
            hashMap.put("TXT_1_5", String.format(Locale.CANADA, "%.2e", exp.getPorogStar()).toUpperCase());
            hashMap.put("TXT_2_5", String.format(Locale.CANADA, "%.2f", exp.getParams().getPorogStarPersent()));
            hashMap.put("TXT_3_5", String.valueOf(bpInCentral(exp.getFrList(), 5, 32)));
            hashMap.put("TXT_4_5", String.valueOf(exp.getFrList().get(5).getBpList().size()));
        }
        //7
        if (exp.getDetectivity() < 0) {
            hashMap.put("TXT_0_6", "---");
            hashMap.put("TXT_1_6", "---");
            hashMap.put("TXT_2_6", "---");
            hashMap.put("TXT_3_6", "---");
            hashMap.put("TXT_4_6", "---");
        } else {
            hashMap.put("TXT_0_6", exp.getParams().getTXT_0_6());
            hashMap.put("TXT_1_6", String.format(Locale.CANADA, "%.2e", exp.getDetectivity()).toUpperCase());
            hashMap.put("TXT_2_6", String.format(Locale.CANADA, "%.2f", exp.getParams().getDetectivityPersent()));
            hashMap.put("TXT_3_6", String.valueOf(bpInCentral(exp.getFrList(), 6, 32)));
            hashMap.put("TXT_4_6", String.valueOf(exp.getFrList().get(6).getBpList().size()));
        }
        //8
        if (exp.getDetectivityStar() < 0) {
            hashMap.put("TXT_0_7", "---");
            hashMap.put("TXT_1_7", "---");
            hashMap.put("TXT_2_7", "---");
            hashMap.put("TXT_3_7", "---");
            hashMap.put("TXT_4_7", "---");
        } else {
            hashMap.put("TXT_0_7", exp.getParams().getTXT_0_7());
            hashMap.put("TXT_1_7", String.format(Locale.CANADA, "%.2e", exp.getDetectivityStar()).toUpperCase());
            hashMap.put("TXT_2_7", String.format(Locale.CANADA, "%.2f", exp.getParams().getDetectivityStarPersent()));
            hashMap.put("TXT_3_7", String.valueOf(bpInCentral(exp.getFrList(), 7, 32)));
            hashMap.put("TXT_4_7", String.valueOf(exp.getFrList().get(7).getBpList().size()));
        }
        //9
        if (exp.getNETD() < 0) {
            hashMap.put("TXT_0_8", "---");
            hashMap.put("TXT_1_8", "---");
            hashMap.put("TXT_2_8", "---");
            hashMap.put("TXT_3_8", "---");
            hashMap.put("TXT_4_8", "---");
        } else {
            hashMap.put("TXT_0_8", exp.getParams().getTXT_0_8());
            hashMap.put("TXT_1_8", String.format(Locale.CANADA, "%.2e", exp.getNETD()).toUpperCase());
            hashMap.put("TXT_2_8", String.format(Locale.CANADA, "%.2f", exp.getParams().getNETDPersent()));
            hashMap.put("TXT_3_8", String.valueOf(bpInCentral(exp.getFrList(), 8, 32)));
            hashMap.put("TXT_4_8", String.valueOf(exp.getFrList().get(8).getBpList().size()));
        }
        //10
        if (exp.getExposure() < 0) {
            hashMap.put("TXT_0_9", "---");
            hashMap.put("TXT_1_9", "---");
            hashMap.put("TXT_2_9", "---");
            hashMap.put("TXT_3_9", "---");
            hashMap.put("TXT_4_9", "---");
        } else {
            hashMap.put("TXT_0_9", exp.getParams().getTXT_0_9());
            hashMap.put("TXT_1_9", String.format(Locale.CANADA, "%.2e", exp.getExposure()).toUpperCase());
            hashMap.put("TXT_2_9", String.format(Locale.CANADA, "%.2f", exp.getParams().getExposurePersent()));
            hashMap.put("TXT_3_9", String.valueOf(bpInCentral(exp.getFrList(), 9, 32)));
            hashMap.put("TXT_4_9", String.valueOf(exp.getFrList().get(9).getBpList().size()));
        }

        hashMap.put("WITHBP", exp.isWithDefPx() ? "Примечание - Результаты приведены с учетом дефектных элементов." :
                "Примечание - Результаты приведены без учета дефектных элементов.");

        return hashMap;
    }

    /**
     * Флаг подчистки форм из файла.
     */
    private boolean cleadForms = false;

    /**
     * Метод заполнения отчета
     *
     * @return
     */
    public boolean savePDF() {
        try {
            String path = loadFilePath(fileName);
            File file = new File(path);
            this.pDDocument = PDDocument.load(file);
            this.pDAcroForm = pDDocument.getDocumentCatalog().getAcroForm();
            this.pDResources = pDAcroForm.getDefaultResources();

            List<String> fontNames = prepareFont(pDDocument,
                    Arrays.asList(loadTrueTypeFont(pDDocument, "ARIALUNI.TTF"),
                            PDType1Font.HELVETICA_BOLD));


            for (Map.Entry<String, Object> item : map.entrySet()) {
                String key = item.getKey();
                PDField field = pDAcroForm.getField(key);
                if (field != null) {
                    LOG.debug("Form field with placeholder name: '" + key + "' found");

                    if (field instanceof PDTextField) {
                        LOG.debug("(type: " + field.getClass().getSimpleName() + ")");
                        saveField(key, (String) item.getValue(), fontNames);
                        LOG.debug("value is set to: '" + item.getValue() + "'");

                    } else if (field instanceof PDPushButton) {
                        System.out.println("(type: " + field.getClass().getSimpleName() + ")");
                        PDPushButton pdPushButton = (PDPushButton) field;
                        convertChartToImage(key, 4 + Integer.parseInt(String.valueOf(key.charAt(key.length() - 1))), (JFreeChart) item.getValue());
                    } else {
                        LOG.error("Unexpected form field type found with placeholder name: '" + key + "'");
                    }
                } else {
                    LOG.error("No field found with name:" + key);
                }
            }
            if (cleadForms) {
                pDAcroForm.flatten();// если нужно убрать отсатки форм
            }
            pDDocument.save(pdfFile);
            pDDocument.close();
        } catch (IOException e) {
            LOG.error("Error while saving {}", e);
            return false;
        }
        return true;
    }

    /**
     * Сохранение поля.
     *
     * @param name  имя поля.
     * @param value Значение.
     * @throws IOException
     */
    public void saveField(String name, String value, List<String> fontNames) {

        PDField field = pDAcroForm.getField(name);
        COSDictionary dict = field.getCOSObject();
        COSString defaultAppearance = (COSString) dict
                .getDictionaryObject(COSName.DA);
        if (defaultAppearance != null) {
            String appearanceStringstring = defaultAppearance.getString();
            String[] splits = appearanceStringstring.split(" ", 2);
            StringBuilder stringBuilder = new StringBuilder("/");
            stringBuilder.append(fontNames.get(0)).append(" ").append(splits[1]);
            dict.setString(COSName.DA,stringBuilder.toString());
        }
        pDAcroForm.setNeedAppearances(true);

        try {
            field.setValue(value);
        } catch (IOException e) {
            LOG.error("Error in typing fiels {} value {}", name, value);
            throw new RuntimeException(e);
        }
        LOG.debug("saved " + name + ":" + value);
    }

    public void convertChartToImage(String name, int pageNumb, JFreeChart chart) throws IOException {

        chart.getPlot().setBackgroundPaint(Color.lightGray);
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(image, chart, 1600,
                800);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(pDDocument, image.toByteArray(),
                "myImage.jpg");
        setField(pDDocument, name, pageNumb, pdImage);
        LOG.debug("Image inserted Successfully.");
    }

    /**
     * Вставка картинки в форму.
     *
     * @param document документ.
     * @param name     имя формы.
     * @param page     номер страницы с 0.
     * @param image    картинка.
     * @throws IOException
     */
    public void setField(PDDocument document, String name, int page, PDImageXObject image)
            throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        PDField field = acroForm.getField(name);
        if (field != null) {
            PDRectangle rectangle = getFieldArea(field);
            float height = rectangle.getHeight();
            float width = rectangle.getWidth();
            float x = rectangle.getLowerLeftX();
            float y = rectangle.getLowerLeftY();
            try (PDPageContentStream contentStream = new PDPageContentStream(document,
                    document.getPage(page), PDPageContentStream.AppendMode.APPEND, true)) {
                contentStream.drawImage(image, x, y, width, height);
            }
        }
    }

    /**
     * Получение габаритов поля.
     *
     * @param field имя поля.
     * @return
     */
    private PDRectangle getFieldArea(PDField field) {
        COSDictionary fieldDict = field.getCOSObject();
        COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
        return new PDRectangle(fieldAreaArray);
    }

    public boolean saveImages() {
        PDDocument pDDocument = null;
        LOG.trace("Startsaving parse images to PDF file");
        try {
            pDDocument = PDDocument.load(pdfFile);
            PDDocument finalPDDocument = pDDocument;
            ArrayList<PDImageXObject> list = new ArrayList<>();
            AtomicBoolean flag = new AtomicBoolean(false);
            Platform.runLater(() -> {
                try {
                    VBox vbox = (VBox) src;
                    ObservableList<Node> children = vbox.getChildren();
                    Node[] nodes = children.toArray(new Node[0]);
                    for (int i = 0; i < nodes.length; i = i + 3) {
                        BufferedImage bufImage;
                        if (nodes[i] instanceof HBox) {
                            HBox node = (HBox) nodes[i];
                            Node chartNode = node.getChildren().get(0);
                            if (chartNode instanceof Chart) {
                                BarChart<String, Number> bar_chart = (BarChart<String, Number>) chartNode;
                                bar_chart.getStylesheets().add(this.getClass().getResource("chart.css").toExternalForm());
                                bufImage = SwingFXUtils.fromFXImage(nodes[i].snapshot(null, null), null);
                                bar_chart.getStylesheets().clear();
                            } else {
                                bufImage = SwingFXUtils.fromFXImage(nodes[i].snapshot(null, null), null);
                            }
                        } else {
                            bufImage = SwingFXUtils.fromFXImage(nodes[i].snapshot(null, null), null);
                        }

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            ImageIO.write(bufImage, ImageUtils.FORMAT_PNG, bos);
                            bos.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        PDImageXObject pdImage = null;
                        pdImage = PDImageXObject.createFromByteArray(finalPDDocument, bos.toByteArray(),
                                nodes[i].getId());
                        list.add(pdImage);
                        bos.close();
                    }
                } catch (IOException e) {
                    LOG.error("ERROR {}", e.getCause());
                } finally {
                    flag.set(true);
                }
            });
            while (!flag.get()) {
                TimeUnit.MILLISECONDS.sleep(500);
            }

            for (BufferedImage bi :
                    exp.getScList()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(bi, ImageUtils.FORMAT_PNG, bos);
                    bos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                PDImageXObject pdImage = null;
                pdImage = PDImageXObject.createFromByteArray(finalPDDocument, bos.toByteArray(),
                        "");
                list.add(pdImage);
                bos.close();
            }

            int listSize = list.size();
            float x = 30;
            float y = 0;
            float k = 0;
            PDPage tempPage = pDDocument.getPage(3);
            for (int i = 0; i < listSize / 2; i++) {
                //   if (i % 2 == 0) {
                PDFCloneUtility cloner = new PDFCloneUtility(pDDocument);
                COSDictionary pageDictionary = (COSDictionary) cloner.cloneForNewDocument(tempPage);
                PDPage page2 = new PDPage(pageDictionary);
                pDDocument.addPage(page2);
                y = page2.getMediaBox().getHeight() - 150;
                k = page2.getMediaBox().getWidth();
                //   }

                PDImageXObject img = list.get(i);
                float height = img.getHeight();
                float width = img.getWidth();
                try (PDPageContentStream contentStream = new PDPageContentStream(pDDocument,
                        pDDocument.getPage(pDDocument.getNumberOfPages() - 1),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.drawImage(img, (k - width) / 2, y - height, width, height);
                }
                y = y - height;

                PDImageXObject img2 = list.get(i + listSize / 2);
                height = img2.getHeight();
                width = img2.getWidth();
                try (PDPageContentStream contentStream = new PDPageContentStream(pDDocument,
                        pDDocument.getPage(pDDocument.getNumberOfPages() - 1),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.drawImage(img2, (k - width) / 2, y - height, width, height);
                }

            }
            if (listSize % 2 == 1) {
                //Итоговая страница 4
                PDPage page = pDDocument.getPage(3);
                y = page.getMediaBox().getHeight() - 150;
                k = page.getMediaBox().getWidth();
                //   }

                float height = 100;
                float width = 100;
                try (PDPageContentStream contentStream = new PDPageContentStream(pDDocument,
                        pDDocument.getPage(3),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.beginText();

                    PDFont font = PDType0Font.load(pDDocument,
                            this.getClass().getResourceAsStream("ARIALUNI.TTF"));
                    contentStream.setFont(font, 12);
                    contentStream.setLeading(14.5f);
                    contentStream.newLineAtOffset(150, 680);
                    String text1 = "Дефектные элементы по всем выбранным параметрам";
                    contentStream.showText(text1);
                    contentStream.newLineAtOffset(20, -15);
                    contentStream.setFont(font, 10);
                    String text2 = "Всего дефектных элементов в центральной зоне 32*32 px: " + exp.getBpInCenter();
                    contentStream.showText(text2);
                    contentStream.newLine();

                    String text3 = String.format("Всего дефектных элементов в зоне %d*%d px: %d",
                            exp.getSizeX(), exp.getSizeY(), exp.getBpAll());
                    contentStream.showText(text3);

                    byte[] bytes = exp.getBuffToTXT();

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));) {
                        float h = 10.5f;
                        contentStream.setLeading(h);

                        contentStream.setFont(font, 8);
                        contentStream.newLineAtOffset(-90, -350);
                        String line;
                        int count = 0;
                        for (int i = 0; i < 2; i++) {

                            while ((line = br.readLine()) != null && (count++) < 50) {
                                contentStream.newLine();
                                contentStream.showText(line);
                                if ((count) > 24 && i == 0) {
                                    break;
                                }
                            }
                            contentStream.newLineAtOffset(260, h * 25);
                        }

                    } catch (Exception e) {
                        LOG.error("Error while printing list pdf");
                    }

                    contentStream.endText();
                }

                y = 320;
                PDImageXObject img = list.get(listSize - 1);
                height = img.getHeight();
                width = img.getWidth();
                try (PDPageContentStream contentStream = new PDPageContentStream(pDDocument,
                        pDDocument.getPage(3),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.drawImage(img, (k - width) / 2, y, width, height);

                }
            } else {

                pDDocument.removePage(3);
            }
            pDDocument.save(pdfFile);
            pDDocument.close();

        } catch (
                Exception e) {
            LOG.error("Exeption in images {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Добавление шрифтов в документ
     *
     * @param _pdfDocument документ
     * @param fonts        список шрифтов
     * @return перечень шрифтов
     * @throws IOException
     */
    public List<String> prepareFont(PDDocument _pdfDocument, List<PDFont> fonts) throws IOException {
        PDDocumentCatalog docCatalog = _pdfDocument.getDocumentCatalog();
        PDAcroForm acroForm = docCatalog.getAcroForm();
        PDResources res = acroForm.getDefaultResources();
        if (res == null)
            res = new PDResources();
        List<String> fontNames = new ArrayList<String>();
        for (PDFont font : fonts) {
            fontNames.add(res.add(font).getName());
        }
        acroForm.setDefaultResources(res);
        return fontNames;
    }

    /**
     * Подгрузка шрифта
     *
     * @param _pdfDocument документ
     * @param resourceName наименование шрифта
     * @return обернутый шрифт
     * @throws IOException
     */
    public PDFont loadTrueTypeFont(PDDocument _pdfDocument, String resourceName) throws IOException {
        try (InputStream fontStream = getClass().getResourceAsStream(resourceName);) {
            return PDTrueTypeFont.loadTTF(_pdfDocument, fontStream);
        }
    }

}
