package ru.pelengator.API.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.Detector;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

import java.nio.ByteBuffer;



/**
 * Класс обертка задания
 */
public class DetectorReadBufferTask extends DetectorTask {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorReadBufferTask.class);
    private volatile ByteBuffer target = null;

    public DetectorReadBufferTask(DetectorDriver driver, DetectorDevice device, ByteBuffer target) {
        super(driver, device);
        this.target = target;
    }

    public ByteBuffer readBuffer() {
        try {
            process();
        } catch (InterruptedException e) {
            return null;
        }
        return target;
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

        ((DetectorDevice.BufferAccess) device).getImageBytes(target);
    }
}
