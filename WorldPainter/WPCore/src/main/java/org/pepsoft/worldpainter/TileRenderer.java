/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.jetbrains.annotations.Contract;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.renderers.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 * This class is <strong>not</strong> thread-safe! It keeps render state and should only be used to render one tile at a
 * time.
 * 
 * @author pepijn
 */
public final class TileRenderer {
    public TileRenderer(TileProvider tileProvider, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int zoom) {
        biomeRenderer = new BiomeRenderer(customBiomeManager);
        this.tileProvider = tileProvider;
        if ((tileProvider instanceof Dimension) && (((Dimension) tileProvider).getWorld() != null)) {
            platform = ((Dimension) tileProvider).getWorld().getPlatform();
            switch (((Dimension) tileProvider).getDim()) {
                case DIM_NORMAL:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_NORMAL_CEILING);
                    break;
                case DIM_NORMAL_CEILING:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_NORMAL);
                    break;
                case DIM_NETHER:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_NETHER_CEILING);
                    break;
                case DIM_NETHER_CEILING:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_NETHER);
                    break;
                case DIM_END:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_END_CEILING);
                    break;
                case DIM_END_CEILING:
                    oppositeTileProvider = ((Dimension) tileProvider).getWorld().getDimension(DIM_END);
                    break;
                default:
                    oppositeTileProvider = null;
                    break;
            }
        } else {
            platform = Configuration.DEFAULT_PLATFORM;
            oppositeTileProvider = null;
        }
        setColourScheme(colourScheme);
        this.zoom = zoom;
        bufferedImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        renderBuffer = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
    }

    public TileProvider getTileProvider() {
        return tileProvider;
    }

    public TileProvider getOppositeTileProvider() {
        return oppositeTileProvider;
    }

    public Platform getPlatform() {
        return platform;
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        waterColour = colourScheme.getColour(BLK_WATER);
        lavaColour = colourScheme.getColour(BLK_LAVA);
        bedrockColour = colourScheme.getColour(BLK_BEDROCK);
    }

    public void addHiddenLayers(Collection<Layer> hiddenLayers) {
        this.hiddenLayers.addAll(hiddenLayers);
    }
    
    public void addHiddenLayer(Layer hiddenLayer) {
        hiddenLayers.add(hiddenLayer);
    }

    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        this.hiddenLayers.clear();
        // The FloodWithLava layer should *always* remain hidden
        hiddenLayers.add(FloodWithLava.INSTANCE);
        this.hiddenLayers.addAll(hiddenLayers);
    }

    public void removeHiddenLayer(Layer hiddenLayer) {
        // The FloodWithLava layer should *always* remain hidden
        if (! hiddenLayer.equals(FloodWithLava.INSTANCE)) {
            hiddenLayers.remove(hiddenLayer);
        }
    }

    public Set<Layer> getHiddenLayers() {
        return Collections.unmodifiableSet(hiddenLayers);
    }

    public int getZoom() {
        return zoom;
    }

    public boolean isContourLines() {
        return contourLines;
    }

    public void setContourLines(boolean contourLines) {
        this.contourLines = contourLines;
    }

    public int getContourSeparation() {
        return contourSeparation;
    }

    public void setContourSeparation(int contourSeparation) {
        this.contourSeparation = contourSeparation;
    }

    /**
     * Render the tile to an image, with an optional offset. This will
     * completely paint the square covered by the tile, replacing any existing
     * contents.
     *
     * @param tile The tile to render.
     * @param image The image to which to render the tile.
     * @param dx The horizontal offset at which to render the tile.
     * @param dy The vertical offset at which to render the tile.
     */
    public void renderTile(Tile tile, Image image, int dx, int dy) {
        // TODO this deadlocks background painting. Find out why:
//        synchronized (tile) {

        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                final float height = tile.getHeight(x, y);
                floatHeightCache[x | (y << TILE_SIZE_BITS)] = height;
                intHeightCache[x | (y << TILE_SIZE_BITS)] = Math.round(height);
                intFluidHeightCache[x | (y << TILE_SIZE_BITS)] = tile.getWaterLevel(x, y);
            }
        }

        // Determine which coordinates, if any, have heights which would
        // intersect with the opposite tile, if any
        final boolean bottomless, topLayersRelativeToTerrain;
        final long seed;
        boolean noOpposites = true;
        if (tileProvider instanceof Dimension) {
            final Dimension dim = (Dimension) tileProvider;
            bottomless = dim.isBottomless();
            topLayersRelativeToTerrain = dim.getTopLayerAnchor() == Dimension.LayerAnchor.TERRAIN;
            seed = dim.getSeed();
            if (oppositeTileProvider instanceof Dimension) {
                final int totalRange = dim.getMaxHeight() + dim.getMinHeight();
                final Tile oppositeTile = oppositeTileProvider.getTile(tile.getX(), tile.getY());
                if (oppositeTile != null) {
                    Arrays.fill(oppositesOverlap, false);
                    final int oppositesDelta = Math.abs(dim.getCeilingHeight() - ((Dimension) oppositeTileProvider).getCeilingHeight());
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            if ((oppositeTile.getIntHeight(x, y) + intHeightCache[x | (y << TILE_SIZE_BITS)] + oppositesDelta) >= totalRange) {
                                oppositesOverlap[x | (y << TILE_SIZE_BITS)] = true;
                                noOpposites = false;
                            }
                        }
                    }
                }
            }
        } else {
            bottomless = false;
            topLayersRelativeToTerrain = false;
            seed = 0L;
        }

        final List<Layer> layerList = new ArrayList<>(tile.getLayers());
        if (! layerList.contains(Biome.INSTANCE)) {
            layerList.add(Biome.INSTANCE);
        }
        layerList.removeAll(hiddenLayers);
        final boolean hideTerrain = hiddenLayers.contains(TERRAIN_AS_LAYER);
        final boolean hideFluids = hiddenLayers.contains(FLUIDS_AS_LAYER);
        final boolean _void = layerList.contains(org.pepsoft.worldpainter.layers.Void.INSTANCE), notAllChunksPresent = layerList.contains(NotPresent.INSTANCE);
        final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
        final LayerRenderer[] renderers = new LayerRenderer[layers.length];
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] instanceof Biome) {
                renderers[i] = biomeRenderer;
            } else {
                renderers[i] = layers[i].getRenderer();
            }
            if (renderers[i] instanceof ColourSchemeRenderer) {
                ((ColourSchemeRenderer) renderers[i]).setColourScheme(colourScheme);
            }
            if ((renderers[i] instanceof DimensionAwareRenderer) && (tileProvider instanceof Dimension)) {
                ((DimensionAwareRenderer) renderers[i]).setDimension((Dimension) tileProvider);
            }
        }

        final int scale = 1 << -zoom;
        final Graphics2D g2 = (Graphics2D) image.getGraphics();
        try {
            g2.setComposite(AlphaComposite.Src);
            if (zoom == 0) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        if (notAllChunksPresent && (tile.getBitLayerValue(NotPresent.INSTANCE, x, y))) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = 0x00000000;
                        } else if ((!noOpposites) && oppositesOverlap[x | (y << TILE_SIZE_BITS)] && CEILING_PATTERN[x & 0x7][y & 0x7]) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = 0xff000000;
                        } else if (_void && tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y)) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = 0x00000000;
                        } else {
                            int colour = getPixelColour(tile, x, y, layers, renderers, contourLines, hideTerrain, hideFluids, bottomless, topLayersRelativeToTerrain, seed);
                            colour = ColourUtils.multiply(colour, getTerrainBrightenAmount());
                            final int offset = x + y * TILE_SIZE;
                            if (intFluidHeightCache[offset] > intHeightCache[offset]) {
                                colour = ColourUtils.multiply(colour, getFluidBrightenAmount());
                            }
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = 0xff000000 | colour;
                        }
                    }
                }

                g2.drawImage(bufferedImage, dx, dy, null);
            } else {
                final int tileSize = TILE_SIZE / scale;
                for (int x = 0; x < TILE_SIZE; x += scale) {
                    for (int y = 0; y < TILE_SIZE; y += scale) {
                        if (notAllChunksPresent && (tile.getBitLayerValue(NotPresent.INSTANCE, x, y))) {
                            renderBuffer[x / scale + y * tileSize] = 0x00000000;
                        } else if ((!noOpposites) && oppositesOverlap[x | (y << TILE_SIZE_BITS)]) {
                            renderBuffer[x / scale + y * tileSize] = 0xff000000;
                        } else if (_void && tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y)) {
                            renderBuffer[x / scale + y * tileSize] = 0x00000000;
                        } else {
                            int colour = getPixelColour(tile, x, y, layers, renderers, contourLines, hideTerrain, hideFluids, bottomless, topLayersRelativeToTerrain, seed);
                            colour = ColourUtils.multiply(colour, getTerrainBrightenAmount());
                            final int offset = x + y * TILE_SIZE;
                            if (intFluidHeightCache[offset] > intHeightCache[offset]) {
                                colour = ColourUtils.multiply(colour, getFluidBrightenAmount());
                            }
                            renderBuffer[x / scale + y * tileSize] = 0xff000000 | colour;
                        }
                    }
                }

                g2.drawImage(bufferedImage, dx, dy, dx + tileSize, dy + tileSize, 0, 0, tileSize, tileSize, null);
            }
        } finally {
            g2.dispose();
        }
