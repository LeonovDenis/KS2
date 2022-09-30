package ru.pelengator.API.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Класс обертка задания
 */
public class DetectorCloseTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorCloseTask.class);

    public DetectorCloseTask(DetectorDriver driver, DetectorDevice device) {
        super(driver, device);
    }

    public void close() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {

        DetectorDevice device = getDevice();
        if (!device.isOpen()) {
            return;
        }

        LOG.info("Closing {}", device.getName());

        device.close();
    }

}
