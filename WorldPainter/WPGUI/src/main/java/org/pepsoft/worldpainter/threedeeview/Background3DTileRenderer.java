/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pepsoft.util.jobqueue.UniqueJobQueue;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;

/**
 *
 * @author pepijn
 */
public class Background3DTileRenderer extends Thread {
    public Background3DTileRenderer(Dimension dimension, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, int rotation, UniqueJobQueue<Tile3DRenderJob> jobQueue, ThreeDeeRenderManager threeDeeRenderManager) {
        super("Background 3D renderer");
        this.jobQueue = jobQueue;
        this.threeDeeRenderManager = threeDeeRenderManager;
        renderer = new Tile3DRenderer(dimension, colourScheme, biomeScheme, customBiomeManager, rotation);
        setDaemon(true);
    }

    @Override
    public void run() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Background 3D rendering thread started");
        }
        try {
            while (running) {
                synchronized (this) {
                    Tile3DRenderJob job = jobQueue.takeJob();
                    Tile tile = job.getTile();
                    renderTile(tile);
                }
            }
        } catch (InterruptedException e) {
            if (running) {
                throw new RuntimeException("Thread interrupted while waiting for render job", e);
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Background 3D rendering thread halted");
        }
    }
    
    public void halt() {
        running = false;
        interrupt();
    }
    
    /**
     * Blocks until the thread is idle (not rendering a tile)
     */
    public synchronized void waitToIdle() {
        // Do nothing (the synchronized takes care of it)
    }
    
    private void renderTile(Tile tile) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Rendering 3D view of tile " + tile);
        }
        BufferedImage image = renderer.render(tile);
        if (running) {
            threeDeeRenderManager.tileFinished(new RenderResult(tile, image));
        }
    }
    
    private final UniqueJobQueue<Tile3DRenderJob> jobQueue;
    private final ThreeDeeRenderManager threeDeeRenderManager;
    private final Tile3DRenderer renderer;
    private volatile boolean running = true, rendering;
    
    private static final Logger logger = Logger.getLogger(Background3DTileRenderer.class.getName());
}