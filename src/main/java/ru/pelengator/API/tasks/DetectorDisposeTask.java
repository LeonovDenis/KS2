package ru.pelengator.API.tasks;

import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Освобождение ресурсов устройства.
 */
public class DetectorDisposeTask extends DetectorTask {

    public DetectorDisposeTask(DetectorDriver driver, DetectorDevice device) {
        super(driver, device);
    }

    public void dispose() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {
        getDevice().dispose();
    }
}
