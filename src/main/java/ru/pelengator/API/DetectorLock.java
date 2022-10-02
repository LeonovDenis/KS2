package ru.pelengator.API;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Этот класс используется как глобальная (системная) блокировка,
 * не позволяющая другим процессам использовать один и тот же детектор, пока он открыт.
 * Всякий раз, когда детектор открыт, в фоновом режиме выполняется поток, который
 * обновляет блокировку раз в 2 секунды.
 * Блокировка снимается всякий раз, когда детектор закрывается или
 * полностью высвобожден.
 * Блокировка будет оставаться не менее 2 секунд в случае, если JVM не была
 * корректно завершена (из-за сигналов SIGSEGV, SIGTERM и т. д.).
 *
 */
public class DetectorLock {

    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorLock.class);

    /**
     * Интервал обновления (мс).
     */
    public static final long INTERVAL = 2000;

    /**
     * Используется для обновления состояния блокировки.
     */
    private class LockUpdater extends Thread {

        public LockUpdater() {
            super();
            setName(String.format("Detector-lock-[%s]", detector.getName()));
            setDaemon(true);
            setUncaughtExceptionHandler(DetectorExceptionHandler.getInstance());
        }

        @Override
        public void run() {
            do {
                if (disabled.get()) {
                    return;
                }
                update();
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException e) {
                    LOG.debug("Lock updater has been interrupted");
                    return;
                }
            } while (locked.get());
        }

    }

    /**
     * Детектор,который мы будем запирать/блокировать.
     */
    private final Detector detector;

    /**
     * Поток обновления. Он будет обновлять значение блокировки с фиксированным интервалом.
     */
    private Thread updater = null;

    /**
     * Заблокирован ли детектор.
     */
    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * Блокировка полностью отключена.
     */
    private final AtomicBoolean disabled = new AtomicBoolean(false);

    /**
     * Блокировочный файл.
     */
    private final File lock;

    /**
     * Создает глобальную блокировку детектора.
     *
     * @param detector экземпляр детектора, который нужно заблокировать
     */
    protected DetectorLock(Detector detector) {
        super();
        this.detector = detector;
        this.lock = new File(System.getProperty("java.io.tmpdir"), getLockName());
        this.lock.deleteOnExit();
    }

    private String getLockName() {
        return String.format(".detector-lock-%d", Math.abs(detector.getName().hashCode()));
    }

    private void write(long value) {

        if (disabled.get()) {
            return;
        }

        String name = getLockName();

        File tmp = null;
        DataOutputStream dos = null;

        try {

            tmp = File.createTempFile(String.format("%s-tmp", name), "");
            tmp.deleteOnExit();

            dos = new DataOutputStream(new FileOutputStream(tmp));
            dos.writeLong(value);
            dos.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!locked.get()) {
            return;
        }

        if (tmp.renameTo(lock)) {

            // Операция атомарного переименования может завершиться ошибкой (в основном в Windows), поэтому мы
            // просто выпрыгиваем из метода, если он успешен или попытаться переписать содержимое

            return;
        } else {

            // Создаем файл блокировки, если он не существует

            if (!lock.exists()) {
                try {
                    if (lock.createNewFile()) {
                        LOG.info("Lock file {} for {} has been created", lock, detector);
                    } else {
                        throw new RuntimeException("Not able to create file " + lock);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            FileOutputStream fos = null;
            FileInputStream fis = null;

            int k = 0;
            int n = -1;
            byte[] buffer = new byte[8];
            boolean rewritten = false;

            // Перезаписываем содержимое временного файла для блокировки. Пробуем не более 5 раз

            synchronized (detector) {
                do {
                    try {

                        fos = new FileOutputStream(lock);
                        fis = new FileInputStream(tmp);
                        while ((n = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, n);
                        }
                        rewritten = true;

                    } catch (IOException e) {
                        LOG.debug("Not able to rewrite lock file", e);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    if (rewritten) {
                        break;
                    }
                } while (k++ < 5);
            }

            if (!rewritten) {
                throw new DetectorException("Not able to write lock file");
            }

            // Удалить временный файл

            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
        }

    }

    private long read() {

        if (disabled.get()) {
            return -1;
        }

        DataInputStream dis = null;

        long value = -1;
        boolean broken = false;

        synchronized (detector) {

            try {
                value = (dis = new DataInputStream(new FileInputStream(lock))).readLong();
            } catch (EOFException e) {
                LOG.debug("Detector lock is broken - EOF when reading long variable from stream", e);
                broken = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (broken) {
                LOG.warn("Lock file {} for {} is broken - recreating it", lock, detector);
                write(-1);
            }
        }

        return value;
    }

    private void update() {

        if (disabled.get()) {
            return;
        }

        write(System.currentTimeMillis());
    }

    /**
     * Блокировка детектора.
     */
    public void lock() {

        if (disabled.get()) {
            return;
        }

        if (isLocked()) {
            throw new DetectorLockException(String.format("Detector %s has already been locked", detector.getName()));
        }

        if (!locked.compareAndSet(false, true)) {
            return;
        }

        LOG.debug("Lock {}", detector);

        update();

        updater = new LockUpdater();
        updater.start();
    }

    /**
     * Полностью отключить блокирующий механизм.
     * После вызова этого метода блокировки не будет.
     */
    public void disable() {
        if (disabled.compareAndSet(false, true)) {
            LOG.info("Locking mechanism has been disabled in {}", detector);
            if (updater != null) {
                updater.interrupt();
            }
        }
    }

    /**
     * Разблокировать детектор.
     */
    public void unlock() {

        // ничего не делаем, когда блокировка отключена

        if (disabled.get()) {
            return;
        }

        if (!locked.compareAndSet(true, false)) {
            return;
        }

        LOG.debug("Unlock {}", detector);

        updater.interrupt();

        write(-1);

        if (!lock.delete()) {
            lock.deleteOnExit();
        }
    }

    /**
     * Проверяем заблокирован ли детектор.
     *
     * @return True, если детектор заблокирован, иначе false
     */
    public boolean isLocked() {

        // всегда возвращаем false, если блокировка отключена

        if (disabled.get()) {
            return false;
        }

        // проверяем, не заблокирован ли текущий процесс

        if (locked.get()) {
            return true;
        }

        // проверяем, не заблокирован ли другой процесс

        if (!lock.exists()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long tsp = read();

        LOG.trace("Lock timestamp {} now {} for {}", tsp, now, detector);

        if (tsp > now - INTERVAL * 2) {
            return true;
        }

        return false;
    }

    public File getLockFile() {
        return lock;
    }
}