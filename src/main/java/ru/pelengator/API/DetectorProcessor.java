package ru.pelengator.API;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Обработчик заданий детектора (синхронно).
 */
public class DetectorProcessor {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorProcessor.class);

    /**
     * Поток выполняет суперсинхронную обработку.
     *
     */
    public static final class ProcessorThread extends Thread {

        private static final AtomicInteger N = new AtomicInteger(0);

        public ProcessorThread(Runnable r) {
            super(r, String.format("Atomic-processor-%d", N.incrementAndGet()));
        }
    }

    /**
     * Фабрика потоков для процессора.
     *
     */
    private static final class ProcessorThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new ProcessorThread(r);
            t.setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Сердце общей системы обработки.
     * Этот класс обрабатывает все вызовы, заключенные в задачи.
     * При этом выполнение всех задач суперсинхронизируется.
     */
    private static final class AtomicProcessor implements Runnable {

        private SynchronousQueue<DetectorTask> inbound = new SynchronousQueue<DetectorTask>(true);
        private SynchronousQueue<DetectorTask> outbound = new SynchronousQueue<DetectorTask>(true);

        /**
         * Фабрика потоков для процессора.
         *
         * @param task задача для обработки
         * @throws InterruptedException, когда поток был прерван
         */
        public void process(DetectorTask task) throws InterruptedException {
            inbound.put(task);

            Throwable t = outbound.take().getThrowable();
            if (t != null) {
                throw new DetectorException("Cannot execute task", t);
            }
        }

        @Override
        public void run() {
            while (true) {
                DetectorTask t = null;
                try {
                    (t = inbound.take()).handle();
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    if (t != null) {
                        t.setThrowable(e);
                    }
                } finally {
                    if (t != null) {
                        try {
                            outbound.put(t);
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot put task into outbound queue", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Процессор запущен?
     */
    private static final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Служба исполнения.
     */
    private static ExecutorService runner = null;

    /**
     * Статический процессор.
     */
    private static final AtomicProcessor processor = new AtomicProcessor();

    /**
     * Экземпляр синглтона.
     */
    private static final DetectorProcessor INSTANCE = new DetectorProcessor();

    private DetectorProcessor() {
    }

    /**
     * Обработка одной задачи детектора.
	 *
     * @param task задача для обработки
	 * @throws InterruptedException, когда поток был прерван
	 */
    public void process(DetectorTask task) throws InterruptedException {

        if (started.compareAndSet(false, true)) {
            runner = Executors.newSingleThreadExecutor(new ProcessorThreadFactory());
            runner.execute(processor);
        }

        if (!runner.isShutdown()) {
            processor.process(task);
        } else {
            throw new RejectedExecutionException("Cannot process because processor runner has been already shut down");
        }
    }

    public void shutdown() {
        if (started.compareAndSet(true, false)) {

            LOG.debug("Shutting down detector processor");

            runner.shutdown();

            LOG.debug("Awaiting tasks termination");

            while (runner.isTerminated()) {

                try {
                    runner.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }

                runner.shutdownNow();
            }

            LOG.debug("All tasks has been terminated");
        }

    }

    public static synchronized DetectorProcessor getInstance() {
        return INSTANCE;
    }
}
