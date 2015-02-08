/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.pepsoft.worldpainter.TileRendererFactory;

/**
 * Manages a queue of tile painting jobs
 * 
 * @author pepijn
 */
class TilePaintQueue {
    TilePaintQueue(Listener listener) {
        this.listener = listener;
    }
    
    synchronized void addJob(TilePaintJob job) {
        jobQueue.add(job);
        notify();
        System.out.println("Scheduled job " + job);
    }
    
    synchronized void cancel(TilePaintJob job) {
        if (jobQueue.contains(job)) {
            jobQueue.remove(job);
        } else {
            // This means the tile is currently being rendered. Make sure that
            // when it's done the result is thrown away
            cancelledJobs.add(job);
        }
    }
    
    synchronized TilePaintJob take() throws InterruptedException {
        while (running && jobQueue.isEmpty()) {
            wait();
        }
        return running ? jobQueue.remove() : null;
    }
    
    synchronized void start(TileRendererFactory rendererFactory) {
        if (running) {
            throw new IllegalStateException("Already running");
        }
        running = true;
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new TilePaintThread("Tile painting thread " + (i + 1), this, rendererFactory.createTileRenderer());
            threads[i].start();
        }
    }
    
    synchronized void shutdown() {
        if (! running) {
            throw new IllegalStateException("Not running");
        }
        running = false;
        notifyAll();
    }
    
    synchronized void jobFinished(TilePaintJob job) {
        if (cancelledJobs.contains(job)) {
            cancelledJobs.remove(job);
        } else if (running) {
            listener.tilePainted(job.tile);
        }
    }
    
    volatile boolean running;
    
    private final LinkedList<TilePaintJob> jobQueue = new LinkedList<TilePaintJob>();
//    private final TilePaintThread[] threads = new TilePaintThread[Math.max(Runtime.getRuntime().availableProcessors() - 1, 1)];
    private final TilePaintThread[] threads = new TilePaintThread[1];
    private final Set<TilePaintJob> cancelledJobs = new HashSet<TilePaintJob>();
    private final Listener listener;
    
    interface Listener {
        void tilePainted(Tile tile);
    }
}