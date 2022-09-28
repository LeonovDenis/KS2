package ru.pelengator.charts;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.fx.overlay.CrosshairOverlayFX;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Класс вспомогательных графиков
 */
public class ModernChart {

    static JFreeChart charttt;

    /**
     * Вложенный класс панели графика
     */
    public static class MyPane extends StackPane implements ChartMouseListenerFX {

        private ChartViewer chartViewer;//окно для вывода графика
        private Crosshair xCrosshair;//перекрестие
        private Crosshair yCrosshair;//перекрестие

        /**
         * Конструктор
         *
         * @param title  Заголовок
         * @param xLable Подпись по оси Х
         * @param yLable Подпись по оси У
         * @param mass   Массив данных
         */
        public MyPane(String dataName, String title, String xLable, String yLable, Map mass) {
            XYDataset dataset = null;

            dataset = createDataset(dataName, mass);

            JFreeChart chart = createChart(dataset, title, xLable, yLable);
            charttt = chart;
            XYPlot xyPlot = chart.getXYPlot();
            chartViewer = new ChartViewer(chart);
            chartViewer.addChartMouseListener(this);
            getChildren().add(chartViewer);
            CrosshairOverlayFX crosshairOverlay = new CrosshairOverlayFX();
            xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(1f));
            xCrosshair.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1, new float[]{5.0f, 5.0f}, 0));
            xCrosshair.setLabelVisible(true);
            yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(1f));
            yCrosshair.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1, new float[]{5.0f, 5.0f}, 0));
            yCrosshair.setLabelVisible(true);
            yCrosshair.setLabelFont(new Font("Tahoma", 0, 15));
            xCrosshair.setLabelFont(new Font("Tahoma", 0, 15));
            xCrosshair.setLabelOutlineVisible(false);
            xCrosshair.setLabelXOffset(5);
            xCrosshair.setLabelYOffset(5);
            yCrosshair.setLabelOutlineVisible(false);
            yCrosshair.setLabelXOffset(5);
            yCrosshair.setLabelYOffset(5);
            xCrosshair.setLabelBackgroundPaint(new Color(0, 0, 0, 0));
            yCrosshair.setLabelBackgroundPaint(new Color(0, 0, 0, 0));
            crosshairOverlay.addDomainCrosshair(xCrosshair);
            crosshairOverlay.addRangeCrosshair(yCrosshair);

            Platform.runLater(() -> {
                chartViewer.getCanvas().addOverlay(crosshairOverlay);
            });
        }

        @Override
        public void chartMouseClicked(ChartMouseEventFX event) {
            // ignore
        }

        @Override
        public void chartMouseMoved(ChartMouseEventFX event) {
            Rectangle2D dataArea = chartViewer.getCanvas().getRenderingInfo().getPlotInfo().getDataArea();
            JFreeChart chart = event.getChart();
            XYPlot plot = (XYPlot) chart.getPlot();
            ValueAxis xAxis = plot.getDomainAxis();
            double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea,
                    RectangleEdge.BOTTOM);
            // убирает перекрестие если указатель за пределами графика
            if (!xAxis.getRange().contains(x)) {
                x = Double.NaN;
            }
            xCrosshair.setValue((int) x);


            double y = DatasetUtils.findYValue(plot.getDataset(), 0, (int) x);
            yCrosshair.setValue(y);

        }

    }

    /**
     * Создание датасета
     *
     * @param mass
     * @return
     */
    private static XYDataset createDataset(String name, Map<String, Number> mass) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        if (mass == null) {
            return dataset;
        }
        XYSeries series = new XYSeries(name);
        for (Map.Entry<String, Number> set :
                mass.entrySet()) {
            String replace = set.getKey().replace(",", ".");
            if (set.getValue().longValue() > 0) {
                series.add(Double.parseDouble(replace), set.getValue());
            }
        }
        dataset.addSeries(series);

        return dataset;
    }

    /**
     * Создание графика
     *
     * @param dataset
     * @param title
     * @param xLable
     * @param yLable
     * @return
     */
    private static JFreeChart createChart(XYDataset dataset, String title, String xLable, String yLable) {
        JFreeChart chart = ChartFactory.createXYBarChart(title, xLable, false, yLable, (IntervalXYDataset) dataset);
        return chart;
    }

    /**
     * Точка входа в класс
     *
     * @param winTitle
     * @param title
     * @param xLable
     * @param yLable
     */
    public void start(String datasetName, String winTitle, String title, String xLable, String yLable, Map mass) {
        Scene scene = new Scene(new MyPane(datasetName, title, xLable, yLable, mass), 1600, 800);
        Stage newWindow = new Stage();
        newWindow.setTitle(winTitle);
        newWindow.setScene(scene);
        newWindow.show();
    }

}
