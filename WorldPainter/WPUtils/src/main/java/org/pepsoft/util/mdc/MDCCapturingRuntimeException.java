package org.pepsoft.util.mdc;

import org.slf4j.MDC;

import java.util.Map;

/**
 * A {@link RuntimeException} which captures the current contents of the
 * {@link MDC} thread local diagnostic context at the moment of throwing. The
 * captured context is available from the {@link #getMdcContext()} method.
 */
public class MDCCapturingRuntimeException extends RuntimeException implements MDCContextProvider {
    public MDCCapturingRuntimeException() {
        mdcContext = MDC.getCopyOfContextMap();
    }

    public MDCCapturingRuntimeException(String message) {
        super(message);
        mdcContext = MDC.getCopyOfContextMap();
    }

    public MDCCapturingRuntimeException(String message, Throwable cause) {
        super(message, cause);
        mdcContext = MDC.getCopyOfContextMap();
    }

    public MDCCapturingRuntimeException(Throwable cause) {
        super(cause);
        mdcContext = MDC.getCopyOfContextMap();
    }

    // MDCContextProvider

    public final Map<String, String> getMdcContext() {
        return mdcContext;
    }

    private final Map<String, String> mdcContext;
}