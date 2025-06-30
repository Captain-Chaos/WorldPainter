/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.renderers.PaintRenderer;
import org.pepsoft.worldpainter.operations.Filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Role.*;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.FillMode.AIR;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.FillMode.CAVE_AIR;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.CAVE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.CUSTOM_DIMENSION;

/**
 *
 * @author SchmitzP
 */
public class TunnelLayer extends CustomLayer {
    public TunnelLayer(String name, LayerMode mode, Object paint, Platform platform) {
        super(name, name, DataSize.BIT, 22, paint);
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        this.mode = mode;
        fillMode = platform.capabilities.contains(NAME_BASED) ? CAVE_AIR : AIR;
    }

    public Mode getRoofMode() {
        return roofMode;
    }

    public void setRoofMode(Mode roofMode) {
        this.roofMode = roofMode;
    }

    public Mode getFloorMode() {
        return floorMode;
    }

    public void setFloorMode(Mode floorMode) {
        this.floorMode = floorMode;
    }

    public int getRoofLevel() {
        return roofLevel;
    }

    public void setRoofLevel(int roofLevel) {
        this.roofLevel = roofLevel;
    }

    public int getFloorLevel() {
        return floorLevel;
    }

    public void setFloorLevel(int floorLevel) {
        this.floorLevel = floorLevel;
    }

    public int getFloorWallDepth() {
        return floorWallDepth;
    }

    public void setFloorWallDepth(int floorWallDepth) {
        this.floorWallDepth = floorWallDepth;
    }

    public int getRoofWallDepth() {
        return roofWallDepth;
    }

    public void setRoofWallDepth(int roofWallDepth) {
        this.roofWallDepth = roofWallDepth;
    }

    public boolean isStalactites() {
        return stalactites;
    }

    public void setStalactites(boolean stalactites) {
        this.stalactites = stalactites;
    }

    public boolean isStalagmites() {
        return stalagmites;
    }

    public void setStalagmites(boolean stalagmites) {
        this.stalagmites = stalagmites;
    }

    public MixedMaterial getFloorMaterial() {
        return floorMaterial;
    }

    public void setFloorMaterial(MixedMaterial floorMaterial) {
        this.floorMaterial = floorMaterial;
    }

    public MixedMaterial getWallMaterial() {
        return wallMaterial;
    }

    public void setWallMaterial(MixedMaterial wallMaterial) {
        this.wallMaterial = wallMaterial;
    }

    public MixedMaterial getRoofMaterial() {
        return roofMaterial;
    }

    public void setRoofMaterial(MixedMaterial roofMaterial) {
        this.roofMaterial = roofMaterial;
    }

    public NoiseSettings getFloorNoise() {
        return floorNoise;
    }

    public void setFloorNoise(NoiseSettings floorNoise) {
        this.floorNoise = floorNoise;
    }

    public NoiseSettings getRoofNoise() {
        return roofNoise;
    }

    public void setRoofNoise(NoiseSettings roofNoise) {
        this.roofNoise = roofNoise;
    }

    public NoiseSettings getWallNoise() {
        return wallNoise;
    }

    public void setWallNoise(NoiseSettings wallNoise) {
        this.wallNoise = wallNoise;
    }

    public int getRoofMin() {
        return roofMin;
    }

    public void setRoofMin(int roofMin) {
        this.roofMin = roofMin;
    }

    public int getRoofMax() {
        return roofMax;
    }

    public void setRoofMax(int roofMax) {
        this.roofMax = roofMax;
    }

    public int getFloorMin() {
        return floorMin;
    }

    public void setFloorMin(int floorMin) {
        this.floorMin = floorMin;
    }

    public int getFloorMax() {
        return floorMax;
    }

    public void setFloorMax(int floorMax) {
        this.floorMax = floorMax;
    }

    public boolean isRemoveWater() {
        return removeWater;
    }

    public void setRemoveWater(boolean removeWater) {
        this.removeWater = removeWater;
    }

    @Override
    public void setMinMaxHeight(int oldMinHeight, int newMinHeight, int oldMaxHeight, int newMaxHeight, HeightTransform transform) {
        adjustLayers(orderedFloorLayers, oldMinHeight, newMinHeight, oldMaxHeight, newMaxHeight);
        adjustLayers(orderedRoofLayers, oldMinHeight, newMinHeight, oldMaxHeight, newMaxHeight);
    }

