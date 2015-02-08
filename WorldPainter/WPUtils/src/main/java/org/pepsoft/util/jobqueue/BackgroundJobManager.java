/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util.jobqueue;

/**
 *
 * @author SchmitzP
 */
public final class BackgroundJobManager {
    public BackgroundJobManager(int priorities) {
        this(priorities, Math.max(Runtime.getRuntime().availableProcessors() - 1, 1));
    }
    
    public BackgroundJobManager(int priorities, int threads) {
        if ((priorities < 1) || (threads < 1)) {
            throw new IllegalArgumentException();
        }
        queues = new UniqueJobQueue[priorities];
        this.threads = new BackgroundJobThread[threads];
    }
    
    public void submitJob(Job job) {
        submitJob(job, 1);
    }
    
    public void submitJob(Job job, int priority) {
        if (job == null) {
            throw new NullPointerException();
        }
        if ((priority < 1) || (priority >= queues.length)) {
            throw new IllegalArgumentException();
        }
        queues[priority - 1].scheduleJobIfNotScheduled(job);
    }
    
    public void insertJob(Job job) {
        insertJob(job, 1);
    }
    
    public void insertJob(Job job, int priority) {
        if (job == null) {
            throw new NullPointerException();
        }
        if ((priority < 1) || (priority >= queues.length)) {
            throw new IllegalArgumentException();
        }
        queues[priority - 1].rescheduleJob(job);
    }
    
    public void removeJob(Job job) {
        for (UniqueJobQueue<Job> queue: queues) {
            if (queue.removeJob(job)) {
                return;
            }
        }
    }
    
    public void start() {
        
    }
    
    public void stop() {
        
    }

    final Object queueLock = new Object();
    boolean running = true;
    
    private final UniqueJobQueue<Job>[] queues;
    private final BackgroundJobThread[] threads;
    
    class BackgroundJobThread extends Thread {
        @Override
        public void run() {
            while (running) {
                Job job = null;
                synchronized(queueLock) {
                    do {
                        for (UniqueJobQueue<Job> queue: queues) {
                            job = queue.pollJob();
                            if (job != null) {
                                break;
                            }
                        }
                    } while (running && (job == null));
                }
            }
        }
    }
}