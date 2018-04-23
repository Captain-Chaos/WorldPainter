/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.pockets;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.MixedMaterialManager;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.CustomLayer;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 * @author pepijn
 */
public class UndergroundPocketsLayer extends CustomLayer {
    public UndergroundPocketsLayer(String name, MixedMaterial material, Terrain terrain, int frequency, int minLevel, int maxLevel, int scale, int colour) {
        super(name, "underground pockets of " + name, DataSize.NIBBLE, 15, colour);
        if ((frequency < 1) || (frequency > 1000)) {
            throw new IllegalArgumentException("frequency");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale");
        }
        if (minLevel < 0) {
            throw new IllegalArgumentException("minLevel");
        }
        if (maxLevel < minLevel) {
            throw new IllegalArgumentException("maxLevel");
        }
        mixedMaterial = material;
        this.terrain = terrain;
        this.frequency = frequency;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.scale = scale;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        setDescription("underground pockets of " + name);
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public MixedMaterial getMaterial() {
        return mixedMaterial;
    }

    public void setMaterial(MixedMaterial material) {
        mixedMaterial = material;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public UndergroundPocketsLayerExporter getExporter() {
        return new UndergroundPocketsLayerExporter(this);
    }

    // Cloneable

    @Override
    public UndergroundPocketsLayer clone() {
        UndergroundPocketsLayer clone = (UndergroundPocketsLayer) super.clone();
        if (mixedMaterial != null) {
            clone.mixedMaterial = mixedMaterial.clone();
            MixedMaterialManager.getInstance().register(clone.mixedMaterial);
        }
        return clone;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy
        if (material != null) {
            mixedMaterial = MixedMaterial.create(material);
            material = null;
        }
    }

    @Deprecated
    private Material material;
    private int scale, frequency, maxLevel, minLevel;
    private MixedMaterial mixedMaterial;
    private Terrain terrain;

    private static final long serialVersionUID = 1L;
}