package ru.pelengator.API;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * ShutdownHook выключения должен быть выполнен, когда JVM корректно завершает работу.
 * Это намерение класса предназначен только для внутреннего использования.
 */
public final class DetectorShutdownHook extends Thread {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorShutdownHook.class);

    /**
     * Количество экземпляров перехватчика выключения.
     */
    private static int number = 0;

    /**
     * Экземпляр детектора,который подлежит удалению/закрытию.
     */
    private Detector detector = null;

    /**
     * Создать новый экземпляр хука выключения.
     *
     * @param detector детектор, для которого предназначен хук
     */
    protected DetectorShutdownHook(Detector detector) {
        super("Shutdown-hook-" + (++number));
        this.detector = detector;
        this.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
    }

    @Override
    public void run() {
        LOG.info("Automatic {} deallocation", detector.getName());
        detector.dispose();
    }
}