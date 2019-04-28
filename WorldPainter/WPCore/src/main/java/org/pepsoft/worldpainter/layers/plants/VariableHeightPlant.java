package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import javax.vecmath.Point3i;
import java.util.Optional;

/**
 * A plant with variable height. Bottom and top blocks are provided by
 * overridable methods so that subclasses can optionally manage them.
 */
class VariableHeightPlant extends Plant {
    VariableHeightPlant(String name, Material middleMaterial, Category category, int maxHeight) {
        this(name, category, "block/" + middleMaterial.simpleName + ".png", null, middleMaterial, null, maxHeight, maxHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, Category category, String iconName, int maxHeight) {
        this(name, category, iconName, null, middleMaterial, null, maxHeight, maxHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, Material topMaterial, Category category, int maxHeight) {
        this(name, category, "block/" + topMaterial.simpleName + ".png", null, middleMaterial, topMaterial, maxHeight, maxHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, Material topMaterial, Category category, String iconName, int maxHeight) {
        this(name, category, iconName, null, middleMaterial, topMaterial, maxHeight, maxHeight, null);
    }

    private VariableHeightPlant(String name, Category category, String iconName, Material bottomMaterial, Material middleMaterial, Material topMaterial, int maxGrowth, int growth, Platform platform) {
        super(name, middleMaterial, category, iconName);
        this.bottomMaterial = bottomMaterial;
        this.topMaterial = topMaterial;
        this.maxGrowth = maxGrowth;
        this.growth = growth;
        this.platform = platform;
        dimensions = new Point3i(1, 1, growth);
    }

    @Override
    public int getMaxGrowth() {
        return maxGrowth;
    }

    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        if (z == (growth - 1)) {
            // Top block
            return getTopMaterial().orElse(material);
        } else if (z == 0) {
            // Bottom block
            return getBottomMaterial().orElse(material);
        } else {
            // Middle block
            return material;
        }
    }

    @Override
    public VariableHeightPlant realise(int growth, Platform platform) {
        return new VariableHeightPlant(name, category, iconName, bottomMaterial, material, topMaterial, maxGrowth, growth, platform);
    }

    Optional<Material> getBottomMaterial() {
        return Optional.ofNullable(bottomMaterial);
    }

    Optional<Material> getTopMaterial() {
        return Optional.ofNullable(topMaterial);
    }

    protected final Material bottomMaterial, topMaterial;
    protected final int maxGrowth, growth;
    protected final Point3i dimensions;
    protected final Platform platform;
}
