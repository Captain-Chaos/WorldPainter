/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.jobqueue;


/**
 * A FIFO job queue where each job may only exist at most once in the queue.
 * What constitutes "the same job" is determined by the {@link #equals(java.lang.Object)}
 * and {@link #hashCode()} methods of the job itself, and is therefore the
 * responsibility of the programmer.
 *
 * @author pepijn
 */
public class UniqueJobQueue<T extends Job> {
    /**
     * Blocking.
     */
    public synchronized T takeJob() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        return queue.remove(0);
    }

    /**
     * Non-blocking.
     */
    public synchronized T pollJob() {
        if (! queue.isEmpty()) {
            T job = queue.remove(0);
            notifyAll();
            return job;
        } else {
            return null;
        }
    }
    
    /**
     * Adds a job to the front of the queue, but only if it is not on the
     * queue yet.
     * 
     * @param job The job to schedule.
     * @return <code>true</code> if the job was scheduled, <code>false</code> if
     *     it already was on the queue.
     */
    public synchronized boolean scheduleJobIfNotScheduled(T job) {
        if (! queue.contains(job)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Scheduling job " + job);
            }
            queue.add(job);
            notifyAll();
            return true;
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("NOT scheduling job " + job + " due to duplicate on queue");
            }
            return false;
        }
    }
    
    /**
     * Adds a job to the front of the queue. If the job was already on the queue
     * the existing instance is removed.
     * 
     * @param job The job to schedule.
     * @return <code>true</code> if the job already existed on the queue,
     *     <code>false</code> if it did not.
     */
    public synchronized boolean rescheduleJob(T job) {
        if (queue.contains(job)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Scheduling job " + job + ", replacing existing job");
            }
            queue.add(job);
            notifyAll();
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Scheduling job " + job);
            }
            queue.add(job);
            notifyAll();
            return false;
        }
    }
    
    /**
     * Remove a job from the queue if it has not already been executed.
     * 
     * @param job The job to remove.
     * @return <code>true</code> if the job was still on the queue and has been
     *     removed.
     */
    public synchronized boolean removeJob(T job) {
        if (queue.contains(job)) {
            queue.remove(job);
            notifyAll();
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Block until the queue is empty.
     */
    public synchronized void drain() throws InterruptedException {
        while (! queue.isEmpty()) {
            wait();
        }
    }

    /**
     * Empty the queue.
     */
    public synchronized void clear() {
        queue.clear();
        notifyAll();
    }
    
    private final HashList<T> queue = new HashList<>();
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UniqueJobQueue.class);
}