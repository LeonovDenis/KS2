package ru.pelengator.API.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Класс обертка задания
 */
public class DetectorOpenTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorOpenTask.class);

    public DetectorOpenTask(DetectorDriver driver, DetectorDevice device) {
        super(driver, device);
    }

    public void open() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {

        DetectorDevice device = getDevice();

        if (device.isOpen()) {
            return;
        }

        if (device.getResolution() == null) {
            device.setResolution(device.getResolutions()[0]);
        }

        LOG.info("Opening detector {}", device.getName());

        device.open();
    }
}
