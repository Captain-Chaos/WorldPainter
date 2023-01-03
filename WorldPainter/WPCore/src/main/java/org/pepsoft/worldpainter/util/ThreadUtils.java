package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.Configuration;
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
        final Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        final long totalMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long memoryInUse = totalMemory - freeMemory;
        final long maxMemory = runtime.maxMemory();
        final long maxMemoryAvailable = maxMemory - memoryInUse;
        final int threadCount;
        final String sysProp = System.getProperty("org.pepsoft.worldpainter.threads");
        final Integer configProp = (Configuration.getInstance() != null) ? Configuration.getInstance().getMaxThreadCount() : null;
        if (sysProp != null) {
            threadCount = Math.max(Math.min(Integer.parseInt(sysProp), jobCount), 1);
            logger.info("Using " + threadCount + " thread(s) for " + operation + " (max. thread count source: org.pepsoft.worldpainter.threads advanced setting set to " + sysProp + ")");
        } else if (configProp != null) {
            threadCount = Math.max(Math.min(configProp, jobCount), 1);
            logger.info("Using " + threadCount + " thread(s) for " + operation + " (max. thread count source: max. thread count in preferences set to " + configProp + ")");
        } else {
            final int maxThreadsByMem = (int) (maxMemoryAvailable / REQUIRED_MEMORY_PER_JOB);
            threadCount = Math.max(Math.min(Math.min(maxThreadsByMem, runtime.availableProcessors()), jobCount), 1);
            logger.info("Using " + threadCount + " thread(s) for " + operation + " (max. thread count source: logical processors: " + runtime.availableProcessors() + ", available memory: " + (maxMemoryAvailable / 1048576L) + " MB)");
        }
        mostRecentThreadCount = threadCount;
        return threadCount;
    }

    /**
     * Get the value most recently returned by {@link #chooseThreadCount(String, int)}, if any.
     */
    public static Integer getMostRecentThreadCount() {
        return mostRecentThreadCount;
    }

    public static final long REQUIRED_MEMORY_PER_JOB = 250000000L;

    private static final Logger logger = getLogger(ThreadUtils.class);
    private static volatile Integer mostRecentThreadCount;
}