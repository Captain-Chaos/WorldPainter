/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.jetbrains.annotations.Contract;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Void;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.renderers.*;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayerHelper;
import org.pepsoft.worldpainter.ramps.ColourRamp;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.FIXED_HEIGHT_ABOVE_FLOOR;

/**
 * This class is <strong>not</strong> thread-safe! It keeps render state and should only be used to render one tile at a
 * time.
 * 
 * @author pepijn
 */
public final class TileRenderer {
    public TileRenderer(TileProvider tileProvider, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int zoom, boolean transparentVoid, ColourRamp colourRamp) {
        biomeRenderer = new BiomeRenderer(customBiomeManager, colourScheme);
        this.tileProvider = tileProvider;
        final Dimension dimension = (tileProvider instanceof Dimension) ? (Dimension) tileProvider : null;
        TileProvider relatedTileProvider = null;
        boolean renderCeilingIntersection = false, renderTunnelRoofIntersection = false;
        TunnelLayerHelper tunnelLayerHelper = null;
        if ((dimension != null) && (dimension.getWorld() != null)) {
            platform = dimension.getWorld().getPlatform();
            final Anchor anchor = dimension.getAnchor();
            if (anchor.role == DETAIL) {
                relatedTileProvider = dimension.getWorld().getDimension(new Anchor(anchor.dim, anchor.role, ! anchor.invert, 0));
                renderCeilingIntersection = (relatedTileProvider != null);
            } else if (anchor.role == CAVE_FLOOR) {
                final Dimension detailDimension = dimension.getWorld().getDimension(new Anchor(anchor.dim, DETAIL, anchor.invert, 0));
                if (detailDimension != null) {
                    final TunnelLayer tunnelLayer = TunnelLayer.find(dimension);
                    if (tunnelLayer.getRoofMode() != FIXED_HEIGHT_ABOVE_FLOOR) {
                        relatedTileProvider = detailDimension;
                        renderTunnelRoofIntersection = true;
                        tunnelLayerHelper = new TunnelLayerHelper(tunnelLayer, detailDimension);
                    }
                }
            }
        } else {
            platform = Configuration.DEFAULT_PLATFORM;
        }
        this.relatedTileProvider = relatedTileProvider;
        this.renderCeilingIntersection = renderCeilingIntersection;
        this.renderTunnelRoofIntersection = renderTunnelRoofIntersection;
        this.tunnelLayerHelper = tunnelLayerHelper;
        this.zoom = zoom;
        this.transparentVoid = transparentVoid;
        this.colourRamp = colourRamp;
        setColourScheme(colourScheme);
        bufferedImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        renderBuffer = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
    }

    public Platform getPlatform() {
        return platform;
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        waterColour = colourScheme.getColour(WATER);
        lavaColour = colourScheme.getColour(LAVA);
        bedrockColour = colourScheme.getColour(BEDROCK);
        notPresentColour = 0x00000000;
        voidColour = (transparentVoid ? 0x00000000 : 0xff000000) | VoidRenderer.getColour();
    }

