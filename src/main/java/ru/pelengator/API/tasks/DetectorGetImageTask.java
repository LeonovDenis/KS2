package ru.pelengator.API.tasks;

import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Класс обертка задания
 */
public class DetectorGetImageTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorGetImageTask.class);

    private volatile BufferedImage image = null;

    public DetectorGetImageTask(DetectorDriver driver, DetectorDevice device) {
        super(driver, device);
    }

    public BufferedImage getImage() {

        try {
            process();
        } catch (InterruptedException e) {
            LOG.debug("Interrupted exception", e);
            return null;
        }

        return image;
    }

    @Override
    protected void handle() {

        DetectorDevice device = getDevice();
        if (!device.isOpen()) {
            return;
        }

        image = device.getImage();
    }
}
