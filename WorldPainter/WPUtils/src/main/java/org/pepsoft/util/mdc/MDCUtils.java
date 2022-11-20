package org.pepsoft.util.mdc;

import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.Callable;

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

    /**
     * Execute a task returning a result with a set of key-value pairs on the {@link MDC} log context, wrapping any
     * exception thrown from the task in a {@link MDCCapturingException} to capture the full current MDC context if
     * necessary.
     *
     * @param task    The task to execute.
     * @param context An interleaved list of keys and values. Must have an even length. The keys must be
     *                {@code String}s. The values may be any type and will be converted to a string by invoking
     *                {@code toString()}. They may also be {@code null}, in which case the string {@code "null"} will be
     *                placed on the context.
     * @return The result of the task.
     */
    public static <T> T doWithMdcContext(Callable<T> task, Object... context) {
        final Set<String> keys = new HashSet<>();
        for (int i = 0; i < context.length; i += 2) {
            final String key = (String) context[i];
            final String value = String.valueOf(context[i + 1]);
            keys.add(key);
            MDC.put(key, value);
        }
        try {
            return task.call();
        } catch (Exception e) {
            // Check if the MDC context has already been captured
            boolean found = false;
            Throwable exception = e;
            do {
                if (exception instanceof MDCContextProvider) {
                    found = true;
                    break;
                }
                exception = exception.getCause();
            } while (exception != null);
            if (found) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            } else {
                throw new MDCWrappingRuntimeException(e);
            }
        } finally {
            for (String key: keys) {
                MDC.remove(key);
            }
        }
    }

    /**
     * Checks whether the MDC context has been captured somewhere on the causal chain already, and if not, wraps the
     * exception in an {@link MDCCapturingException} or {@link MDCCapturingRuntimeException}.
     *
     * @param exception The exception to check for the presence of a captured MDC context.
     * @return An exception that is guaranteed to have the MDC context of the current thread somewhere on the chain.
     */
    public static Throwable decorateWithMdcContext(Throwable exception) {
        Throwable cause = exception;
        do {
            if (cause instanceof MDCContextProvider) {
                return exception;
            }
            cause = cause.getCause();
        } while (cause != null);
        if (exception instanceof RuntimeException) {
            return new MDCWrappingRuntimeException(exception);
        } else {
            return new MDCWrappingException(exception);
        }
    }
}