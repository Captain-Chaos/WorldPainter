/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.Box;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.dynmap.DynMapPreviewer;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 * A utility class for generating previews of layers. It renders a layer to a
 * small temporary world and generates a {@link MinecraftWorldObject} (an object
 * which implements both {@link MinecraftWorld} and {@link WPObject}) which can
 * be used to display it, for instance with {@link DynMapPreviewer}.
 *
 * @author SchmitzP
 */
public class LayerPreviewCreator {
    public MinecraftWorldObject renderPreview() {
        // Phase one: setup
        long timestamp = System.currentTimeMillis();
        TileFactory tileFactory = subterranean
                ? TileFactoryFactory.createNoiseTileFactory(0L, Terrain.BARE_GRASS, previewHeight, 56, 62, false, true, 20f, 0.5)
                : TileFactoryFactory.createNoiseTileFactory(0L, Terrain.BARE_GRASS, previewHeight, 8, 14, false, true, 20f, 0.5);
        long seed = 0L;
        Dimension dimension = new Dimension(seed, tileFactory, DIM_NORMAL, previewHeight);
        Tile tile = tileFactory.createTile(0, 0);
        dimension.addTile(tile);
        MinecraftWorldObject minecraftWorldObject = new MinecraftWorldObject(layer.getName() + " Preview", new Box(-8, 136, -8, 136, 0, previewHeight), previewHeight, null, new Point3i(-64, -64, 0));
        long now = System.currentTimeMillis();
        System.out.println("Creating data structures took " + (now - timestamp) + " ms");

        // Phase two: apply layer to dimension
        timestamp = now;
        switch (layer.getDataSize()) {
            case BIT:
                Random random = new Random(seed);
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        if (random.nextFloat() < pattern.getStrength(x, y)) {
                            dimension.setBitLayerValueAt(layer, x, y, true);
                        }
                    }
                }
                break;
            case BIT_PER_CHUNK:
                random = new Random(seed);
                for (int x = 0; x < 128; x += 16) {
                    for (int y = 0; y < 128; y += 16) {
                        if (random.nextFloat() < pattern.getStrength(x, y)) {
                            dimension.setBitLayerValueAt(layer, x, y, true);
                        }
                    }
                }
                break;
            case BYTE:
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        dimension.setLayerValueAt(layer, x, y, Math.min((int) (pattern.getStrength(x, y) * 256), 255));
                    }
                }
                break;
            case NIBBLE:
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        dimension.setLayerValueAt(layer, x, y, Math.min((int) (pattern.getStrength(x, y) * 16), 15));
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported data size " + layer.getDataSize() + " encountered");
        }
        now = System.currentTimeMillis();
        System.out.println("Painting layer values took " + (now - timestamp) + " ms");

        LayerExporter<Layer> exporter = layer.getExporter();
        if (settings != null) {
            exporter.setSettings(settings);
        }
        if (exporter instanceof FirstPassLayerExporter) {

            // Phase three: generate terrain and render the layer
            timestamp = now;
            WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, Collections.singletonMap(layer, exporter), Constants.SUPPORTED_VERSION_2, previewHeight);
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    Chunk chunk = chunkFactory.createChunk(x, y).chunk;
                    ((FirstPassLayerExporter<Layer>) exporter).render(dimension, tile, chunk);
                    minecraftWorldObject.addChunk(chunk);
                }
            }
            now = System.currentTimeMillis();
            System.out.println("Generating terrain and rendering layer took " + (now - timestamp) + " ms");

            return minecraftWorldObject;
        } else if (exporter instanceof SecondPassLayerExporter) {

            // Phase three: generate terrain
            timestamp = now;
            WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, Collections.singletonMap(layer, exporter), Constants.SUPPORTED_VERSION_2, previewHeight);
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    minecraftWorldObject.addChunk(chunkFactory.createChunk(x, y).chunk);
                }
            }
            now = System.currentTimeMillis();
            System.out.println("Generating terrain took " + (now - timestamp) + " ms");

            // Phase four: render the layer
            timestamp = now;
            Rectangle area = new Rectangle(128, 128);
            ((SecondPassLayerExporter<Layer>) exporter).render(dimension, area, area, minecraftWorldObject);
            now = System.currentTimeMillis();
            System.out.println("Rendering layer took " + (now - timestamp) + " ms");

            return minecraftWorldObject;
        } else {
            throw new IllegalArgumentException("Unknown exporter type " + exporter.getClass() + " encountered");
        }
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public ExporterSettings<Layer> getSettings() {
        return settings;
    }

    public void setSettings(ExporterSettings<Layer> settings) {
        this.settings = settings;
    }

    public boolean isSubterranean() {
        return subterranean;
    }

    public void setSubterranean(boolean subterranean) {
        this.subterranean = subterranean;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public static void main(String[] args) throws IOException {
        Layer layer = Jungle.INSTANCE;
//        Layer layer = Resources.INSTANCE;
        LayerPreviewCreator renderer = new LayerPreviewCreator();
        renderer.setLayer(layer);
//        renderer.setSubterranean(true);
//        renderer.setPattern(HALF);
        long start = System.currentTimeMillis();
        MinecraftWorldObject preview = renderer.renderPreview();
        System.out.println("Total: " + (System.currentTimeMillis() - start) + " ms");

//        JFrame frame = new JFrame("LayerPreviewCreator Test");
//        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        DynMapPreviewer previewer = new DynMapPreviewer();
        previewer.setZoom(-2);
        previewer.setInclination(30.0);
        previewer.setAzimuth(60.0);
        previewer.setObject(preview);
//        frame.getContentPane().add(previewer, BorderLayout.CENTER);
//
//        frame.setSize(800, 600);
//        frame.setLocationRelativeTo(null); // Center on screen
//        frame.setVisible(true);


        start = System.currentTimeMillis();
        BufferedImage image = previewer.createImage();
        System.out.println("Creating image took " + (System.currentTimeMillis() - start) + " ms");

        ImageIO.write(image, "png", new File("preview.png"));
    }
    
    private Layer layer;
    private ExporterSettings<Layer> settings;
    private int previewHeight = 128;
    private boolean subterranean;
    private Pattern pattern = GRADIENT_WITH_HIGHLIGHT;

    public static abstract class Pattern {
        abstract float getStrength(int x, int y);
    }

    public static final Pattern HALF = new Pattern() {
        @Override
        float getStrength(int x, int y) {
            return 0.5f;
        }
    };

    public static final Pattern FULL = new Pattern() {
        @Override
        float getStrength(int x, int y) {
            return 1.0f;
        }
    };

    public static final Pattern X_GRADIENT = new Pattern() {
        @Override
        float getStrength(int x, int y) {
            return x / 127f;
        }
    };

    public static final Pattern CIRCULAR = new Pattern() {
        @Override
        float getStrength(int x, int y) {
            return 1f - MathUtils.getDistance(x - 64, y - 64) / MAX_DIST;
        }

        private final float MAX_DIST = (float) Math.sqrt(64 * 64);
    };

    public static final Pattern GRADIENT_WITH_HIGHLIGHT = new Pattern() {
        @Override
        float getStrength(int x, int y) {
            return Math.max(x < 64 ? x / 127f : 0.5f, 1f - MathUtils.getDistance(x - 64, 127 - y) / 64);
        }
    };
}