package ru.pelengator.API;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;


/**
 * Deallocator, целью которого является высвобождение ресурсов всех устройств, когда SIGTERM
 * сигнал был обнаружен.
 */
final class DetectorDeallocator {
    private static final DetectorSignalHandler HANDLER = new DetectorSignalHandler();

    private final Detector[] detectors;

    /**
     * Этот конструктор используется внутренне для создания нового деаллокатора для
     * данного массива устройств.
     *
     * @param devices устройства, которые будут храниться в деаллокаторе.
     */
    private DetectorDeallocator(Detector[] devices) {
        this.detectors = devices;
    }

    /**
     * Сохранить устройства, которые будут быть освобождены после получения сигнала TERM.
     *
     * @param detectors массив устройств, который будет храниться в деаллокаторе
     */
    protected static void store(Detector[] detectors) {
        if (HANDLER.get() == null) {
            HANDLER.set(new DetectorDeallocator(detectors));
        } else {
            throw new IllegalStateException("Deallocator is already set!");
        }
    }

    protected static void unstore() {
        HANDLER.reset();
    }

    protected void deallocate() {
        for (Detector d : detectors) {
            try {
                d.dispose();
            } catch (Throwable t) {
                caugh(t);
            }
        }
    }

    private void caugh(Throwable t) {
        File f = new File(String.format("Detector-capture-hs-%s", System.currentTimeMillis()));
        PrintStream ps = null;
        try {
            t.printStackTrace(ps = new PrintStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

}
