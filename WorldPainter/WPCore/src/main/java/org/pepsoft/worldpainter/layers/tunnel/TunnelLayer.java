/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.NotPresentBlock;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.operations.Filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;

/**
 *
 * @author SchmitzP
 */
public class TunnelLayer extends CustomLayer {
    public TunnelLayer(String name, int colour) {
        super(name, name, DataSize.BIT, 22, colour);
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
        return "Cave/Tunnel";
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

    public Dimension updateFloorDimension(Dimension dimension, String name) {
        final Anchor anchor = dimension.getAnchor();
        final Dimension floorDimension = dimension.getWorld().getDimension(new Anchor(anchor.dim, CAVE_FLOOR, anchor.invert, floorDimensionId));
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
        if (floorAnchor.role != CAVE_FLOOR) {
            throw new IllegalArgumentException("Not a CAVE_FLOOR dimension");
        }
        final Anchor detailAnchor = new Anchor(floorAnchor.dim, DETAIL, floorAnchor.invert, 0);
        final Dimension detailDimension = floorDimension.getWorld().getDimension(detailAnchor);
        if (detailDimension != null) {
            for (CustomLayer layer: detailDimension.getCustomLayers()) {
                if ((layer instanceof TunnelLayer)
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

    // CustomLayer

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return TunnelLayerExporter.class;
    }

    @Override
    public TunnelLayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new TunnelLayerExporter(dimension, platform, this, getHelper(dimension)); // TODO creating the helper is not necessary to do for every exporter instance
    }

    @Override
    public TunnelLayerRenderer getRenderer() {
        return new TunnelLayerRenderer(this);
    }

    // Cloneable

    @Override
    public TunnelLayer clone() {
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
        wpVersion = CURRENT_WP_VERSION;
    }

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

    private static final int CURRENT_WP_VERSION = 2;
    private static final long serialVersionUID = 1L;
    
    public enum Mode { FIXED_HEIGHT, CONSTANT_DEPTH, INVERTED_DEPTH, CUSTOM_DIMENSION, FIXED_HEIGHT_ABOVE_FLOOR }

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