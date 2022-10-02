package ru.pelengator.API;

/**
 * Обертка ошибки блокировки детектора.
 */
public class DetectorLockException extends DetectorException {

    private static final long serialVersionUID = 1L;

    public DetectorLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public DetectorLockException(String message) {
        super(message);
    }

    public DetectorLockException(Throwable cause) {
        super(cause);
    }
}