//        }
    }

    public static TileRenderer forWorld(World2 world, int dim, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int zoom) {
        Dimension dimension = world.getDimension(dim);
        return new TileRenderer(dimension, colourScheme, customBiomeManager, zoom);
    }

    /**
     * Determine the brighten amount. This method assumes that the
     * {@link #deltas} array has been filled by a previous call to
     * {@link #getPixelColour(Tile, int, int, Layer[], LayerRenderer[], boolean, boolean, boolean, boolean, boolean, long)}.
     * 
     * @return The amount by which to brighten the pixel for the specified
     * block, out of 256; values below 256 darkening the pixel; values above
     * brightening it and 256 resulting in no change.
     */
    private int getTerrainBrightenAmount() {
        return getBrightenAmount(deltas);
    }

    /**
     * Determine the brighten amount for fluid. This method assumes that the
     * {@link #deltas} array has been filled by a previous call to
     * {@link #getPixelColour(Tile, int, int, Layer[], LayerRenderer[], boolean, boolean, boolean, boolean, boolean, long)}.
     *
     * @return The amount by which to brighten the pixel for the specified
     * block, out of 256; values below 256 darkening the pixel; values above
     * brightening it and 256 resulting in no change.
     */
    private int getFluidBrightenAmount() {
        return getBrightenAmount(fluidDeltas);
    }

    private int getBrightenAmount(int[][] deltas) {
        switch (lightOrigin) {
            case NORTHWEST:
                return Math.max(0, ((deltas[2][1] - deltas[0][1] + deltas[1][2] - deltas[1][0]) << 5) + 256);
            case NORTHEAST:
                return Math.max(0, ((deltas[0][1] - deltas[2][1] + deltas[1][2] - deltas[1][0]) << 5) + 256);
            case SOUTHEAST:
                return Math.max(0, ((deltas[0][1] - deltas[2][1] + deltas[1][0] - deltas[1][2]) << 5) + 256);
            case SOUTHWEST:
                return Math.max(0, ((deltas[2][1] - deltas[0][1] + deltas[1][0] - deltas[1][2]) << 5) + 256);
            case ABOVE:
                return 256;
            default:
                throw new InternalError();
        }
    }

    public LightOrigin getLightOrigin() {
        return lightOrigin;
    }
    
    public void setLightOrigin(LightOrigin lightOrigin) {
        if (lightOrigin == null) {
            throw new NullPointerException();
        }
        this.lightOrigin = lightOrigin;
    }

    private int getPixelColour(Tile tile, int x, int y, Layer[] layers, LayerRenderer[] renderers, boolean contourLines, boolean hideTerrain, boolean hideFluids, boolean bottomless, boolean topLayersRelativeToTerrain, long seed) {
        final int offset = x + y * TILE_SIZE;
        final int intHeight = intHeightCache[offset], minHeight = tile.getMinHeight();
        heights[1][0] = getNeighbourHeight(tile, x, y,  0, -1);
        deltas [1][0] = heights[1][0] - intHeight;
        heights[0][1] = getNeighbourHeight(tile, x, y, -1,  0);
        deltas [0][1] = heights[0][1] - intHeight;
        heights[2][1] = getNeighbourHeight(tile, x, y,  1,  0);
        deltas [2][1] = heights[2][1] - intHeight;
        heights[1][2] = getNeighbourHeight(tile, x, y,  0,  1);
        deltas [1][2] = heights[1][2] - intHeight;
        if (contourLines && ((intHeight % contourSeparation) == 0)
                && ((deltas[0][1] < 0)
                    || (deltas[2][1] < 0)
                    || (deltas[1][0] < 0)
                    || (deltas[1][2] < 0))) {
            return BLACK;
        }
        final int waterLevel = tile.getWaterLevel(x, y);
        fluidHeights[1][0] = getNeighbourFluidHeight(tile, x, y, 0, -1, waterLevel);
        fluidDeltas [1][0] = fluidHeights[1][0] - waterLevel;
        fluidHeights[0][1] = getNeighbourFluidHeight(tile, x, y, -1, 0, waterLevel);
        fluidDeltas [0][1] = fluidHeights[0][1] - waterLevel;
        fluidHeights[2][1] = getNeighbourFluidHeight(tile, x, y, 1, 0, waterLevel);
        fluidDeltas [2][1] = fluidHeights[2][1] - waterLevel;
        fluidHeights[1][2] = getNeighbourFluidHeight(tile, x, y, 0, 1, waterLevel);
        fluidDeltas [1][2] = fluidHeights[1][2] - waterLevel;
        final int worldX = (tile.getX() << TILE_SIZE_BITS) | x, worldY = (tile.getY() << TILE_SIZE_BITS) | y;
        int colour;
        if ((! hideFluids) && (waterLevel > intHeight)) {
            if (tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y)) {
                colour = lavaColour;
            } else {
                colour = waterColour;
            }
        } else if (! hideTerrain) {
            final float height = floatHeightCache[offset];
            if ((! bottomless) && (intHeight == minHeight)) {
                colour = bedrockColour;
            } else {
                Terrain terrain = tile.getTerrain(x, y);
                if (topLayersRelativeToTerrain
                        && terrain.isCustom()) {
                    MixedMaterial mixedMaterial = Terrain.getCustomMaterial(terrain.getCustomTerrainIndex());
                    if (mixedMaterial.getMode() == MixedMaterial.Mode.LAYERED){
                        colour = terrain.getColour(seed, worldX, worldY, mixedMaterial.getPatternHeight() - 1, intHeight, platform, colourScheme);
                    } else {
                        colour = terrain.getColour(seed, worldX, worldY, height, intHeight, platform, colourScheme);
                    }
                } else {
                    colour = terrain.getColour(seed, worldX, worldY, height, intHeight, platform, colourScheme);
                }
            }
        } else {
            colour = LIGHT_GREY;
        }
        for (int i = 0; i < layers.length; i++) {
            final Layer layer = layers[i];
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    if (hideFluids && (layer instanceof Frost) && (waterLevel > intHeightCache[offset])) {
                        continue;
                    }
                    boolean bitLayerValue = tile.getBitLayerValue(layer, x, y);
                    if (bitLayerValue) {
                        final BitLayerRenderer renderer = (BitLayerRenderer) renderers[i];
                        if (reportMissingRenderer(layer, renderer == null)) continue;
                        colour = renderer.getPixelColour(worldX, worldY, colour, true);
                    }
                    break;
                case NIBBLE:
                    int layerValue = tile.getLayerValue(layer, x, y);
                    if (layerValue > 0) {
                        final NibbleLayerRenderer renderer = (NibbleLayerRenderer) renderers[i];
                        if (reportMissingRenderer(layer, renderer == null)) continue;
                        colour = renderer.getPixelColour(worldX, worldY, colour, layerValue);
                    }
                    break;
                case BYTE:
                    final ByteLayerRenderer byteLayerRenderer = (ByteLayerRenderer) renderers[i];
                    if (reportMissingRenderer(layer, byteLayerRenderer == null)) continue;
                    colour = byteLayerRenderer.getPixelColour(worldX, worldY, colour, tile.getLayerValue(layer, x, y));
                    break;
                default:
                    throw new UnsupportedOperationException("Don't know how to render " + layer.getClass().getSimpleName());
            }
        }
        return colour;
    }

    @Contract("_, true -> true")
    private boolean reportMissingRenderer(Layer layer, boolean rendererMissing) {
        if (rendererMissing) {
            logger.error("Missing renderer for layer " + layer + " (type: " + layer.getClass().getSimpleName() + ")");
            if (! missingRendererReportedFor.contains(layer)) {
                missingRendererReportedFor.add(layer);
                throw new IllegalStateException("Missing renderer for layer " + layer + " (type: " + layer.getClass().getSimpleName() + ")");
            } else {
                return true;
            }
        }
        return false;
    }

    private int getNeighbourHeight(Tile tile, int x, int y, int dx, int dy) {
        x = x + dx;
        y = y + dy;
        if ((x >= 0) && (x < TILE_SIZE) && (y >= 0) && (y < TILE_SIZE)) {
            return intHeightCache[x + y * TILE_SIZE];
        } else {
            int tileDX = 0, tileDY = 0;
            if (x < 0) {
                tileDX = -1;
                x += TILE_SIZE;
            } else if (x >= TILE_SIZE) {
                tileDX = 1;
                x -= TILE_SIZE;
            }
            if (y < 0) {
                tileDY = -1;
                y += TILE_SIZE;
            } else if (y >= TILE_SIZE) {
                tileDY = 1;
                y -= TILE_SIZE;
            }
            Tile neighborTile = tileProvider.getTile(tile.getX() + tileDX, tile.getY() + tileDY);
            return (neighborTile != null) ? neighborTile.getIntHeight(x, y) : DEFAULT_WATER_LEVEL;
        }
    }

    private int getNeighbourFluidHeight(Tile tile, int x, int y, int dx, int dy, int defaultHeight) {
        x = x + dx;
        y = y + dy;
        if ((x >= 0) && (x < TILE_SIZE) && (y >= 0) && (y < TILE_SIZE)) {
            int offset = x + y * TILE_SIZE;
            return (intFluidHeightCache[offset] > intHeightCache[offset]) ? intFluidHeightCache[offset] : defaultHeight;
        } else {
            int tileDX = 0, tileDY = 0;
            if (x < 0) {
                tileDX = -1;
                x += TILE_SIZE;
            } else if (x >= TILE_SIZE) {
                tileDX = 1;
                x -= TILE_SIZE;
            }
            if (y < 0) {
                tileDY = -1;
                y += TILE_SIZE;
            } else if (y >= TILE_SIZE) {
                tileDY = 1;
                y -= TILE_SIZE;
            }
            Tile neighborTile = tileProvider.getTile(tile.getX() + tileDX, tile.getY() + tileDY);
            if (neighborTile != null) {
                int waterLevel = neighborTile.getWaterLevel(x, y);
                return (waterLevel > neighborTile.getIntHeight(x, y)) ? waterLevel : defaultHeight;
            } else {
                return defaultHeight;
            }
        }
    }

    private final BiomeRenderer biomeRenderer;
    private final Set<Layer> hiddenLayers = new HashSet<>(Collections.singletonList(FloodWithLava.INSTANCE));
    private final int[] intHeightCache = new int[TILE_SIZE * TILE_SIZE], intFluidHeightCache = new int[TILE_SIZE * TILE_SIZE];
    private final float[] floatHeightCache = new float[TILE_SIZE * TILE_SIZE];
    private final BufferedImage bufferedImage;
    private final int[] renderBuffer;
    private final int[][] heights = new int[3][3], deltas = new int[3][3], fluidHeights = new int[3][3], fluidDeltas = new int[3][3];
    private final Set<Layer> missingRendererReportedFor = new HashSet<>(); // TODO remove when no longer necessary!
    private final boolean[] oppositesOverlap = new boolean[TILE_SIZE * TILE_SIZE];
    private final int zoom;
    private final Platform platform;
    private final TileProvider tileProvider, oppositeTileProvider;
    private ColourScheme colourScheme;
    private boolean contourLines = true;
    private int contourSeparation = 10, waterColour, lavaColour, bedrockColour;
    private LightOrigin lightOrigin = LightOrigin.NORTHWEST;

    public static final Layer TERRAIN_AS_LAYER = new Layer("org.pepsoft.synthetic.Terrain", "Terrain", "The terrain type of the surface", Layer.DataSize.NONE, 0) {
        @Override
        public BufferedImage getIcon() {
            return ICON;
        }

        private final BufferedImage ICON = IconUtils.scaleIcon(IconUtils.loadScaledImage("org/pepsoft/worldpainter/resources/terrain.png"), 16);
    };
    public static final Layer FLUIDS_AS_LAYER = new Layer("org.pepsoft.synthetic.Fluids", "Water/Lava", "Areas flooded with water or lava", Layer.DataSize.NONE, 0) {
        @Override
        public BufferedImage getIcon() {
            return ICON;
        }

        private final BufferedImage ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/resources/fluids.png");
    };

    private static final int BLACK = 0x000000, LIGHT_GREY = 0xD0D0D0;
    private static final boolean[][] CEILING_PATTERN = {
            { true,  true, false, false,  true,  true, false, false},
            {false,  true,  true,  true,  true, false, false, false},
            {false, false,  true,  true, false, false, false, false},
            {false,  true,  true,  true,  true, false, false, false},
            { true,  true, false, false,  true,  true, false, false},
            { true, false, false, false, false,  true,  true,  true},
            {false, false, false, false, false, false,  true,  true},
            { true, false, false, false, false,  true,  true,  true}};
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TileRenderer.class);

    public enum LightOrigin {
        NORTHWEST {
            @Override
            public LightOrigin left() {
                return ABOVE;
            }

            @Override
            public LightOrigin right() {
                return NORTHEAST;
            }
        },
        NORTHEAST {
            @Override
            public LightOrigin left() {
                return NORTHWEST;
            }

            @Override
            public LightOrigin right() {
                return SOUTHEAST;
            }
        },
        SOUTHEAST{
            @Override
            public LightOrigin left() {
                return NORTHEAST;
            }

            @Override
            public LightOrigin right() {
                return SOUTHWEST;
            }
        },
        SOUTHWEST{
            @Override
            public LightOrigin left() {
                return SOUTHEAST;
            }

            @Override
            public LightOrigin right() {
                return ABOVE;
            }
        },
        ABOVE{
            @Override
            public LightOrigin left() {
                return SOUTHWEST;
            }

            @Override
            public LightOrigin right() {
                return NORTHWEST;
            }
        };

        public abstract LightOrigin left();
        
        public abstract LightOrigin right();
    }
}