    @Override
    public String getType() {
        switch (mode) {
            case CAVE:
                return "Cave/Tunnel";
            case FLOATING:
                return "Floating Dimension";
            default:
                throw new InternalError("Unknown mode " + mode);
        }
    }

    public int getFloodLevel() {
        return floodLevel;
    }

    public void setFloodLevel(int floodLevel) {
        this.floodLevel = floodLevel;
    }

    public boolean isFloodWithLava() {
        return floodWithLava;
    }

    public void setFloodWithLava(boolean floodWithLava) {
        this.floodWithLava = floodWithLava;
    }

    public List<LayerSettings> getFloorLayers() {
        return orderedFloorLayers;
    }

    public void setFloorLayers(List<LayerSettings> floorLayers) {
        orderedFloorLayers = floorLayers;
    }

    public List<LayerSettings> getRoofLayers() {
        return orderedRoofLayers;
    }

    public void setRoofLayers(List<LayerSettings> roofLayers) {
        orderedRoofLayers = roofLayers;
    }

    public Integer getTunnelBiome() {
        return tunnelBiome;
    }

    public void setTunnelBiome(Integer biome) {
        this.tunnelBiome = biome;
    }

    public Integer getFloorDimensionId() {
        return floorDimensionId;
    }

    public void setFloorDimensionId(Integer floorDimensionId) {
        this.floorDimensionId = floorDimensionId;
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    public void setFillMode(FillMode fillMode) {
        this.fillMode = fillMode;
    }

    public int getFillLightLevel() {
        return fillLightLevel;
    }

    public void setFillLightLevel(int fillLightLevel) {
        this.fillLightLevel = fillLightLevel;
    }

    public MixedMaterial getFillMaterial() {
        return fillMaterial;
    }

    public void setFillMaterial(MixedMaterial fillMaterial) {
        this.fillMaterial = fillMaterial;
    }

    public LayerMode getLayerMode() {
        return mode;
    }

    public EdgeShape getBottomEdgeShape() {
        return bottomEdgeShape;
    }

    public void setBottomEdgeShape(EdgeShape bottomEdgeShape) {
        this.bottomEdgeShape = bottomEdgeShape;
    }

    public boolean isApplyBiomesAboveGround() {
        return applyBiomesAboveGround;
    }

    public void setApplyBiomesAboveGround(boolean applyBiomesAboveGround) {
        this.applyBiomesAboveGround = applyBiomesAboveGround;
    }

    public boolean isApplyBiomesBelowGround() {
        return applyBiomesBelowGround;
    }

    public void setApplyBiomesBelowGround(boolean applyBiomesBelowGround) {
        this.applyBiomesBelowGround = applyBiomesBelowGround;
    }

    /**
     * Ensure that tiles exist in the floor dimension for this layer for all the places where the layer is painted, and
     * mark out the shape of the layer on the floor dimension using the {@link NotPresentBlock} layer.
     */
    public Dimension updateFloorDimension(Dimension dimension, String name) {
        final Anchor anchor = dimension.getAnchor();
        final Dimension floorDimension = dimension.getWorld().getDimension(new Anchor(anchor.dim, (mode == CAVE) ? CAVE_FLOOR : FLOATING_FLOOR, anchor.invert, floorDimensionId));
        if (name != null) {
            floorDimension.setName(name);
        }
        final TileFactory tileFactory = floorDimension.getTileFactory();
        floorDimension.setEventsInhibited(true);
        try {
            dimension.visitTiles().forFilter(Filter.build(dimension).onlyOn(this).build()).andDo(tile -> {
                Tile floorTile = floorDimension.getTileForEditing(tile.getX(), tile.getY());
                if (floorTile == null) {
                    floorTile = tileFactory.createTile(tile.getX(), tile.getY());
                    floorDimension.addTile(floorTile);
                } else {
                    floorTile.clearLayerData(NotPresentBlock.INSTANCE);
                }
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        if (! tile.getBitLayerValue(this, x, y)) {
                            floorTile.setBitLayerValue(NotPresentBlock.INSTANCE, x, y, true);
                        }
                    }
                }
            });
        } finally {
            floorDimension.setEventsInhibited(false);
        }
        return floorDimension;
    }

    /**
     * Ensure that tiles exist in the floor dimension for this layer for all the places where the layer is painted. That
     * might not be the case if the user has painted the layer in additional places but not then edited the floor
     * dimension to fill in those places.
     */
    public void updateFloorDimensionTiles(Dimension dimension) {
        final Anchor anchor = dimension.getAnchor();
        final Dimension floorDimension = dimension.getWorld().getDimension(new Anchor(anchor.dim, (mode == CAVE) ? CAVE_FLOOR : FLOATING_FLOOR, anchor.invert, floorDimensionId));
        final TileFactory tileFactory = floorDimension.getTileFactory();
        floorDimension.setEventsInhibited(true);
        try {
            dimension.visitTiles().forFilter(Filter.build(dimension).onlyOn(this).build()).andDo(tile -> {
                Tile floorTile = floorDimension.getTileForEditing(tile.getX(), tile.getY());
                if (floorTile == null) {
                    floorTile = tileFactory.createTile(tile.getX(), tile.getY());
                    floorDimension.addTile(floorTile);
                }
            });
        } finally {
            floorDimension.setEventsInhibited(false);
        }
    }

    /**
     * Get a helper for applying this layer to a particular dimension.
     */
    public TunnelLayerHelper getHelper(Dimension dimension) {
        return new TunnelLayerHelper(this, dimension);
    }

    /**
     * Find the {@code TunnelLayer} instance with which a particular cave floor dimension is associated.
     *
     * @param floorDimension The cave floor dimension for which to find the {@code TunnelLayer}.
     * @return The {@code TunnelLayer} with which the specified floor dimension is associated.
     * @throws IllegalArgumentException If the specified dimension is not a cave floor dimension.
     * @throws IllegalStateException If no {@code TunnelLayer} can be found, or if multiple {@code TunnelLayer}s claim
     * to be associated with the specified floor dimension.
     */
    public static TunnelLayer find(Dimension floorDimension) {
        final Anchor floorAnchor = floorDimension.getAnchor();
        if ((floorAnchor.role != CAVE_FLOOR) && (floorAnchor.role != FLOATING_FLOOR)){
            throw new IllegalArgumentException("Not a CAVE_FLOOR or FLOATING_FLOOR dimension");
        }
        final Anchor detailAnchor = new Anchor(floorAnchor.dim, DETAIL, floorAnchor.invert, 0);
        final Dimension detailDimension = floorDimension.getWorld().getDimension(detailAnchor);
        final LayerMode layerMode = (floorAnchor.role == CAVE_FLOOR) ? CAVE : FLOATING;
        if (detailDimension != null) {
            for (CustomLayer layer: detailDimension.getCustomLayers()) {
                if ((layer instanceof TunnelLayer)
                        && (((TunnelLayer) layer).getLayerMode() == layerMode)
                        && (((TunnelLayer) layer).getFloorDimensionId() != null)
                        && (((TunnelLayer) layer).getFloorDimensionId() == floorAnchor.id)) {
                    return (TunnelLayer) layer;
                }
            }
            throw new IllegalArgumentException("Could not find TunnelLayer for floor dimension " + floorAnchor);
        } else {
            throw new IllegalArgumentException("Could not find detail dimension for floor dimension " + floorAnchor);
        }
    }

    public static boolean isLayerTypeSupportedForCaveFloorDimension(Class<? extends Layer> layerType) {
        return ! (Caves.class.isAssignableFrom(layerType)
                || Caverns.class.isAssignableFrom(layerType)
                || Chasms.class.isAssignableFrom(layerType)
                || Resources.class.isAssignableFrom(layerType)
                || Populate.class.isAssignableFrom(layerType)
                || ReadOnly.class.isAssignableFrom(layerType)
                || TunnelLayer.class.isAssignableFrom(layerType)
                || UndergroundPocketsLayer.class.isAssignableFrom(layerType));
    }

    public static boolean isLayerTypeSupportedForFloatingFloorDimension(Class<? extends Layer> layerType) {
        // TODO support more layers for floating dimensions
        return isLayerTypeSupportedForCaveFloorDimension(layerType);
    }

    public static boolean isLayerSupportedForCaveFloorDimension(Layer layer) {
        if (! isLayerTypeSupportedForCaveFloorDimension(layer.getClass())) {
            return false;
        }
        if (layer instanceof CombinedLayer) {
            for (Layer constituentLayer: ((CombinedLayer) layer).getLayers()) {
                if (! isLayerSupportedForCaveFloorDimension(constituentLayer)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isLayerSupportedForFloatingFloorDimension(Layer layer) {
        if (! isLayerTypeSupportedForFloatingFloorDimension(layer.getClass())) {
            return false;
        }
        if (layer instanceof CombinedLayer) {
            for (Layer constituentLayer: ((CombinedLayer) layer).getLayers()) {
                if (! isLayerSupportedForFloatingFloorDimension(constituentLayer)) {
                    return false;
                }
            }
        }
        return true;
    }

    // CustomLayer

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        switch (mode) {
            case CAVE:
                return TunnelLayerExporter.class;
            case FLOATING:
                return FloatingLayerExporter.class;
            default:
                throw new InternalError("Unknown mode " + mode);
        }
    }

    @Override
    public AbstractTunnelLayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        switch (mode) {
            case CAVE:
                return new TunnelLayerExporter(dimension, platform, this, getHelper(dimension)); // TODO creating the helper is not necessary to do for every exporter instance
            case FLOATING:
                return new FloatingLayerExporter(dimension, platform, this, getHelper(dimension)); // TODO creating the helper is not necessary to do for every exporter instance
            default:
                throw new InternalError("Unknown mode " + mode);
        }
    }

    @Override
    public PaintRenderer getRenderer() {
        switch (mode) {
            case CAVE:
                return new TunnelLayerRenderer(this);
            case FLOATING:
                return new PaintRenderer(getPaint(), getOpacity());
            default:
                throw new InternalError("Unknown mode " + mode);
        }
    }

    @Override
    public boolean isExportableToFile() {
        return floorMode != CUSTOM_DIMENSION;
    }

    // Cloneable

    @Override
    public TunnelLayer clone() {
        if (floorMode == CUSTOM_DIMENSION) {
            throw new MDCCapturingRuntimeException("TunnelLayers with a floor dimension are not Cloneable");
        }
        TunnelLayer clone = (TunnelLayer) super.clone();
        MixedMaterialManager mixedMaterialManager = MixedMaterialManager.getInstance();
        if (floorMaterial != null) {
            clone.floorMaterial = floorMaterial.clone();
            mixedMaterialManager.register(clone.floorMaterial);
        }
        if (wallMaterial != null) {
            clone.wallMaterial = wallMaterial.clone();
            mixedMaterialManager.register(clone.wallMaterial);
        }
        if (roofMaterial != null) {
            clone.roofMaterial = roofMaterial.clone();
            mixedMaterialManager.register(clone.roofMaterial);
        }
        if (floorNoise != null) {
            clone.floorNoise = floorNoise.clone();
        }
        if (roofNoise != null) {
            clone.roofNoise = roofNoise.clone();
        }
        if (wallNoise != null) {
            clone.wallNoise = wallNoise.clone();
        }
        if (orderedFloorLayers != null) {
            clone.orderedFloorLayers = new ArrayList<>(orderedFloorLayers.size());
            for (LayerSettings layerSettings: orderedFloorLayers) {
                clone.orderedFloorLayers.add(layerSettings.clone());
            }
        }
        if (orderedRoofLayers != null) {
            clone.orderedRoofLayers = new ArrayList<>(orderedRoofLayers.size());
            for (LayerSettings layerSettings: orderedRoofLayers) {
                clone.orderedRoofLayers.add(layerSettings.clone());
            }
        }
        return clone;
    }

    private void adjustLayers(List<LayerSettings> layers, int oldMinHeight, int newMinHeight, int oldMaxHeight, int newMaxHeight) {
        if (layers != null) {
            layers.forEach(layerSettings -> {
                if ((layerSettings.getMinLevel() == oldMinHeight) || (layerSettings.getMinLevel() < newMinHeight)){
                    layerSettings.setMinLevel(newMinHeight);
                }
                if ((layerSettings.getMaxLevel() == (oldMaxHeight - 1)) || (layerSettings.getMaxLevel() >= newMaxHeight)){
                    layerSettings.setMaxLevel(newMaxHeight - 1);
                }
            });
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (wpVersion < 1) {
            if (roofMin == 0) {
                roofMin = Integer.MIN_VALUE;
            }
            if (floorMin == 0) {
                floorMin = Integer.MIN_VALUE;
            }
            if (floodLevel == 0) {
                floodLevel = Integer.MIN_VALUE;
            }
        }
        if (wpVersion < 2) {
            if (floorLayers != null) {
                orderedFloorLayers = new ArrayList<>(floorLayers.size());
                for (Map.Entry<Layer, LayerSettings> entry: floorLayers.entrySet()) {
                    entry.getValue().setLayer(entry.getKey());
                    orderedFloorLayers.add(entry.getValue());
                }
                floorLayers = null;
            }
        }
        if (wpVersion < 3) {
            fillMode = AIR;
            fillLightLevel = 8;
        }
        if (wpVersion < 4) {
            mode = CAVE;
        }
        wpVersion = CURRENT_WP_VERSION;
    }

    // The "roof" settings double for the bottom of floating dimensions
    Mode roofMode = Mode.FIXED_HEIGHT_ABOVE_FLOOR, floorMode = Mode.FIXED_HEIGHT;
    int roofLevel = 16, floorLevel = 32, floorWallDepth = 4, roofWallDepth = 4, roofMin = Integer.MIN_VALUE, roofMax = Integer.MAX_VALUE, floorMin = Integer.MIN_VALUE, floorMax = Integer.MAX_VALUE, floodLevel = Integer.MIN_VALUE;
    private boolean stalactites, stalagmites, floodWithLava, removeWater;
    private MixedMaterial floorMaterial, wallMaterial, roofMaterial;
    NoiseSettings floorNoise, roofNoise, wallNoise;
    @Deprecated private Map<Layer, LayerSettings> floorLayers;
    private Integer tunnelBiome;
    private int wpVersion = CURRENT_WP_VERSION;
    private List<LayerSettings> orderedFloorLayers, orderedRoofLayers;
    Integer floorDimensionId;
    private FillMode fillMode;
    private int fillLightLevel = 8;
    private MixedMaterial fillMaterial;
    private LayerMode mode;
    Integer roofDimensionId;
    private EdgeShape bottomEdgeShape;
    private boolean applyBiomesAboveGround, applyBiomesBelowGround;

    private static final int CURRENT_WP_VERSION = 4;
    private static final long serialVersionUID = 1L;
    
    public enum Mode { FIXED_HEIGHT, CONSTANT_DEPTH, INVERTED_DEPTH, CUSTOM_DIMENSION, FIXED_HEIGHT_ABOVE_FLOOR }
    public enum FillMode { CAVE_AIR, AIR, LIGHT, MIXED_MATERIAL }

    public enum LayerMode {
        /**
         * In cave mode this layer will carve out a void dictated by the floor and roof level settings and apply the
         * floor features to the terrain at the bottom of the void and the roof features, if any, to the terrain above
         * the void, if any.
         */
        CAVE,

        /**
         * In floating mode this layer will not carve out a void but place terrain at the level of the floor dimension,
         * to the depth dictated by the roof level settings. It will apply the floor features to the top of the terrain
         * and the roof features, if any, to the bottom of the terrain. In this mode the layer must always have a floor
         * dimension, since it dictates the height of the floor. TODO: explore non-dimension floor level modes?
         */
        FLOATING
    }

    public enum EdgeShape {SHEER, LINEAR, SMOOTH, ROUNDED}

    public static class LayerSettings implements Serializable, Cloneable {
        public LayerSettings(int minLevel, int maxLevel) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public Layer getLayer() {
            return layer;
        }

        public void setLayer(Layer layer) {
            this.layer = layer;
        }

        public int getIntensity() {
            return intensity;
        }

        public void setIntensity(int intensity) {
            this.intensity = intensity;
        }

        public NoiseSettings getVariation() {
            return variation;
        }

        public void setVariation(NoiseSettings variation) {
            this.variation = variation;
        }

        public int getMinLevel() {
            return minLevel;
        }

        public void setMinLevel(int minLevel) {
            this.minLevel = minLevel;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public void setMaxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
        }

        public boolean isInvert() {
            return invert;
        }

        public void setInvert(boolean invert) {
            this.invert = invert;
        }

        @Override
        public LayerSettings clone() {
            try {
                LayerSettings clone = (LayerSettings) super.clone();
                if (layer instanceof CustomLayer) {
                    clone.layer = ((CustomLayer) layer).clone();
                }
                if (variation != null) {
                    clone.variation = variation.clone();
                }
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
        }
        
        /**
         * The base intensity with which the layer will be applied.
         */
        private int intensity = 50;
        
        /**
         * The random variation which will be applied to the layer intensity.
         */
        private NoiseSettings variation;
        
        /**
         * The minimum and maximum heights at which the layer should be applied.
         */
        private int minLevel, maxLevel;

        /**
         * Whether this layer should be inverted on export. Only applies to roof layers.
         */
        private boolean invert;

        private Layer layer;
        
        private static final long serialVersionUID = 1L;
    }
}