package ru.pelengator.API.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Вспомогательный класс статистики
 */
public class StatisticsUtilsDouble {
    private static final String DEFAULT_FORMAT = "0.###";
    private static final NumberFormat FORMATTER = new DecimalFormat(DEFAULT_FORMAT);

    private double sum;
    private double squares;
    private double count;
    private double max;
    private double min;
    private double last;
    private long failureCount;
    private long resetCount;
    private String lastFailureReason;

    public StatisticsUtilsDouble() {
        reset();
    }

    public synchronized void addFailure(String reason) {
        this.lastFailureReason = reason;
        this.failureCount++;
    }

    public synchronized void addValue(double x) {
        sum += x;
        squares += x * x;
        min = ((x < min) ? x : min);
        max = ((x > max) ? x : max);
        last = x;
        ++count;
        if (squares < 0L) {
            reset();
        }
    }

    public synchronized void reset() {
        sum = 0L;
        squares = 0L;
        count = 0L;
        max = Double.NEGATIVE_INFINITY;
        min = Double.POSITIVE_INFINITY;
        last = 0L;
        this.resetCount++;
    }

    public synchronized double getMean() {
        double mean = 0.0;
        if (count > 0L) {
            mean = (double) sum / count;
        }
        return mean;
    }
    public synchronized double getSK() {
        double sk = 0.0;
        if (count > 0L) {
            sk = Math.sqrt(squares / count);
        }
        return sk;
    }

    public synchronized double getVariance() {
        double variance = 0.0;
        if (count > 1L) {
            variance = (squares - (double) sum * sum / count) / (count);
        }
        return variance;
    }

    public synchronized double getStdDev() {
        return Math.sqrt(this.getVariance());
    }

    public synchronized double getMax() {
        return max;
    }

    public synchronized double getMin() {
        return min;
    }

    public String toString() {
        return "StatisticsUtils{" +
                "sum=" + sum +
                ", min=" + min +
                ", max=" + max +
                ", last=" + last +
                ", squares=" + squares +
                ", count=" + count +
                ", variance=" + FORMATTER.format(getVariance()) +
                ", mean=" + FORMATTER.format(getMean()) +
                ", dev=" + FORMATTER.format(getStdDev()) +
                '}';
    }
}

