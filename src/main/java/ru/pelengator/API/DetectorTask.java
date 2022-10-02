package ru.pelengator.API;

/**
 * Обертка для заданий, отправляемыхна детектор
 */
public abstract class DetectorTask {
    private boolean doSync = true;
    private DetectorProcessor processor = null;
    private DetectorDevice device = null;
    private Throwable throwable = null;

    public DetectorTask(boolean threadSafe, DetectorDevice device) {
        this.doSync = !threadSafe;
        this.device = device;
        this.processor = DetectorProcessor.getInstance();
    }

    public DetectorTask(DetectorDriver driver, DetectorDevice device) {
        this(driver.isThreadSafe(), device);
    }

    public DetectorTask(DetectorDevice device) {
        this(false, device);
    }

    public DetectorDevice getDevice() {
        return device;
    }

    /**
     * Обработка задачи потоком процессора.
     *
     * @throws InterruptedException когда поток был прерван
     */
    public void process() throws InterruptedException {

        boolean alreadyInSync = Thread.currentThread() instanceof DetectorProcessor.ProcessorThread;

        if (alreadyInSync) {
            handle();
        } else {
            if (doSync) {
                if (processor == null) {
                    throw new RuntimeException("Driver should be synchronized, but processor is null");
                }
                processor.process(this);
            } else {
                handle();
            }
        }
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable t) {
        this.throwable = t;
    }

    protected abstract void handle();
}
