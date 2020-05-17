package org.pepsoft.util.mdc;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.*;

/**
 * A variant of {@link ThreadPoolExecutor} which adds two behaviours:
 *
 * <ul><li>The {@link MDC} diagnostic context map of the thread that executes
 * the {@link #execute(Runnable)} method is propagated to the runnable task.
 * <li>If the runnable task throws an exception, it is wrapped in an
 * {@link MDCCapturingRuntimeException} to preserve a copy of the {@code MDC}
 * diagnostic context at the moment the exception occurred.
 * </ul>
 */
public class MDCThreadPoolExecutor extends ThreadPoolExecutor {
    public MDCThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public MDCThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrap(command));
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new MDCThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new MDCThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory);
    }

    private static Runnable wrap(Runnable runnable) {
        Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (mdcContextMap != null) {
                MDC.setContextMap(mdcContextMap);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (mdcContextMap != null) {
                    MDC.clear();
                }
            }
        };
    }
}