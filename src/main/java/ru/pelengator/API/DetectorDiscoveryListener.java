package ru.pelengator.API;

public interface DetectorDiscoveryListener {
    void detectorFound(DetectorDiscoveryEvent event);

    void detectorGone(DetectorDiscoveryEvent event);
}
