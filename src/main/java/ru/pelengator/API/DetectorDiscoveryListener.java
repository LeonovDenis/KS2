package ru.pelengator.API;

/**
 * Интерфейс, который должны поддерживать слушатели обнаружения детекторов.
 */
public interface DetectorDiscoveryListener {
    void detectorFound(DetectorDiscoveryEvent event);

    void detectorGone(DetectorDiscoveryEvent event);
}
