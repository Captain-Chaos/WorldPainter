package org.pepsoft.worldpainter.exception;

import org.pepsoft.util.mdc.MDCCapturingException;
import org.slf4j.MDC;

/**
 * A WorldPainter checked {@code Exception}, with the following features:
 *
 * <ul>
 *     <li>Captures the {@link MDC} thread diagnostic context at the moment of
 *     throwing.
 * </ul>
 */
public class WPException extends MDCCapturingException {
    public WPException() {
        // Do nothing
    }

    public WPException(String message) {
        super(message);
    }

    public WPException(String message, Throwable cause) {
        super(message, cause);
    }

    public WPException(Throwable cause) {
        super(cause);
    }
}