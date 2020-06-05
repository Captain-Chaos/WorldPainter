package org.pepsoft.util.mdc;

import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Utility methods for working with the classes and interfaces in this package.
 */
public final class MDCUtils {
    /**
     * Finds all MDC context information in the causal chain and combines them
     * into one map. If a key occurs more than once with different values, the
     * values are concatenated together, separated by commas.
     *
     * @param exception The exception for which to gather the MDC context
     *                  information.
     * @return All MDC context information from the causal chain combined
     * together. May be an empty {@code Map}, but never {@code null}.
     */
    public static Map<String, String> gatherMdcContext(Throwable exception) {
        Map<String, Set<String>> mdcContext = new HashMap<>();
        do {
            Optional.of(exception)
                    .filter(e -> e instanceof MDCContextProvider)
                    .map(e -> ((MDCContextProvider) e).getMdcContext())
                    .ifPresent(context -> context.forEach((key, value)
                            -> mdcContext.computeIfAbsent(key, k -> new HashSet<>()).add(value)));
            exception = exception.getCause();
        } while (exception != null);
        return mdcContext.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
    }
}