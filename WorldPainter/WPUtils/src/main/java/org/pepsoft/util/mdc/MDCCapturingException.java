package org.pepsoft.util.mdc;

import org.slf4j.MDC;

import java.util.Map;

/**
 * An {@link Exception} which captures the current contents of the {@link MDC} thread local diagnostic context at the
 * moment of throwing. The captured context is available from the {@link #getMdcContext()} method.
 *
 * <p>This exception is meant for adding semantic information to the exception chain. To <em>just</em> capture the MDC
 * context, use {@link MDCWrappingException}.
 */
public class MDCCapturingException extends Exception implements MDCContextProvider {
    public MDCCapturingException(String message) {
        super(message);
        mdcContext = MDC.getCopyOfContextMap();
    }

    public MDCCapturingException(String message, Throwable cause) {
        super(message, cause);
        mdcContext = MDC.getCopyOfContextMap();
    }

    // MDCContextProvider

    public final Map<String, String> getMdcContext() {
        return mdcContext;
    }

    private final Map<String, String> mdcContext;
}