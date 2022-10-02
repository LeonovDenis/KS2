package ru.pelengator.API;
import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

/**
 * Обертка обработчика ошибок
 *
 */
public class DetectorExceptionHandler implements UncaughtExceptionHandler {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorExceptionHandler.class);

    private static final DetectorExceptionHandler INSTANCE = new DetectorExceptionHandler();

    private DetectorExceptionHandler() {
        // синглетон
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Object context = LoggerFactory.getILoggerFactory();
        if (context instanceof NOPLoggerFactory) {
            System.err.println(String.format("Exception in thread %s", t.getName()));
            e.printStackTrace();
        } else {
            LOG.error(String.format("Exception in thread %s", t.getName()), e);
        }
    }

    public static void handle(Throwable e) {
        INSTANCE.uncaughtException(Thread.currentThread(), e);
    }

    public static final DetectorExceptionHandler getInstance() {
        return INSTANCE;
    }
}
