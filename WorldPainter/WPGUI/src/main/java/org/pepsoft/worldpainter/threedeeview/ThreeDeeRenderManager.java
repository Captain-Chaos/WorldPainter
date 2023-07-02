/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import org.pepsoft.util.jobqueue.UniqueJobQueue;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.threedeeview.Tile3DRenderer.LayerVisibilityMode;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author pepijn
 */
public class ThreeDeeRenderManager {
    public ThreeDeeRenderManager(Dimension dimension, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int rotation) {
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        this.rotation = rotation;
    }
    
    /**
     * Add the tile to the start of the queue of tiles to be rendered.
     * 
     * @param tile The tile to be rendered.
     */
    public synchronized void renderTile(Tile tile) {
//        System.out.println("Queueing tile " + tile + " for rendering");
        if (jobQueue == null) {
            startThreads();
        }
        jobQueue.scheduleJobIfNotScheduled(new Tile3DRenderJob(tile));
    }
    
    /**
     * Collect the tiles rendered so far, if any. May be empty.
     * 
     * @return The tiles rendered so far, if any. May be empty.
     */
    @SuppressWarnings("unchecked") // Guaranteed by Java
    public synchronized Set<RenderResult> getRenderedTiles() {
        Set<RenderResult> rc = results;
        results = new HashSet<>();
        return rc;
    }

    /**
     * Blocks until all tiles currently on the queue are rendered.
     */
    public synchronized void renderAllTiles() throws InterruptedException {
        if (jobQueue != null) {
            jobQueue.drain();
            for (Background3DTileRenderer renderThread: renderThreads) {
                renderThread.waitToIdle();
            }
        }
    }
    
    public synchronized void stop() {
        if (renderThreads != null) {
            for (Background3DTileRenderer renderThread : renderThreads) {
                renderThread.halt();
            }
        }
        renderThreads = null;
        jobQueue = null;
        results.clear();
    }

    synchronized void tileFinished(RenderResult renderResult) {
        results.add(renderResult);
    }

    public void setLayerVisibility(LayerVisibilityMode layerVisibility) {
        this.layerVisibility = layerVisibility;
    }

    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        this.hiddenLayers = hiddenLayers;
    }

    private void startThreads() {
        jobQueue = new UniqueJobQueue<>();
        int noOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        renderThreads = new Background3DTileRenderer[noOfThreads];
        for (int i = 0; i < noOfThreads; i++) {
            renderThreads[i] = new Background3DTileRenderer(dimension, colourScheme, customBiomeManager, rotation, jobQueue, this, layerVisibility, hiddenLayers);
            renderThreads[i].start();
        }
    }
 
    private final Dimension dimension;
    private final ColourScheme colourScheme;
    private final CustomBiomeManager customBiomeManager;
    private final int rotation;
    private HashSet<RenderResult> results = new HashSet<>();
    private Background3DTileRenderer[] renderThreads;
    private UniqueJobQueue<Tile3DRenderJob> jobQueue;
    private LayerVisibilityMode layerVisibility;
    private Set<Layer> hiddenLayers;
}