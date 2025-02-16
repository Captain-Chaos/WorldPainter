/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A plant.
 *
 * @author pepijn
 */
public abstract class Plant implements WPObject {
    protected Plant(String name, Material material, String iconName, Category... categories) {
        this(name, material, new String[] { iconName }, categories);
    }

    protected Plant(String name, Material material, String[] iconNames, Category... categories) {
        this.name = name;
        this.material = material;
        this.categories = categories;
        this.iconNames = iconNames;
    }

    /**
     * Get the categories of the plant. If there is more than one, the first one is considered the "main" category.
     *
     * @return The categories of the plant.
     */
    public final Category[] getCategories() {
        return categories;
    }

    /**
     * Determine whether the block at a particular location in a particular map is a valid foundation on which to place
     * this plant. This will always be invoked by {@link PlantLayerExporter} before exporting a plant.
     *
     * @param world           The map in which to check the foundation.
     * @param x               The X coordinate (in the WorldPainter coordinate system) to check.
     * @param y               The Y coordinate (in the WorldPainter coordinate system) to check.
     * @param height          The Z coordinate (in the WorldPainter coordinate system) to check.
     * @param checkBlockBelow Whether the block underneath the plant should be checked for validity.
     * @return The applicable category if this plant may be placed on the block at the specified location; {@code null}
     * otherwise.
     */
    public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
        for (Category category: categories) {
            if (category.isValidFoundation(world, x, y, height, checkBlockBelow)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Get the maximum growth stage of this plant, where 1 is the minimum growth
     * stage. Should be 1 for plants without growth stages.
     *
     * @return The maximum growth stage of this plant.
     */
    public int getMaxGrowth() {
        return 1;
    }

    /**
     * The default growth stage to present to the user. The intent is for this to be the maximum height of plants that
     * vanilla Minecraft will generate, with {@code maxGrowth} optionally being higher.
     *
     * @return The default growth stage to present to the user.
     */
    public int getDefaultGrowth() {
        return getMaxGrowth();
    }

    /**
     * Obtain a version of the plant suitable for actually placing in a map.
     * This will always be invoked by {@link PlantLayerExporter} before
     * exporting a plant. It is meant for plants with varying growth stages and/
     * or which will return different materials for different platforms.
     * Implementations to which neither applies may simply return themselves.
     *
     * @param growth The growth stage of the plant to export. Always 1 for
     *               plants without growth stages.
     * @param platform The platform for which the plant is being exported.
     * @return A version of the plant configured for the specified growth stage
     * and platform.
     */
    public Plant realise(int growth, Platform platform) {
        return this;
    }
    
    // WPObject
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return material;
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return true;
    }

    @Override
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return null;
    }

    @Override
    public void prepareForExport(Dimension dimension) {
        // Do nothing
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public Point3i getOffset() {
        return new Point3i(0, 0, 0);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone") // Plants are unmodifiable
    @Override
    public WPObject clone() {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
    
    String[] getIconNames() {
        return iconNames;
    }

    protected final String name;
    protected final Material material;
    protected final Category[] categories;

    private final String[] iconNames;
}