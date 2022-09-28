package ru.pelengator.API.tasks;

import ru.pelengator.API.Detector;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;

import java.nio.ByteBuffer;




public class DetectorReadBufferTask extends DetectorTask {

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
