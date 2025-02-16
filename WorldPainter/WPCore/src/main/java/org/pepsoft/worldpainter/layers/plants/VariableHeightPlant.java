package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import javax.vecmath.Point3i;
import java.util.Optional;

/**
 * A plant with variable height. Bottom and top blocks are provided by
 * overridable methods so that subclasses can optionally manage them.
 */
class VariableHeightPlant extends PlantWithGrowth {
    VariableHeightPlant(String name, Material middleMaterial, int defaultHeight, Category... category) {
        this(name, category, "block/" + middleMaterial.simpleName + ".png", null, middleMaterial, null, defaultHeight, defaultHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, String iconName, int defaultHeight, Category... category) {
        this(name, category, iconName, null, middleMaterial, null, defaultHeight, defaultHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, Material topMaterial, int defaultHeight, Category... category) {
        this(name, category, "block/" + topMaterial.simpleName + ".png", null, middleMaterial, topMaterial, defaultHeight, defaultHeight, null);
    }

    VariableHeightPlant(String name, Material middleMaterial, Material topMaterial, String iconName, int defaultHeight, Category... category) {
        this(name, category, iconName, null, middleMaterial, topMaterial, defaultHeight, defaultHeight, null);
    }

    VariableHeightPlant(String name, Material bottomMaterial, Material middleMaterial, Material topMaterial, int defaultHeight, Category... category) {
        this(name, category, "block/" + topMaterial.simpleName + ".png", bottomMaterial, middleMaterial, topMaterial, defaultHeight, defaultHeight, null);
    }

    VariableHeightPlant(String name, Material bottomMaterial, Material middleMaterial, Material topMaterial, String iconName, int defaultHeight, Category... category) {
        this(name, category, iconName, bottomMaterial, middleMaterial, topMaterial, defaultHeight, defaultHeight, null);
    }

    private VariableHeightPlant(String name, Category[] categories, String iconName, Material bottomMaterial, Material middleMaterial, Material topMaterial, int defaultGrowth, int growth, Platform platform) {
        this(name, categories, new String[] { iconName }, bottomMaterial, middleMaterial, topMaterial, defaultGrowth, growth, platform);
    }

    private VariableHeightPlant(String name, Category[] categories, String[] iconNames, Material bottomMaterial, Material middleMaterial, Material topMaterial, int defaultGrowth, int growth, Platform platform) {
        super(name, middleMaterial, iconNames, DEFAULT_MAX_GROWTH, categories);
        this.bottomMaterial = bottomMaterial;
        this.topMaterial = topMaterial;
        this.defaultGrowth = defaultGrowth;
        this.growth = growth;
        this.platform = platform;
        dimensions = new Point3i(1, 1, growth);
    }

    @Override
    public int getDefaultGrowth() {
        return defaultGrowth;
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
    public Plant realise(int growth, Platform platform) {
        return new VariableHeightPlant(name, categories, getIconNames(), bottomMaterial, material, topMaterial, defaultGrowth, growth, platform);
    }

    Optional<Material> getBottomMaterial() {
        return Optional.ofNullable(bottomMaterial);
    }

    Optional<Material> getTopMaterial() {
        return Optional.ofNullable(topMaterial);
    }

    public static final int DEFAULT_MAX_GROWTH = 99;

    protected final Material bottomMaterial, topMaterial;
    protected final int defaultGrowth, growth;
    protected final Point3i dimensions;
    protected final Platform platform;
}
