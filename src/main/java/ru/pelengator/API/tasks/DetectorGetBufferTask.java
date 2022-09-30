package ru.pelengator.API.tasks;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

/**
 * Класс обертка задания
 */
public class DetectorGetBufferTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorGetBufferTask.class);

    private volatile ByteBuffer buffer = null;

    public DetectorGetBufferTask(DetectorDriver driver, DetectorDevice device) {
        super(driver, device);
    }

    public ByteBuffer getBuffer() {
        try {
            process();
        } catch (InterruptedException e) {
            LOG.debug("Image buffer request interrupted", e);
            return null;
        }
        return buffer;
    }

    @Override
    protected void handle() {

        DetectorDevice device = getDevice();
        if (!device.isOpen()) {
            return;
        }

        if (!(device instanceof DetectorDevice.BufferAccess)) {
            return;
        }

        buffer = ((DetectorDevice.BufferAccess) device).getImageBytes();
    }
}
