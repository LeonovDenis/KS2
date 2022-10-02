package ru.pelengator.model;


import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.util.Map;
import java.util.Set;

/**
 * Класс увеличенной интерактивной гистограммы.
 */
public class SampleBarChart implements ChartMouseListenerFX {
    private static ChartViewer viewer;

    /**
     * Создаем датасет.
     *
     * @return  датасет.
     */
    private static CategoryDataset createDataset(Map<String, Number> columnKey, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Set<String> keySet = columnKey.keySet();
        String[] strings = keySet.toArray(new String[0]);
        int i = 0;
        for (Map.Entry<String, Number> map : columnKey.entrySet()) {
            if (map.getValue().doubleValue() >= 0) {
                double v0 = Double.parseDouble(strings[i++]);
                double v1 = Double.parseDouble(map.getKey());
                double v = (v1 + v0) / 2.0;
                String value = String.format("%.3e ", v).toUpperCase();
                dataset.addValue(map.getValue().doubleValue(), title, value);
            }
        }
        return dataset;
    }

    /**
     * Создаем график.
     *
     * @param dataset датасет.
     * @return График
     */
    private static JFreeChart createChart(CategoryDataset dataset, String title, String xLable, String yLable) {
        JFreeChart chart = ChartFactory.createBarChart(title, xLable, yLable, dataset);
        chart.setBackgroundPaint(Color.white);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelPaint(Color.white);
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setCategoryMargin(0.05);
        domainAxis.setMaximumCategoryLabelLines(3);
        chart.getLegend().setVisible(false);

        return chart;
    }

    /**
     * Точка входа в класс.
     *
     * @param winTitle  заголовок окна.
     * @param title     заголовок графика.
     * @param xLable    подпись по оси Х.
     * @param yLable    подпись по оси У.
     * @param columnKey карта вхождений.
     */
    public void start(String winTitle, String title, String xLable, String yLable, Map<String, Number> columnKey) {
        CategoryDataset dataset = createDataset(columnKey, title);
        JFreeChart chart = createChart(dataset, title, xLable, yLable);
        viewer = new ChartViewer(chart);
        viewer.addChartMouseListener(this);
        Scene scene = new Scene(viewer, 760, 500);
        Stage newWindow = new Stage();
        newWindow.setTitle(winTitle);
        newWindow.setScene(scene);
        newWindow.show();
    }

    @Override
    public void chartMouseClicked(ChartMouseEventFX event) {
        // ignore
    }

    @Override
    public void chartMouseMoved(ChartMouseEventFX event) {
        // ignore
    }
}
