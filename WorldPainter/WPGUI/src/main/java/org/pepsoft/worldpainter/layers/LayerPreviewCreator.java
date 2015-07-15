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
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.dynmap.DynMapPreviewer;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import javax.imageio.ImageIO;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        long seed = 0L;
        TileFactory tileFactory = subterranean
                ? TileFactoryFactory.createNoiseTileFactory(seed, Terrain.BARE_GRASS, previewHeight, 56, 62, false, true, 20f, 0.5)
                : TileFactoryFactory.createNoiseTileFactory(seed, Terrain.BARE_GRASS, previewHeight, 8, 14, false, true, 20f, 0.5);
        Dimension dimension = new Dimension(seed, tileFactory, DIM_NORMAL, previewHeight);
        dimension.setSubsurfaceMaterial(Terrain.STONE);
        MinecraftWorldObject minecraftWorldObject = new MinecraftWorldObject(layer.getName() + " Preview", new Box(-8, 136, -8, 136, 0, previewHeight), previewHeight, null, new Point3i(-64, -64, 0));
        long now = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Creating data structures took " + (now - timestamp) + " ms");
        }

        // Phase two: apply layer to dimension
        timestamp = now;
        Tile tile = tileFactory.createTile(0, 0);
        switch (layer.getDataSize()) {
            case BIT:
                Random random = new Random(seed);
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        if (random.nextFloat() < pattern.getStrength(x, y)) {
                            tile.setBitLayerValue(layer, x, y, true);
                        }
                    }
                }
                break;
            case BIT_PER_CHUNK:
                random = new Random(seed);
                for (int x = 0; x < 128; x += 16) {
                    for (int y = 0; y < 128; y += 16) {
                        if (random.nextFloat() < pattern.getStrength(x, y)) {
                            tile.setBitLayerValue(layer, x, y, true);
                        }
                    }
                }
                break;
            case BYTE:
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        tile.setLayerValue(layer, x, y, Math.min((int) (pattern.getStrength(x, y) * 256), 255));
                    }
                }
                break;
            case NIBBLE:
                // If it's a CombinedLayer, also apply the terrain and biome, if
                // any
                if (layer instanceof CombinedLayer) {
                    final Terrain terrain = ((CombinedLayer) layer).getTerrain();
                    final int biome = ((CombinedLayer) layer).getBiome();
                    final boolean terrainConfigured = terrain != null;
                    final boolean biomeConfigured = biome != -1;
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            float strength = pattern.getStrength(x, y);
                            tile.setLayerValue(layer, x, y, Math.min((int) (strength * 16), 15));
                            // Double the strength so that 50% intensity results
                            // in full coverage for terrain and biome, which is
                            // inaccurate but probably more closely resembles
                            // practical usage
                            strength = Math.min(strength * 2, 1.0f);
                            if (terrainConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                tile.setTerrain(x, y, terrain);
                            }
                            if (biomeConfigured && ((strength > 0.95f) || (Math.random() < strength))) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, biome);
                            }
                        }
                    }
                } else {
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            tile.setLayerValue(layer, x, y, Math.min((int) (pattern.getStrength(x, y) * 16), 15));
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported data size " + layer.getDataSize() + " encountered");
        }
        // If the layer is a combined layers, apply it recursively
        if (layer instanceof CombinedLayer) {
            Set<Layer> addedLayers = ((CombinedLayer) layer).apply(tile);
            while (! addedLayers.isEmpty()) {
                Set<Layer> newlyAddedLayers = new HashSet<>();
                addedLayers.stream()
                    .filter(layer -> layer instanceof CombinedLayer)
                    .forEach(layer -> newlyAddedLayers.addAll(((CombinedLayer) layer).apply(tile)));
                addedLayers = newlyAddedLayers;
            }
        }
        dimension.addTile(tile);
        now = System.currentTimeMillis();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Applying layer(s) took " + (now - timestamp) + " ms");
        }

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
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Generating terrain and rendering layer took " + (now - timestamp) + " ms");
            }
        } else if (exporter instanceof SecondPassLayerExporter) {
            // Phase three: generate terrain
            timestamp = now;
            WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, Collections.emptyMap(), Constants.SUPPORTED_VERSION_2, previewHeight);
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    minecraftWorldObject.addChunk(chunkFactory.createChunk(x, y).chunk);
                }
            }
            now = System.currentTimeMillis();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Generating terrain took " + (now - timestamp) + " ms");
            }

            // Phase four: render the layer
            timestamp = now;
            Rectangle area = new Rectangle(128, 128);
            ((SecondPassLayerExporter<Layer>) exporter).render(dimension, area, area, minecraftWorldObject);
            now = System.currentTimeMillis();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Rendering layer took " + (now - timestamp) + " ms");
            }
        } else {
            throw new IllegalArgumentException("Unknown exporter type " + exporter.getClass() + " encountered");
        }

        // Final phase: post processing
        timestamp = now;
        now = System.currentTimeMillis();
        try {
            PostProcessor.postProcess(minecraftWorldObject, new Rectangle(-8, -8, 136, 136), null);
        } catch (ProgressReceiver.OperationCancelled e) {
            // Can't happen since we didn't pass in a progress receiver
            throw new InternalError();
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Post processing took " + (now - timestamp) + " ms");
        }

        return minecraftWorldObject;
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

    /**
     * Create a new <code>LayerPrevierCreator</code> configured with some
     * sensible defaults for a particular layer and dimension.
     *
     * @param layer The layer with which to configure the
     *              <code>LayerPrevierCreator</code>.
     * @param dimension The dimension from which to take some defaults.
     * @return A <code>LayerPrevierCreator</code> configured with the
     * specified layer and some sensible defaults.
     */
    public static LayerPreviewCreator createPreviewerForLayer(Layer layer, Dimension dimension) {
        LayerPreviewCreator previewer = new LayerPreviewCreator();
        previewer.setLayer(layer);
        if ((layer instanceof UndergroundPocketsLayer)
                || (layer instanceof Caverns)
                || (layer instanceof Chasms)
                || (layer instanceof TunnelLayer)
                || layer.equals(Resources.INSTANCE)) {
            previewer.setSubterranean(true);
            previewer.setPattern(CONSTANT_HALF);
        } else {
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    previewer.setPattern(CONSTANT_FULL_PLUS_GRADIENT);
                    break;
                default:
                    previewer.setPattern(CONSTANT_HALF_PLUS_GRADIENT_PLUS_HIGHLIGHT);
                    break;
            }
        }
        return previewer;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Configuration config = Configuration.load();
        if (config == null) {
            config = new Configuration();
        }
        Configuration.setInstance(config);
        WPPluginManager.initialise(config.getUuid());
        Dimension dimension = WorldFactory.createDefaultWorldWithoutTiles(config, 0L).getDimension(DIM_NORMAL);

        for (Layer layer: LayerManager.getInstance().getLayers()) {
//            Layer layer = Caverns.INSTANCE;
            LayerPreviewCreator renderer = createPreviewerForLayer(layer, dimension);
            long start = System.currentTimeMillis();
            MinecraftWorldObject preview = renderer.renderPreview();
            System.out.println("Total: " + (System.currentTimeMillis() - start) + " ms");
    //        JFrame frame = new JFrame("LayerPreviewCreator Test");
    //        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            DynMapPreviewer previewer = new DynMapPreviewer();
            previewer.setZoom(-2);
            previewer.setInclination(30.0);
            previewer.setAzimuth(60.0);
            if ((layer instanceof Caverns) || (layer instanceof Chasms) || (layer instanceof TunnelLayer)) {
                previewer.setCaves(true);
            }
            previewer.setObject(preview);
    //        frame.getContentPane().add(previewer, BorderLayout.CENTER);
    //        frame.setSize(800, 600);
    //        frame.setLocationRelativeTo(null); // Center on screen
    //        frame.setVisible(true);
            start = System.currentTimeMillis();
            BufferedImage image = previewer.createImage();
            System.out.println("Creating image took " + (System.currentTimeMillis() - start) + " ms");
            ImageIO.write(image, "png", new File(layer.getName().toLowerCase().replaceAll("\\s", "") + "-preview.png"));
        }
    }
    
    private Layer layer;
    private ExporterSettings<Layer> settings;
    private int previewHeight = 128;
    private boolean subterranean;
    private Pattern pattern = CONSTANT_HALF_PLUS_GRADIENT_PLUS_HIGHLIGHT;

    private static final Logger logger = Logger.getLogger(LayerPreviewCreator.class.getName());

    public static abstract class Pattern {
        protected Pattern(String description) {
            this.description = description;
        }
        
        abstract float getStrength(int x, int y);

        public String getDescription() {
            return description;
        }
        
        public BufferedImage getIcon() {
            BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
            for (int x = 1; x < 31; x++) {
                for (int y = 1; y < 31; y++) {
                    int value = Math.min((int) (64.0f + getStrength((31 - x) << 2, (31 - y) << 2) * 192.0f), 255);
                    icon.setRGB(x, y, (value << 16) | (value << 8) | value);
                }
            }
            return icon;
        }
        
        private final String description;
    }

    public static final Pattern CONSTANT_ONE_QUARTER = new Pattern("25%") {
        @Override
        float getStrength(int x, int y) {
            return 0.25f;
        }
    };

    public static final Pattern CONSTANT_HALF = new Pattern("50%") {
        @Override
        float getStrength(int x, int y) {
            return 0.5f;
        }
    };

    public static final Pattern CONSTANT_THREE_QUARTERS = new Pattern("75%") {
        @Override
        float getStrength(int x, int y) {
            return 0.75f;
        }
    };

    public static final Pattern CONSTANT_FULL = new Pattern("100%") {
        @Override
        float getStrength(int x, int y) {
            return 1.0f;
        }
    };

    public static final Pattern GRADIENT_ZERO_TO_FULL = new Pattern("100% - 0%") {
        @Override
        float getStrength(int x, int y) {
            return x / 127f;
        }
    };

    public static final Pattern CIRCULAR_ZERO_TO_FULL = new Pattern("0% - 100% (circular)") {
        @Override
        float getStrength(int x, int y) {
            return 1f - MathUtils.getDistance(x - 64, y - 64) / MAX_DIST;
        }

        private final float MAX_DIST = (float) Math.sqrt(64 * 64 + 64 * 64);
    };

    public static final Pattern CONSTANT_HALF_PLUS_GRADIENT_PLUS_HIGHLIGHT = new Pattern("50% - 50% - 0% (100% highlight)") {
        @Override
        float getStrength(int x, int y) {
            return Math.max(x < 64 ? x / 127f : 0.5f, 1f - MathUtils.getDistance(x - 64, 127 - y) / 64);
        }
    };

    public static final Pattern CONSTANT_FULL_PLUS_GRADIENT = new Pattern("100% - 100% - 0%") {
        @Override
        float getStrength(int x, int y) {
            return x < 64 ? x / 63f : 1.0f;
        }
    };

    public static final Pattern[] PATTERNS = {CONSTANT_ONE_QUARTER, CONSTANT_HALF, CONSTANT_THREE_QUARTERS,
            CONSTANT_FULL, CONSTANT_HALF_PLUS_GRADIENT_PLUS_HIGHLIGHT, CONSTANT_FULL_PLUS_GRADIENT,
            GRADIENT_ZERO_TO_FULL, CIRCULAR_ZERO_TO_FULL};
}