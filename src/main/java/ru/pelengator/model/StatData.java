package ru.pelengator.model;

import ru.pelengator.API.utils.StatisticsUtils;
import ru.pelengator.Controller;

/**
 * Статистические данные
 */
public class StatData {

    private int[][] data = null;
    private float[] dataArray = null;
    private float[] skoArray;
    private StatisticsUtils statisticsUtils;

    /**
     * Конструктор
     *
     * @param data входящий массив данных
     */
    public StatData(int[][] data) {
        this.data = data;
        init(data);
    }

    /**
     * Возврат массива СКО
     * i- Y; k- X
     *
     * @return массив СКО
     */
    public float[] getSKOArray() {
        StatisticsUtils[] statSKO = new StatisticsUtils[data.length];
        float[] statSKOfloat = new float[data.length];

        for (int i = 0; i < data.length; i++) {
            statSKO[i] = new StatisticsUtils();
            for (int k = 0; k < data[0].length; k++) {
                statSKO[i].addValue(data[i][k]);
            }
            statSKOfloat[i] = (float) statSKO[i].getStdDev() * Controller.getMASHTAB();
        }
        skoArray = statSKOfloat;
        return statSKOfloat;
    }

    /**
     * Возврат массива СКО по столбцам
     *
     * @return массив СКО
     */
    public float[] getSKOArrayHorisontal() {
        StatisticsUtils[] statSKO = new StatisticsUtils[data[0].length];
        float[] statSKOfloat = new float[data[0].length];

        for (int i = 0; i < data[0].length; i++) {
            statSKO[i] = new StatisticsUtils();
            for (int k = 0; k < data.length; k++) {
                statSKO[i].addValue(data[k][i]);
            }
            statSKOfloat[i] = (float) statSKO[i].getStdDev() * Controller.getMASHTAB();
        }
        skoArray = statSKOfloat;
        return statSKOfloat;
    }


    /**
     * Инициализация массива данных
     *
     * @param data массив данных
     */
    private void init(int[][] data) {
        int i = 0;
        statisticsUtils = new StatisticsUtils();
        dataArray = new float[data.length * data[0].length];
        for (int h = 0; h < data.length; h++) {
            for (int w = 0; w < data[0].length; w++) {
                statisticsUtils.addValue(data[h][w]);
                dataArray[i++] = data[h][w];
            }
        }
    }

    /**
     * Перевод двумерного массива в список
     *
     * @return список исходных данных
     */
    public float[] getDataArray() {
        return dataArray;
    }

    /**
     * Средний сигнал по выборке
     *
     * @return мВ
     */
    public float getMean() {
        float value = (float) (statisticsUtils.getMean() * Controller.getMASHTAB());

        return value;
    }

    /**
     * Среднее СКО по выборке
     *
     * @return мВ
     */
    public float getSKO() {
        float value;
        StatisticsUtils statisticsUtils1 = new StatisticsUtils();
        for (float data :
                skoArray) {
            statisticsUtils1.addValue((long) data);
        }
        value = (float) (statisticsUtils1.getMean());
        return value;
    }


    /**
     * Максимальный сигнал по выборке
     *
     * @return мВ
     */
    public float getMAX() {
        float value = statisticsUtils.getMax() * Controller.getMASHTAB();

        return value;
    }

    /**
     * Минимальный сигнал по выборке
     *
     * @return мВ
     */
    public float getMin() {
        float value = statisticsUtils.getMin() * Controller.getMASHTAB();

        return value;
    }

}