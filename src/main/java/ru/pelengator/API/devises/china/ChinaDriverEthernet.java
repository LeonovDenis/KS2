package ru.pelengator.API.devises.china;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDiscoverySupport;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;
import ru.pelengator.API.driver.Driver;
import ru.pelengator.API.driver.ethernet.EthernetDriver;
import ru.pelengator.model.StendParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Реализация китайского детектора через Ethernet
 */
public class ChinaDriverEthernet implements DetectorDriver, DetectorDiscoverySupport {

    private volatile static DetectorDriver driver= null;

    private static class DetectorNewGrabberTask extends DetectorTask {

        private AtomicReference<Driver> grabber = new AtomicReference<Driver>();

        public DetectorNewGrabberTask(DetectorDriver driver) {
            super(driver, null);
            ChinaDriverEthernet.driver=driver;
        }

        public Driver newGrabber() {
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
            grabber.set(new EthernetDriver(params,driver));
        }
    }

    private static class GetDevicesTask extends DetectorTask {

        private volatile List<DetectorDevice> devices = null;
        private volatile Driver grabber = null;

        public GetDevicesTask(DetectorDriver driver) {
            super(driver, null);
        }

        /**
         * Возврат устройств.
         *
         * @param grabber собственный граббер для поиска
         * @return Устройство.
         */
        public List<DetectorDevice> getDevices(Driver grabber) {

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
    private static Driver grabber = null;

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

        List<DetectorDevice> devices = new GetDevicesTask(this).getDevices(grabber);
        LOG.debug("Found device {}", devices);
        if (LOG.isDebugEnabled()) {
            for (DetectorDevice device : devices) {
                LOG.debug("Enabled devices {}", device.getName());
            }
        }

        return devices;
    }
    public ChinaDriverEthernet(StendParams params) {
        this.params=params;

    }

    public static Driver getGrabber() {
        return grabber;
    }

    @Override
    public long getScanInterval() {
        return DEFAULT_SCAN_INTERVAL;
    }

    @Override
    public boolean isScanPossible() {
        return false;
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }

}
