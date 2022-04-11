package org.pepsoft.worldpainter.util;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility methods for working with threads and concurrency.
 */
public final class ThreadUtils {
    private ThreadUtils() {
        // Don't instantiate
    }

    /**
     * Choose a thread count taking into account the number of processor cores, the number of jobs and the available
     * memory, assuming that each job will need at least approximately {@link #REQUIRED_MEMORY_PER_JOB} bytes of memory.
     *
     * @param operation The name of the operation for use in the log entry.
     * @param jobCount  The number of jobs to be executed.
     * @return The number of threads to use.
     */
    public static int chooseThreadCount(String operation, int jobCount) {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long memoryInUse = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        long maxMemoryAvailable = maxMemory - memoryInUse;
        int maxThreadsByMem = (int) (maxMemoryAvailable / REQUIRED_MEMORY_PER_JOB);
        int threadCount;
        if (System.getProperty("org.pepsoft.worldpainter.threads") != null) {
            threadCount = Math.max(Math.min(Integer.parseInt(System.getProperty("org.pepsoft.worldpainter.threads")), jobCount), 1);
        } else {
            threadCount = Math.max(Math.min(Math.min(maxThreadsByMem, runtime.availableProcessors()), jobCount), 1);
        }
        logger.info("Using " + threadCount + " thread(s) for " + operation + " (logical processors: " + runtime.availableProcessors() + ", available memory: " + (maxMemoryAvailable / 1048576L) + " MB)");
        return threadCount;
    }

    public static final long REQUIRED_MEMORY_PER_JOB = 250000000L;

    private static final Logger logger = getLogger(ThreadUtils.class);
}