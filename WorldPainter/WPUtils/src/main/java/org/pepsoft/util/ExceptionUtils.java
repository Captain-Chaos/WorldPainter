package org.pepsoft.util;

public final class ExceptionUtils {
    private ExceptionUtils() {
        // Prevent instantiation
    }

    /**
     * Get the ultimate root cause of an exception.
     *
     * @param t The exception of which to obtain the ultimate root cause.
     * @return The ultimate root cause of the specified exception.
     */
    public static Throwable getUltimateCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Determine whether a particular exception type occurs on the chain of causes of an exception.
     *
     * @param exception     The exception of which to inspect the chain of causes.
     * @param exceptionType The type of exception for which to look.
     * @return {@code true} if the chain of causes of the specified exception contains an exception that has the
     * specified type.
     */
    public static boolean chainContains(Throwable exception, Class<? extends Throwable> exceptionType) {
        return getFromChainOfType(exception, exceptionType) != null;
    }

    /**
     * Get the first exception that has a particular type fromt he chain of causes of an exception.
     *
     * @param exception     The exception of which to inspect the chain of causes.
     * @param exceptionType The type of exception for which to look.
     * @return The first exception on the chain of causes of the specified exception which has the specified type, or
     * {@code null} if there is no such exception.
     * @param <T> The type of exception for which to look.
     */
    @SuppressWarnings("unchecked") // Guaranteed by isAssignableFrom()
    public static <T extends Throwable> T getFromChainOfType(Throwable exception, Class<T> exceptionType) {
        Throwable cause = exception;
        do {
            if (exceptionType.isAssignableFrom(cause.getClass())) {
                return (T) cause;
            }
            cause = cause.getCause();
        } while (cause != null);
        return null;
    }
}