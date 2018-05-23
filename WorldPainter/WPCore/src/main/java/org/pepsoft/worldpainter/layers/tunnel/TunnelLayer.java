/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.MixedMaterialManager;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
    public void setColour(int colour) {
        super.setColour(colour);
        renderer = new TunnelLayerRenderer(this);
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

    public Map<Layer, LayerSettings> getFloorLayers() {
        return floorLayers;
    }

    public void setFloorLayers(Map<Layer, LayerSettings> floorLayers) {
        this.floorLayers = floorLayers;
    }

    // CustomLayer
    
    @Override
    public TunnelLayerExporter getExporter() {
        return new TunnelLayerExporter(this);
    }

    @Override
    public TunnelLayerRenderer getRenderer() {
        return renderer;
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
        if (floorLayers != null) {
            clone.floorLayers = new HashMap<>();
            for (Map.Entry<Layer, LayerSettings> entry: floorLayers.entrySet()) {
                clone.floorLayers.put(entry.getKey(), entry.getValue().clone());
            }
        }
        clone.renderer = new TunnelLayerRenderer(clone);
        return clone;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        renderer = new TunnelLayerRenderer(this);
    }
    
    private Mode roofMode = Mode.FIXED_HEIGHT, floorMode = Mode.FIXED_HEIGHT;
    private int roofLevel = 88, floorLevel = 80, floorWallDepth = 4, roofWallDepth = 4, roofMin = 0, roofMax = Integer.MAX_VALUE, floorMin = 0, floorMax = Integer.MAX_VALUE, floodLevel;
    private boolean stalactites, stalagmites, floodWithLava, removeWater;
    private MixedMaterial floorMaterial, wallMaterial, roofMaterial;
    private NoiseSettings floorNoise, roofNoise, wallNoise;
    private Map<Layer, LayerSettings> floorLayers;
    private transient TunnelLayerRenderer renderer = new TunnelLayerRenderer(this);
    
    private static final long serialVersionUID = 1L;
    
    public enum Mode {FIXED_HEIGHT, CONSTANT_DEPTH, INVERTED_DEPTH}
    
    public static class LayerSettings implements Serializable, Cloneable {
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
        
        @Override
        public LayerSettings clone() {
            try {
                LayerSettings clone = (LayerSettings) super.clone();
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
        private int minLevel = 0, maxLevel = Constants.DEFAULT_MAX_HEIGHT_ANVIL - 1;
        
        private static final long serialVersionUID = 1L;
    }
}