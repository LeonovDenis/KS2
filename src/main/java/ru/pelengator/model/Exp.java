package ru.pelengator.model;

import ru.pelengator.API.Detector;

import java.awt.image.BufferedImage;
import java.util.List;

public class Exp {
    /**
     * Коэффициент фотоэлектрической связи соседних элементов
     * %
     */
    double coefficientPc;
    /**
     * Среднее по матрице значение пороговой облученности
     */
    double thresholdIrradiance;
    /**
     * Разброс вольтовой чувствительности от среднего по матрице
     */
    double voltSensitivityDistribution;
    /**
     * Набор кадров
     */
    List<List<BufferedImage>> massiv;

    /**
     * Количество дефектных элементов
     * Общее количество+ в центральной зоне 32*32
     */
    int [] numberOfDefectiveItems;

    /**
     * Детектор
     */
    Detector detector;

    /**
     * Наименование эксперимента
     */
    String name;
}
