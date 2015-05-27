/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.ColourScheme;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.TileFactoryFactory;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.objects.WPObjectRenderer;

/**
 *
 * @author SchmitzP
 */
public class LayerPreviewCreator {
    public LayerPreviewCreator(Layer layer) {
        this(layer, null);
    }
    
    public <L extends Layer> LayerPreviewCreator(L layer, ExporterSettings<L> settings) {
        this.layer = layer;
        this.settings = (ExporterSettings<Layer>) settings;
        previewHeight = 32;
        long timestamp = System.currentTimeMillis();
        TileFactory tileFactory = TileFactoryFactory.createFlatTileFactory(0L, Terrain.BARE_GRASS, previewHeight, 1, 0, false, false);
        dimension = new Dimension(0L, tileFactory, DIM_NORMAL, previewHeight);
        dimension.addTile(tileFactory.createTile(0, 0));
        dimension.addTile(tileFactory.createTile(1, 0));
        long now = System.currentTimeMillis();
        System.out.println("Creating dimension and tiles took " + (now - timestamp) + " ms");
        timestamp = now;
        switch (layer.getDataSize()) {
            case BIT:
                Random random = new Random(0L);
                for (int x = 0; x < 256; x++) {
                    for (int y = 0; y < 128; y++) {
                        if (random.nextInt(256) < x) {
                            dimension.setBitLayerValueAt(layer, x, y, true);
                        }
                    }
                }
                break;
            case BIT_PER_CHUNK:
                random = new Random(0L);
                for (int x = 0; x < 256; x += 16) {
                    for (int y = 0; y < 128; y += 16) {
                        if (random.nextInt(256) < x) {
                            dimension.setBitLayerValueAt(layer, x, y, true);
                        }
                    }
                }
                break;
            case BYTE:
                for (int x = 0; x < 256; x++) {
                    for (int y = 0; y < 128; y++) {
                        dimension.setHeightAt(x, y, 1.0f);
                        dimension.setLayerValueAt(layer, x, y, x);
                    }
                }
                break;
            case NIBBLE:
                for (int x = 0; x < 256; x++) {
                    for (int y = 0; y < 128; y++) {
                        dimension.setHeightAt(x, y, 1.0f);
                        dimension.setLayerValueAt(layer, x, y, x >> 4);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported data size " + layer.getDataSize() + " encountered");
        }
        now = System.currentTimeMillis();
        System.out.println("Painting layer values took " + (now - timestamp) + " ms");
        timestamp = now;
        minecraftWorldObject = new MinecraftWorldObject(layer.getName() + " Preview", new Box(0, 256, 0, 128, 0, previewHeight), previewHeight, new Material[] {Material.BEDROCK, Material.GRASS});
        now = System.currentTimeMillis();
        System.out.println("Creating world took " + (now - timestamp) + " ms");
    }
    
    public BufferedImage createPreview(ColourScheme colourScheme, int blockSize) {
        LayerExporter<Layer> exporter = (LayerExporter<Layer>) layer.getExporter();
        if (settings != null) {
            exporter.setSettings(settings);
        }
        if (exporter instanceof FirstPassLayerExporter) {
            throw new UnsupportedOperationException("First pass layers not yet supported");
        } else if (exporter instanceof SecondPassLayerExporter) {
            long timestamp = System.currentTimeMillis();
            minecraftWorldObject.reset();
            long now = System.currentTimeMillis();
            System.out.println("Resetting world took " + (now - timestamp) + " ms");
            Rectangle area = new Rectangle(256, 128);
            ((SecondPassLayerExporter<Layer>) exporter).render(dimension, area, area, minecraftWorldObject);
            now = System.currentTimeMillis();
            System.out.println("Exporting layer took " + (now - timestamp) + " ms");
            timestamp = now;
            WPObjectRenderer renderer = new WPObjectRenderer(minecraftWorldObject, colourScheme, blockSize);
            BufferedImage preview = renderer.render();
            System.out.println("Rendering 3D image took " + (System.currentTimeMillis() - timestamp) + " ms");
            return preview;
        } else {
            throw new IllegalArgumentException("Unknown exporter type " + exporter.getClass() + " encountered");
        }
    }
    
    public static void main(String[] args) throws IOException {
        Layer layer = PineForest.INSTANCE;
        LayerPreviewCreator previewer = new LayerPreviewCreator(layer);
        ColourScheme colourScheme = new DynMapColourScheme("default", true);
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            BufferedImage preview = previewer.createPreview(colourScheme, 1);
            System.out.println("Creating preview took " + (System.currentTimeMillis() - start) + " ms");
            ImageIO.write(preview, "PNG", new File("preview.png"));
        }
    }
    
    private final Layer layer;
    private final ExporterSettings<Layer> settings;
    private final int previewHeight;
    private final Dimension dimension;
    private final MinecraftWorldObject minecraftWorldObject;
}