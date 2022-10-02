package ru.pelengator.API;

/**
 * Обертка ошибки.
 */
public class DetectorException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    public DetectorException(String message) {
        super(message);
    }

    public DetectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public DetectorException(Throwable cause) {
        super(cause);
    }

}
