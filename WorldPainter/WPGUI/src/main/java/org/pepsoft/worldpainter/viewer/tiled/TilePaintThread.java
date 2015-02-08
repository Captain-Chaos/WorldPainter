/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pepsoft.worldpainter.TileProvider;
import org.pepsoft.worldpainter.TileRenderer;

/**
 *
 * @author pepijn
 */
class TilePaintThread extends Thread {
    TilePaintThread(String name, TilePaintQueue jobQueue, TileRenderer tileRenderer) {
        super(name);
        this.jobQueue = jobQueue;
        this.renderer = tileRenderer;
        tileProvider = tileRenderer.getTileProvider();
    }

    @Override
    public void run() {
        try {
            TilePaintJob job = jobQueue.take();
            while (job != null) {
                System.out.println(Thread.currentThread().getName() + " took job " + job.tile.x + "," + job.tile.y);
                org.pepsoft.worldpainter.Tile wpTile = tileProvider.getTile(job.tile.x, job.tile.y);
                System.out.println("Tile is " + ((wpTile != null) ? "" : "not ") + "present in world");
                if (wpTile != null) {
                    renderer.setTile(wpTile);
                    renderer.renderTile(job.tile.image, 0, 0);
                }
                job.tile.dirty = false;
                jobQueue.jobFinished(job);
                job = jobQueue.take();
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Tile painting thread quitting due to queue shutdown");
            }
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Tile painting thread interrupted");
            }
        }
    }
    
    private final TilePaintQueue jobQueue;
    private final TileProvider tileProvider;
    private final TileRenderer renderer;
    
    private static final Logger logger = Logger.getLogger(TilePaintThread.class.getName());
}