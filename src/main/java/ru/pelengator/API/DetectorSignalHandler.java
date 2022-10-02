package ru.pelengator.API;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;
import sun.misc.SignalHandler;


/**
 * Примитивный обработчик сигналов.
 * Этот класс использует недокументированные классы из
 * sun.misc.* и поэтому следует использовать его с осторожностью.
 */
@SuppressWarnings("restriction")
final class DetectorSignalHandler implements SignalHandler {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorSignalHandler.class);

    private DetectorDeallocator deallocator = null;

    private SignalHandler handler = null;

    public DetectorSignalHandler() {
        handler = Signal.handle(new Signal("TERM"), this);
    }

    @Override
    public void handle(Signal signal) {

        LOG.warn("Detected signal {} {}, calling deallocator", signal.getName(), signal.getNumber());

        // ничего не делать при "сигнале по умолчанию" или "игнорировании сигнала"
        if (handler == SIG_DFL || handler == SIG_IGN) {
            return;
        }

        try {
            deallocator.deallocate();
        } finally {
            handler.handle(signal);
        }
    }

    public void set(DetectorDeallocator deallocator) {
        this.deallocator = deallocator;
    }

    public DetectorDeallocator get() {
        return this.deallocator;
    }

    public void reset() {
        this.deallocator = null;
    }
}
