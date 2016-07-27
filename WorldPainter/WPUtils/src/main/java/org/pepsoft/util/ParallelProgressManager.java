/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;


import java.util.BitSet;

/**
 * A manager of parallel progress receivers, which reports to one parent
 * progress receiver, combining the progress values and managing the reporting
 * of exceptions or task completion.
 * 
 * <ol><li>Instantiate it with a parent progress receiver, and a task count (if
 * known). <strong>Note:</strong> the parent progress receiver should be thread
 * safe and not make assumptions about which threads its methods will be invoked
 * from!
 * 
 * <li>Invoke createProgressReceiver() as many times as needed.
 * <strong>Note:</strong> if the manager has not been created with a task count
 * you cannot invoke this method any more after the first task has started!
 * 
 * <li>Start the tasks in background threads and invoke {@link #join()} on the
 * manager to wait for all tasks to complete (defined as either invoking
 * {@link ProgressReceiver#done()} or {@link ProgressReceiver#exceptionThrown(Throwable)}
 * on their progress receivers).</ol>
 *
 * <p>If a task invokes {@link ProgressReceiver#exceptionThrown(Throwable)} it will
 * be reported to the parent progress receiver, and all subsequent invocations
 * on their progress receivers by any other tasks will result in an
 * {@link ProgressReceiver.OperationCancelled} exception being thrown. If any
 * more exceptions are reported these are <em>not</em> reported to the parent
 * progress receiver (instead they are logged using the java.util logging
 * framework). Also, if an exception has been reported,
 * {@link ProgressReceiver#done()} will not subsequently be invoked on the
 * parent progress receiver.
 * 
 * <p>If no exceptions are reported, {@link ProgressReceiver#done()} will be
 * invoked on the parent progress receiver after the last task has invoked it on
 * its sub progress receiver.
 * 
 * <p>All invocations on {@link ProgressReceiver#setMessage(String)} are passed
 * through unaltered to the parent progress receiver.
 * 
 * <p>If the parent progress receiver throws an <code>OperationCancelled</code>
 * exception at any time, it is stored and rethrown to every task whenever they
 * next invoke a method (that declares it) on their sub progress receivers. It
 * is immediately rethrown to the calling task.
 *
 * @author pepijn
 */
public class ParallelProgressManager {
    public ParallelProgressManager(ProgressReceiver progressReceiver) {
        this.progressReceiver = progressReceiver;
        taskCountKnown = false;
    }
    
    public ParallelProgressManager(ProgressReceiver progressReceiver, int taskCount) {
        this.progressReceiver = progressReceiver;
        this.taskCount = taskCount;
        taskCountKnown = true;
        taskProgress = new float[taskCount];
        running.set(0, taskCount);
        started = true;
    }
    
    public synchronized ProgressReceiver createProgressReceiver() {
        if ((! taskCountKnown) && started) {
            throw new IllegalStateException("Cannot create new progress receivers after tasks have started");
        }
        if (taskCountKnown && (tasksCreated == taskCount)) {
            throw new IllegalStateException("Attempt to create more sub progress receivers than indicated task count (" + taskCount + ")");
        }
        return new SubProgressReceiver(tasksCreated++);
    }
    
    public synchronized void join() throws InterruptedException {
        while (true) {
            if (! started) {
                wait();
            } else {
                if (running.isEmpty()) {
                    return;
                } else {
                    wait();
                }
            }
        }
    }
    
    public synchronized boolean isExceptionThrown() {
        return exceptionThrown;
    }

    private synchronized void setProgress(int index, float subProgress) throws ProgressReceiver.OperationCancelled {
        if (! started) {
            start();
        }
        if (cancelledException != null) {
            throw cancelledException;
        }
        taskProgress[index] = subProgress;
        float totalProgress = 0.0f;
        for (float progress: taskProgress) {
            totalProgress += progress;
        }
        try {
            progressReceiver.setProgress(totalProgress / taskCount);
        } catch (ProgressReceiver.OperationCancelled e) {
            cancelledException = e;
            throw e;
        }
    }
    
    private synchronized void exceptionThrown(int index, Throwable exception) {
        if (! started) {
            start();
        }
        exceptionThrown = true;
        if (cancelledException == null) {
            if (exception instanceof ProgressReceiver.OperationCancelled) {
                cancelledException = (ProgressReceiver.OperationCancelled) exception;
            } else {
                cancelledException = new ProgressReceiver.OperationCancelled("Operation cancelled due to exception on other thread (type: " + exception.getClass().getSimpleName() + ", message: " + exception.getMessage() + ")");
            }
        }
        running.clear(index);
        notifyAll();
        if (! exceptionReported) {
            exceptionReported = true;
            progressReceiver.exceptionThrown(exception);
        } else if (exception instanceof ProgressReceiver.OperationCancelledByUser) {
            if (logger.isDebugEnabled()) {
                logger.debug("Operation cancelled by user; not reporting to progress receiver", exception);
            }
        } else {
            logger.error("Secondary exception from parallel task; not reporting to progress receiver", exception);
        }
    }

    private synchronized void done(int index) {
        if (! started) {
            start();
        }
        running.clear(index);
        notifyAll();
        if (! exceptionReported) {
            if (running.isEmpty()) {
                progressReceiver.done();
            }
        }
    }

    private synchronized void setMessage(int index, String message) throws ProgressReceiver.OperationCancelled {
        if (! started) {
            start();
        }
        if (cancelledException != null) {
            throw cancelledException;
        }
        progressReceiver.setMessage(message);
    }

    private synchronized void checkForCancellation() throws ProgressReceiver.OperationCancelled {
        if (! started) {
            start();
        }
        if (cancelledException != null) {
            throw cancelledException;
        }
    }

    private synchronized void subProgressStarted(org.pepsoft.util.SubProgressReceiver subProgressReceiver) throws ProgressReceiver.OperationCancelled {
        if (! started) {
            start();
        }
        if (cancelledException != null) {
            throw cancelledException;
        }
        progressReceiver.subProgressStarted(subProgressReceiver);
    }

    private synchronized void start() {
        taskCount = tasksCreated;
        taskProgress = new float[taskCount];
        running.set(0, taskCount);
        started = true;
        notifyAll();
    }
    
    private final ProgressReceiver progressReceiver;
    private final boolean taskCountKnown;
    private final BitSet running = new BitSet();
    private int taskCount, tasksCreated;
    private float[] taskProgress;
    private ProgressReceiver.OperationCancelled cancelledException;
    private boolean started, exceptionThrown, exceptionReported;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParallelProgressManager.class);

    private class SubProgressReceiver implements ProgressReceiver {
        private SubProgressReceiver(int index) {
            this.index = index;
        }
        
        @Override
        public void setProgress(float progress) throws OperationCancelled {
            ParallelProgressManager.this.setProgress(index, progress);
        }

        @Override
        public void exceptionThrown(Throwable exception) {
            ParallelProgressManager.this.exceptionThrown(index, exception);
        }

        @Override
        public void done() {
            ParallelProgressManager.this.done(index);
        }

        @Override
        public void setMessage(String message) throws OperationCancelled {
            ParallelProgressManager.this.setMessage(index, message);
        }

        @Override
        public void checkForCancellation() throws OperationCancelled {
            ParallelProgressManager.this.checkForCancellation();
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void subProgressStarted(org.pepsoft.util.SubProgressReceiver subProgressReceiver) throws OperationCancelled {
            ParallelProgressManager.this.subProgressStarted(subProgressReceiver);
        }

        private final int index;
    }
}