package ru.pelengator.model;

/**
 * Подкласс для списка детекторов.
 */
public class DetectorInfo {
    /**
     * Наименование детектора.
     */
    private String detectorName;
    /**
     * Индекс детектора в списке.
     */
    private int detectorIndex;

    public DetectorInfo() {

    }

    public DetectorInfo(String detectorName, int detectorIndex) {
        this.detectorName = detectorName;
        this.detectorIndex = detectorIndex;
    }

    public String getDetectorName() {
        return detectorName;
    }

    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }

    public int getDetectorIndex() {
        return detectorIndex;
    }

    public void setDetectorIndex(int detectorIndex) {
        this.detectorIndex = detectorIndex;
    }

    @Override
    public String toString() {
        return detectorName;
    }
}
