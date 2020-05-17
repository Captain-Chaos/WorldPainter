package org.pepsoft.worldpainter.exception;

import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.slf4j.MDC;

/**
 * A WorldPainter {@code RuntimeException}, with the following features:
 *
 * <ul>
 *     <li>Captures the {@link MDC} thread diagnostic context at the moment of
 *     throwing.
 * </ul>
 */
public class WPRuntimeException extends MDCCapturingRuntimeException {
    public WPRuntimeException() {
        // Do nothing
    }

    public WPRuntimeException(String message) {
        super(message);
    }

    public WPRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WPRuntimeException(Throwable cause) {
        super(cause);
    }
}