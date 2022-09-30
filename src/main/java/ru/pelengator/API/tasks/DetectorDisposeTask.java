package ru.pelengator.API.tasks;

import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Класс обертка задания
 */
public class DetectorDisposeTask extends DetectorTask {
    /**
     * Логгер.
     */
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