    public void addHiddenLayers(Collection<Layer> hiddenLayers) {
        this.hiddenLayers.addAll(hiddenLayers);
    }
    
    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        this.hiddenLayers.clear();
        // The FloodWithLava layer should *always* remain hidden
        hiddenLayers.add(FloodWithLava.INSTANCE);
        this.hiddenLayers.addAll(hiddenLayers);
    }

    public Set<Layer> getHiddenLayers() {
        return Collections.unmodifiableSet(hiddenLayers);
    }

    public int getZoom() {
        return zoom;
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

    public boolean isHideAllLayers() {
        return hideAllLayers;
    }

    public void setHideAllLayers(boolean hideAllLayers) {
        this.hideAllLayers = hideAllLayers;
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

        final int tileX = tile.getX(), tileY = tile.getY();
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                final float height = tile.getHeight(x, y);
                floatHeightCache[x | (y << TILE_SIZE_BITS)] = height;
                intHeightCache[x | (y << TILE_SIZE_BITS)] = Math.round(height);
                intFluidHeightCache[x | (y << TILE_SIZE_BITS)] = tile.getWaterLevel(x, y);
            }
        }

        // Determine which coordinates, if any, have heights which would intersect with the opposite tile, if any
        final boolean bottomless, topLayersRelativeToTerrain;
        final long seed;
        boolean noOpposites = true;
        if (tileProvider instanceof Dimension) {
            final Dimension dim = (Dimension) tileProvider, oppositeDim = (Dimension) relatedTileProvider;
            bottomless = dim.isBottomless();
            topLayersRelativeToTerrain = dim.getTopLayerAnchor() == Dimension.LayerAnchor.TERRAIN;
            seed = dim.getSeed();
            if (renderCeilingIntersection) {
                final int reflectionPoint = (dim.getAnchor().invert ? dim.getCeilingHeight() : oppositeDim.getCeilingHeight()) + oppositeDim.getMinHeight();
                final Tile relatedTile = oppositeDim.getTile(tile.getX(), tile.getY());
                if (relatedTile != null) {
                    Arrays.fill(oppositesOverlap, false);
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            if ((reflectionPoint - relatedTile.getIntHeight(x, y)) <= intHeightCache[x | (y << TILE_SIZE_BITS)]) {
                                oppositesOverlap[x | (y << TILE_SIZE_BITS)] = true;
                                noOpposites = false;
                            }
                        }
                    }
                }
            } else if (renderTunnelRoofIntersection) {
                final Dimension detailDimension = (Dimension) relatedTileProvider;
                final int minZ = detailDimension.getMinHeight() + (detailDimension.isBottomless() ? 0 : 1);
                final int maxZ = detailDimension.getMaxHeight() - 1;
                Arrays.fill(oppositesOverlap, false);
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final int worldX = (tileX << TILE_SIZE_BITS) | x, worldY = (tileY << TILE_SIZE_BITS) | y;
                        final int terrainHeight = detailDimension.getIntHeightAt(worldX, worldY);
                        final int tunnelFloorLevel = tunnelLayerHelper.calculateFloorLevel(worldX, worldY, terrainHeight, minZ, maxZ);
                        if (intHeightCache[x | (y << TILE_SIZE_BITS)] >= tunnelLayerHelper.calculateRoofLevel(worldX, worldY, terrainHeight, minZ, maxZ, tunnelFloorLevel)) {
                            oppositesOverlap[x | (y << TILE_SIZE_BITS)] = true;
                            noOpposites = false;
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
        if (hiddenLayers.contains(ALL_TUNNELS_AS_LAYER)) {
            layerList.removeIf(layer -> layer instanceof TunnelLayer);
        }
        final boolean _void = layerList.contains(org.pepsoft.worldpainter.layers.Void.INSTANCE), notAllBlocksPresent = layerList.contains(NotPresent.INSTANCE) || layerList.contains(NotPresentBlock.INSTANCE);
        if (hideAllLayers) {
            layerList.clear();
        } else {
            layerList.removeIf(layer -> (layer instanceof Void) || (layer instanceof NotPresent) || (layer instanceof NotPresentBlock));
        }
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
                        final int worldX = (tileX << TILE_SIZE_BITS) | x, worldY = (tileY << TILE_SIZE_BITS) | y;
                        if (notAllBlocksPresent && (tile.getBitLayerValue(NotPresent.INSTANCE, x, y) || tile.getBitLayerValue(NotPresentBlock.INSTANCE, x, y))) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = notPresentColour;
                        } else if ((! noOpposites) && oppositesOverlap[x | (y << TILE_SIZE_BITS)] && CEILING_PATTERN[x & 0x7][y & 0x7]) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = 0xff000000;
                        } else if (_void && tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y)) {
                            renderBuffer[x | (y << TILE_SIZE_BITS)] = voidColour;
                            // TODO still render ReadOnly, and layers which might still be exported over Void
                        } else {
                            int colour = getPixelColour(tile, worldX, worldY, layers, renderers, contourLines, hideTerrain, hideFluids, bottomless, topLayersRelativeToTerrain, seed);
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
                        final int worldX = (tileX << TILE_SIZE_BITS) | x, worldY = (tileY << TILE_SIZE_BITS) | y;
                        if (notAllBlocksPresent && (tile.getBitLayerValue(NotPresent.INSTANCE, x, y) || tile.getBitLayerValue(NotPresentBlock.INSTANCE, x, y))) {
                            renderBuffer[x / scale + y * tileSize] = notPresentColour;
                        } else if ((! noOpposites) && oppositesOverlap[x | (y << TILE_SIZE_BITS)]) {
                            renderBuffer[x / scale + y * tileSize] = 0xff000000;
                        } else if (_void && tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y)) {
                            renderBuffer[x / scale + y * tileSize] = voidColour;
                        } else {
                            int colour = getPixelColour(tile, worldX, worldY, layers, renderers, contourLines, hideTerrain, hideFluids, bottomless, topLayersRelativeToTerrain, seed);
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

    private int getPixelColour(Tile tile, int worldX, int worldY, Layer[] layers, LayerRenderer[] renderers, boolean contourLines, boolean hideTerrain, boolean hideFluids, boolean bottomless, boolean topLayersRelativeToTerrain, long seed) {
        final int x = worldX & TILE_SIZE_MASK, y = worldY & TILE_SIZE_MASK;
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
            colour = (colourRamp != null) ? colourRamp.getColour(floatHeightCache[offset]) : LIGHT_GREY;
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
    private final TileProvider tileProvider, relatedTileProvider;
    private final boolean renderCeilingIntersection, renderTunnelRoofIntersection, transparentVoid;
    private final TunnelLayerHelper tunnelLayerHelper;
    private final ColourRamp colourRamp;
    private ColourScheme colourScheme;
    private boolean contourLines = true, hideAllLayers;
    private int contourSeparation = 10, waterColour, lavaColour, bedrockColour, notPresentColour, voidColour;
    private LightOrigin lightOrigin = LightOrigin.NORTHWEST;

    public static final Layer TERRAIN_AS_LAYER = new Layer("org.pepsoft.synthetic.Terrain", "Terrain", "The terrain type of the surface", Layer.DataSize.NONE, false, 0) {
        @Override
        public BufferedImage getIcon() {
            return ICON;
        }

        private final BufferedImage ICON = IconUtils.scaleIcon(IconUtils.loadScaledImage("org/pepsoft/worldpainter/resources/terrain.png"), 16);
    };
    public static final Layer FLUIDS_AS_LAYER = new Layer("org.pepsoft.synthetic.Fluids", "Water/Lava", "Areas flooded with water or lava", Layer.DataSize.NONE, false, 0) {
        @Override
        public BufferedImage getIcon() {
            return ICON;
        }

        private final BufferedImage ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/resources/fluids.png");
    };
    public static final Layer ALL_TUNNELS_AS_LAYER = new Layer("org.pepsoft.synthetic.Tunnels", "Cave/Tunnel Layers", "All Custom Cave/Tunnel Layers", Layer.DataSize.NONE, false, 0) {
        @Override
        public BufferedImage getIcon() {
            return ICON;
        }

        private final BufferedImage ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/resources/tunnels.png");
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