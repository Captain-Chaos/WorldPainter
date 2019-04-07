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

import static org.pepsoft.minecraft.Constants.MC_CARROTS;
import static org.pepsoft.minecraft.Constants.MC_POTATOES;
import static org.pepsoft.minecraft.Material.AGE;
import static org.pepsoft.worldpainter.layers.plants.Category.CROPS;

/**
 * A simple plant with one material type and an optional growth stage
 * corresponding to the height of the plant.
 *
 * @author pepijn
 */
public class Plant implements WPObject {
    public Plant(String name, Material material, Category category, String iconName) {
        this(name, material, 1, 0, category, iconName);
    }

    public Plant(String name, Material material, int height, Category category, String iconName) {
        this(name, material, height, 0, category, iconName);
    }

    protected Plant(String name, Material material, int height, int maxGrowth, Category category, String iconName) {
        this.name = name;
        if (category == CROPS) {
            // Adjust the material for the specified maximum growth factor:
            if (material.isNamed(MC_CARROTS) || material.isNamed(MC_POTATOES)) {
                if (maxGrowth == 3) {
                    this.material = material.withProperty(AGE, 7);
                } else {
                    this.material = material.withProperty(AGE, maxGrowth * 2);
                }
            } else {
                this.material =  material.withProperty(AGE, maxGrowth);
            }
        } else if (category == Category.NETHER) {
            this.material = material.withProperty(AGE, maxGrowth + ((maxGrowth > 0) ? 1 : 0));
        } else {
            this.material = material;
        }
        this.category = category;
        this.maxData = maxGrowth;
        this.iconName = iconName;
        dimensions = new Point3i(1, 1, height);
        growth = maxGrowth;
        platform = null;
    }
    
    private Plant(Plant plant, int growth, Platform platform) {
        name = plant.name;
        category = plant.category;
        maxData = plant.maxData;
        iconName = plant.iconName;
        this.growth = growth;
        this.platform = platform;
        switch (category) {
            case CACTUS:
            case SUGAR_CANE:
                material = plant.material;
                dimensions = new Point3i(1, 1, Math.min(growth + 1, plant.dimensions.z));
                break;
            case CROPS:
                if (plant.material.isNamed(MC_CARROTS) || plant.material.isNamed(MC_POTATOES)) {
                    if (growth == 3) {
                        material = plant.material.withProperty(AGE, 7);
                    } else {
                        material = plant.material.withProperty(AGE, growth * 2);
                    }
                } else {
                    material = plant.material.withProperty(AGE, growth);
                }
                dimensions = plant.dimensions;
                break;
            case NETHER:
                material = plant.material.withProperty(AGE, growth + ((growth > 0) ? 1 : 0));
                dimensions = plant.dimensions;
                break;
            default:
                material = plant.material;
                dimensions = plant.dimensions;
                break;
        }
    }
    
    public Category getCategory() {
        return category;
    }

    public int getMaxData() {
        return maxData;
    }
    
    public boolean isValidFoundation(MinecraftWorld world, int x, int y, int height) {
        return category.isValidFoundation(world, x, y, height);
    }
    
    public Plant withGrowth(int growth, Platform platform) {
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
    public Point3i getDimensions() {
        return dimensions;
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
    public <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        return key.defaultValue;
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
    
    String getIconName() {
        return iconName;
    }

    private final String name, iconName;
    private final Point3i dimensions;
    protected final Material material;
    private final Category category;
    private final int maxData, growth;
    protected final Platform platform;
    
}