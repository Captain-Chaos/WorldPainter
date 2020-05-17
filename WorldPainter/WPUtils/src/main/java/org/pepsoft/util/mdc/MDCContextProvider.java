package org.pepsoft.util.mdc;

import org.slf4j.MDC;

import java.util.Map;

/**
 * A provider of {@link MDC} diagnostic context maps.
 */
public interface MDCContextProvider {
    /**
     * Get the diagnostic context map. May be {@code null}.
     */
    Map<String, String> getMdcContext();
}