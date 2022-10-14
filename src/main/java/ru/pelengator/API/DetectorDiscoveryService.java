package ru.pelengator.API;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Реализация сервиса обнаружения.
 */
public class DetectorDiscoveryService implements Runnable{
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorDiscoveryService.class);

    private static final class DetectorsDiscovery implements Callable<List<Detector>>, ThreadFactory {

        private final DetectorDriver driver;

        public DetectorsDiscovery(DetectorDriver driver) {
            this.driver = driver;
        }

        @Override
        public List<Detector> call() throws Exception {
            return toDetectors(driver.getDevices());
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Detector-discovery-service");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            return t;
        }
    }

    private final DetectorDriver driver;
    private final DetectorDiscoverySupport support;

    private volatile List<Detector> detectors = null;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean enabled = new AtomicBoolean(true);

    private Thread runner = null;

    protected DetectorDiscoveryService(DetectorDriver driver) {

        if (driver == null) {
            throw new IllegalArgumentException("Driver cannot be null!");
        }

        this.driver = driver;
        this.support = (DetectorDiscoverySupport) (driver instanceof DetectorDiscoverySupport ? driver : null);
    }

    private static List<Detector> toDetectors(List<DetectorDevice> devices) {
        List<Detector> detectors = new ArrayList<Detector>();
        for (DetectorDevice device : devices) {
            detectors.add(new Detector(device));
        }
        return detectors;
    }

    /**
     * Получить список устройств.
     *
     * @return Список устройств
     */
    private static List<DetectorDevice> getDevices(List<Detector> detectors) {
        List<DetectorDevice> devices = new ArrayList<DetectorDevice>();
        for (Detector detector : detectors) {
            devices.add(detector.getDevice());
        }
        return devices;
    }

    public List<Detector> getDetectors(long timeout, TimeUnit tunit) throws TimeoutException {

        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }

        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        List<Detector> tmp = null;

        synchronized (Detector.class) {

            if (detectors == null) {

                DetectorsDiscovery discovery = new DetectorsDiscovery(driver);
                ExecutorService executor = Executors.newSingleThreadExecutor(discovery);
                Future<List<Detector>> future = executor.submit(discovery);

                executor.shutdown();

                try {

                    executor.awaitTermination(timeout, tunit);

                    if (future.isDone()) {
                        detectors = future.get();
                    } else {
                        future.cancel(true);
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new DetectorException(e);
                }

                if (detectors == null) {
                    throw new TimeoutException(String.format("Detectors discovery timeout (%d ms) has been exceeded", timeout));
                }

                tmp = new ArrayList<Detector>(detectors);

                if (Detector.isHandleTermSignal()) {
                    DetectorDeallocator.store(detectors.toArray(new Detector[detectors.size()]));
                }
            }
        }

        if (tmp != null) {
            DetectorDiscoveryListener[] listeners = Detector.getDiscoveryListeners();
            for (Detector detector : tmp) {
                notifyDetectorFound(detector, listeners);
            }
        }

        return Collections.unmodifiableList(detectors);
    }

    /**
     * Поиск новых добавленных или уже удаленных детекторов.
     */
    public void scan() {

        DetectorDiscoveryListener[] listeners = Detector.getDiscoveryListeners();

        List<DetectorDevice> tmpnew = driver.getDevices();
        List<DetectorDevice> tmpold = null;

        try {
            tmpold = getDevices(getDetectors(Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            throw new DetectorException(e);
        }

        // преобразовать в связанный список из-за O(1) при операции удаления на
        // итератор против O(n) для той же операции в списке массивов

        List<DetectorDevice> oldones = new LinkedList<DetectorDevice>(tmpold);
        List<DetectorDevice> newones = new LinkedList<DetectorDevice>(tmpnew);

        Iterator<DetectorDevice> oi = oldones.iterator();
        Iterator<DetectorDevice> ni = null;

        DetectorDevice od = null; // старое устройство
        DetectorDevice nd = null; // новое устройство

        // уменьшить списки

        while (oi.hasNext()) {

            od = oi.next();
            ni = newones.iterator();

            while (ni.hasNext()) {

                nd = ni.next();

                // удалить оба элемента, если имя устройства совпадает, что
                // на самом деле означает, что устройство точно такое же

                if (nd.getName().equals(od.getName())) {
                    ni.remove();
                    oi.remove();
                    break;
                }
            }
        }

        // если остались в старых значит устройства удалены
        if (oldones.size() > 0) {

            List<Detector> notified = new ArrayList<Detector>();

            for (DetectorDevice device : oldones) {
                for (Detector detector : detectors) {
                    if (detector.getDevice().getName().equals(device.getName())) {
                        notified.add(detector);
                        break;
                    }
                }
            }

            setCurrentDetectors(tmpnew);

            for (Detector detector : notified) {
                notifyDetectorGone(detector, listeners);
                detector.dispose();
            }
        }

        // если остались в новых значит добавлены устройства
        if (newones.size() > 0) {

            setCurrentDetectors(tmpnew);

            for (DetectorDevice device : newones) {
                for (Detector detector : detectors) {
                    if (detector.getDevice().getName().equals(device.getName())) {
                        notifyDetectorFound(detector, listeners);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        // не запускать, если драйвер не поддерживает обнаружение

        if (support == null) {
            return;
        }
        if (!support.isScanPossible()) {
            return;
        }

        // ждем начальный интервал времени с тех пор, как устройства были изначально
        // обнаружено

        Object monitor = new Object();
        do {

            synchronized (monitor) {
                try {
                    monitor.wait(support.getScanInterval());
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    throw new RuntimeException("Problem waiting on monitor", e);
                }
            }

            scan();

        } while (running.get());

        LOG.debug("Detector discovery service loop has been stopped");
    }

    private void setCurrentDetectors(List<DetectorDevice> devices) {
        detectors = toDetectors(devices);
        if (Detector.isHandleTermSignal()) {
            DetectorDeallocator.unstore();
            DetectorDeallocator.store(detectors.toArray(new Detector[detectors.size()]));
        }
    }

    private static void notifyDetectorGone(Detector detector, DetectorDiscoveryListener[] listeners) {
        DetectorDiscoveryEvent event = new DetectorDiscoveryEvent(detector, DetectorDiscoveryEvent.REMOVED);
        for (DetectorDiscoveryListener l : listeners) {
            try {
                l.detectorGone(event);
            } catch (Exception e) {
                LOG.error(String.format("Detector gone, exception when calling listener %s", l.getClass()), e);
            }
        }
    }

    private static void notifyDetectorFound(Detector detector, DetectorDiscoveryListener[] listeners) {
        DetectorDiscoveryEvent event = new DetectorDiscoveryEvent(detector, DetectorDiscoveryEvent.ADDED);
        for (DetectorDiscoveryListener l : listeners) {
            try {
                l.detectorFound(event);
            } catch (Exception e) {
                LOG.error(String.format("Detector found, exception when calling listener %s", l.getClass()), e);
            }
        }
    }

    /**
     * Остановить службу обнаружения.
     */
    public void stop() {

        // возвращаем, если не запущена

        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            runner.join();
        } catch (InterruptedException e) {
            throw new DetectorException("Joint interrupted");
        }

        LOG.debug("Discovery service has been stopped");

        runner = null;
    }

    /**
     * Запускаем службу обнаружения.
     */
    public void start() {

        // если настроено не запускать, то просто возвращаем

        if (!enabled.get()) {
            LOG.info("Discovery service has been disabled and thus it will not be started");
            return;
        }

        // драйвер захвата не поддерживает обнаружение - ничего не делать

        if (support == null) {
            LOG.info("Discovery will not run - driver {} does not support this feature", driver.getClass().getSimpleName());
            return;
        }

        // возвращаем, если уже запущено

        if (!running.compareAndSet(false, true)) {
            return;
        }

        // запускаем средство запуска службы обнаружения

        runner = new Thread(this, "detector-discovery-service");
        runner.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
        runner.setDaemon(true);
        runner.start();
    }

    /**
     * Работает ли служба обнаружения?
     *
     * @return True or false
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Служба обнаружения детектора будет запущена автоматически, если она включена,
     * в противном случае, если установлено значение disabled, она никогда не запустится, даже если пользователь попытается
     * её запустить.
     *
     * @param enabled параметр, управляющий запуском обнаружения
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    /**
     * Уборка.
     */
    protected void shutdown() {

        stop();

        if (detectors == null)
            return;

        // удалить все детекторы

        Iterator<Detector> di = detectors.iterator();
        while (di.hasNext()) {
            Detector detector = di.next();
            detector.dispose();
        }

        synchronized (Detector.class) {

            // очистить список детекторов

            detectors.clear();

            // удалить детекторЫ из деаллокатора

            if (Detector.isHandleTermSignal()) {
                DetectorDeallocator.unstore();
            }
        }
    }
}
