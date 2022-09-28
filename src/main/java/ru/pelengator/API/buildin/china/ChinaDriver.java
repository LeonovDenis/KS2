package ru.pelengator.API.buildin.china;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDiscoverySupport;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;
import ru.pelengator.driver.usb.Jna2;
import ru.pelengator.model.StendParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Реализация китайского детектора через USB 3.0
 */
public class ChinaDriver implements DetectorDriver, DetectorDiscoverySupport {


    private static class DetectorNewGrabberTask extends DetectorTask {

        private AtomicReference<Jna2> grabber = new AtomicReference<Jna2>();

        public DetectorNewGrabberTask(DetectorDriver driver) {
            super(driver, null);
        }

        public Jna2 newGrabber() {
            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Processor has been interrupted");
                return null;
            }
            return grabber.get();
        }

        @Override
        protected void handle() {
            grabber.set(new Jna2());
        }
    }

    private static class GetDevicesTask extends DetectorTask {

        private volatile List<DetectorDevice> devices = null;
        private volatile Jna2 grabber = null;

        public GetDevicesTask(DetectorDriver driver) {
            super(driver, null);
        }

        /**
         * Возврат устройств.
         *
         * @param grabber собственный граббер для поиска
         * @return Устройство.
         */
        public List<DetectorDevice> getDevices(Jna2 grabber) {

            this.grabber = grabber;

            try {
                process();
            } catch (InterruptedException e) {
                LOG.error("Processor has been interrupted");
                return Collections.emptyList();
            }

            return devices;
        }

        @Override
        protected void handle() {
            devices = new ArrayList<DetectorDevice>();
            devices = grabber.getDDevices(devices);

        }
    }

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ChinaDriver.class);

    private static StendParams params=null;
    private static Jna2 grabber = null;

    @Override
    public List<DetectorDevice> getDevices() {

        LOG.debug("Searching devices");

        if (grabber == null) {
            DetectorNewGrabberTask task = new DetectorNewGrabberTask(this);
            grabber = task.newGrabber();

            if (grabber == null) {
                return Collections.emptyList();
            }
        }
        LOG.debug("Found device");
        List<DetectorDevice> devices = new GetDevicesTask(this).getDevices(grabber);
        LOG.debug("Found device {}", devices);
        if (LOG.isDebugEnabled()) {
            for (DetectorDevice device : devices) {
                LOG.debug("Enabled devices {}", device.getName());
            }
        }

        return devices;
    }

    public ChinaDriver(StendParams params) {
        this.params=params;

    }

    @Override
    public long getScanInterval() {
        return DEFAULT_SCAN_INTERVAL;
    }

    @Override
    public boolean isScanPossible() {
        return true;
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }

}
