package ru.pelengator.API.devises.china;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.API.DetectorDevice;
import ru.pelengator.API.DetectorDiscoverySupport;
import ru.pelengator.API.DetectorDriver;
import ru.pelengator.API.DetectorTask;
import ru.pelengator.API.driver.Driver;
import ru.pelengator.API.driver.usb.Jna2;
import ru.pelengator.model.StendParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Реализация китайского детектора через USB 3.0.
 */
public class ChinaDriver implements DetectorDriver, DetectorDiscoverySupport {

    /**
     * Задание на создание драйвера.
     */
    private static class DetectorNewGrabberTask extends DetectorTask {

        private AtomicReference<Driver> grabber = new AtomicReference<>();

        public DetectorNewGrabberTask(DetectorDriver driver) {
            super(driver, null);
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
            grabber.set(new Jna2());
        }
    }

    /**
     * Задание на получение списка найденных устройств.
     */
    private static class GetDevicesTask extends DetectorTask {

        private volatile List<DetectorDevice> devices = null;
        private volatile Driver grabber = null;

        public GetDevicesTask(DetectorDriver driver) {
            super(driver, null);
        }

        /**
         * Возврат списка устройств.
         *
         * @param grabber собственный граббер для поиска.
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
    /**
     * Параметры стенда.
     */
    private static StendParams params = null;
    /**
     * Драйвер.
     */
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
        LOG.debug("Founding devices");
        List<DetectorDevice> devices = new GetDevicesTask(this).getDevices(grabber);
        LOG.debug("Found device {}", Arrays.asList(devices));
        if (LOG.isDebugEnabled()) {
            for (DetectorDevice device : devices) {
                LOG.debug("Enabled devices {}", device.getName());
            }
        }
        return devices;
    }

    /**
     * Конструктор.
     * @param params стендовые параметры
     */
    public ChinaDriver(StendParams params) {
        this.params = params;

    }

    /**
     * Возврат значения интервала поиска устройств.
     * @return
     */
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
