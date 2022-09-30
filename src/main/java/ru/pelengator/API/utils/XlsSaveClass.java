package ru.pelengator.API.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.service.ParamsService;

import java.io.*;
import java.util.*;

/**
 * Класс для работы в Exel.
 */
public class XlsSaveClass {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(XlsSaveClass.class);

    /**
     * Чтение из файла.
     * @param fName
     */
    public static void readXlFile(String fName) {
        File myFile = new File(fName);//C://temp/Employee.xlsx
        try (FileInputStream fis = new FileInputStream(myFile);) {
            // Finds the workbook instance for XLSX file
            XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);

            // Return first sheet from the XLSX workbook
            XSSFSheet mySheet = myWorkBook.getSheetAt(0);

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = mySheet.iterator();

            // Traversing over each row of XLSX file
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // For each row, iterate through each columns
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {

                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {

                        case Cell.CELL_TYPE_STRING:
                            System.out.print(cell.getStringCellValue() + "\t");
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            System.out.print(cell.getNumericCellValue() + "\t");
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            System.out.print(cell.getBooleanCellValue() + "\t");
                            break;
                        default:
                    }
                }
            }
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Запись в файл
     * @param fName
     * @param averageSignal
     * @param shum
     * @param header
     * @param count
     * @param zakladka
     */
    public void saveXlFile(String fName, Map<Integer, Double[]> averageSignal, Map<Integer, Double[]> shum,
                           Map<Integer, Double[]> header, int count, String... zakladka) {

        String dir = "xlsx";
        new File(dir).mkdir();
        File myFile = new File(dir + File.separator + fName);

        try (FileOutputStream os = new FileOutputStream(myFile);) {
            // Finds the workbook instance for XLSX file
            XSSFWorkbook myWorkBook = new XSSFWorkbook();

            // Return first sheet from the XLSX workbook
            XSSFSheet signalSheet = myWorkBook.createSheet(zakladka[0]);
            XSSFSheet shumSheet = myWorkBook.createSheet(zakladka[1]);

            // Set to Iterate and add rows into XLS file


            Double[] headers = header.get(128);
            Row countR = signalSheet.createRow(0);
            Cell cell = countR.createCell(1);
            cell.setCellValue("Количесво отсчетов:" + count);

            Set<Integer> signalRows = averageSignal.keySet();
            Set<Integer> noiseRows = shum.keySet();

            fillRowsAndCells(averageSignal, signalSheet, signalRows);
            fillRowsAndCells(shum, shumSheet, noiseRows);

            Row header0 = signalSheet.createRow(headers.length + 1);
            Row header1 = shumSheet.createRow(headers.length + 1);
            fillHeader(header0, headers, 1);
            fillHeader(header1, headers, 1);
            // open an OutputStream to save written data into XLSX file

            myWorkBook.write(os);
        } catch (FileNotFoundException fe) {
            LOG.error("FileNotFound {}", fe);
            fe.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
            LOG.error("IOException {}", ie);
        }
        LOG.trace("Writing on XLSX file Finished ...");
    }

    /**
     * Заполнение строк и столбцов.
     * @param averageSignal
     * @param signalSheet
     * @param signalRows
     */
    private static void fillRowsAndCells(Map<Integer, Double[]> averageSignal, XSSFSheet signalSheet, Set<Integer> signalRows) {
        int rownum0 = 1;
        for (Integer key : signalRows) {
            // Creating a new Row in existing XLSX sheet
            Row row = signalSheet.createRow(rownum0++);
            Double[] objArr = averageSignal.get(key);
            int cellnum0 = 0;
            for (Double obj : objArr) {
                Cell cell = row.createCell(cellnum0++);

                cell.setCellValue(obj);

            }
        }
    }

    /**
     * Заполнение шапки.
     * @param header0
     * @param headers
     * @param i
     */
    private void fillHeader(Row header0, Double[] headers, int i) {
        int cellnumh = i;
        for (Double obj : headers) {
            Cell cell = header0.createCell(cellnumh++);
            cell.setCellValue(obj);
        }
    }

    /**
     * Создание стиля ячеек.
     * @param style
     * @return
     */
    private CellStyle createCellStyle(CellStyle style) {
        BorderStyle thin = BorderStyle.THIN;
        short black = IndexedColors.BLACK.getIndex();
        style.setWrapText(true);
        style.setTopBorderColor(black);
        style.setRightBorderColor(black);
        style.setBottomBorderColor(black);
        style.setLeftBorderColor(black);

        return style;
    }

    /**
     * Сохранение всех матиц в exel.
     *
     * @param service
     * @param pdfFile
     */
    public void saveXlFile(ParamsService service, File pdfFile) {

        String[] splitedName = pdfFile.getName().split("\\.");

        try (FileOutputStream os = new FileOutputStream(splitedName[0] + ".xlsx")) {
            XSSFWorkbook myWorkBook = new XSSFWorkbook();

            ArrayList<String> strings = new ArrayList<>();
            strings.add("Среднее арифметическое сигнала");
            strings.add("Среднее квадратичное сигнала");
            strings.add("СКО сигнала (шум)");
            strings.add("Вольтовая чувствительность");
            strings.add("Порог чувствительности");
            strings.add("Удельный порог чувствительности");
            strings.add("Обнаружительная способность");
            strings.add("Удельная обнаруж. способность");
            strings.add("NEDT");
            strings.add("Пороговая облученность");

            /**
             *Набор станиц (закладок)
             */
            ArrayList<XSSFSheet> sheets = new ArrayList<>();
            for (String s :
                    strings) {
                sheets.add(myWorkBook.createSheet(s));
            }
            /**
             * Набор матриц
             */
            List<double[][]> list = service.getList();
            /**
             * Набор map-ов матриц
             */
            ArrayList<Map<Integer, Double[]>> maps = new ArrayList<>();
            for (double[][] doubles :
                    list) {
                Map<Integer, Double[]> map = new TreeMap<>();
                fillMap(map, doubles);
                maps.add(map);
            }
            /**
             * Подписи к столбцам и строкам
             */
            Map<Integer, Double[]> header = new TreeMap<>();
            Double[] doubles = new Double[list.get(0).length];
            for (int i = 0; i < list.get(0).length; i++) {
                doubles[i] = (i) * 1.0D;
            }
            header.put(128, doubles);
            Double[] headers = header.get(128);
            int i = 0;
            /**
             * Заполнитель страниц
             */
            for (XSSFSheet sheet :
                    sheets) {
                Set<Integer> rows = maps.get(i).keySet();
                fillRowsAndCells(maps.get(i), sheet, rows);
                Row header0 = sheet.createRow(headers.length + 1);
                fillHeader(header0, headers, 1);
                i++;
            }
            /**
             * Запись в файл
             */
            myWorkBook.write(os);
        } catch (FileNotFoundException fe) {
            LOG.error("FileNotFound {}", fe);
            fe.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
            LOG.error("IOException {}", ie);
        }
        LOG.trace("Writing on XLSX file Finished ...");
    }

    /**
     * Заполнение мапы данными.
     * @param map
     * @param dataArray
     */
    private void fillMap(Map<Integer, Double[]> map, double[][] dataArray) {
        for (int i = 0; i < dataArray.length; i++) {
            Double[] objects = new Double[dataArray.length + 1];
            objects[0] = Double.valueOf(dataArray.length - 1 - i);
            for (int j = 0; j < dataArray.length; j++) {
                objects[j + 1] = dataArray[i][j];
            }
            map.put((i + 1), objects);
        }
    }

}
