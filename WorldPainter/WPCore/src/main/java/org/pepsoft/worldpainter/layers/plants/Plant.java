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
    protected Plant(String name, Material material, Category category, String iconName) {
        this.name = name;
        this.material = material;
        this.category = category;
        this.iconName = iconName;
    }

    public final Category getCategory() {
        return category;
    }

    public boolean isValidFoundation(MinecraftWorld world, int x, int y, int height) {
        return category.isValidFoundation(world, x, y, height);
    }

    public int getMaxGrowth() {
        return 1;
    }
    
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

    protected final String name, iconName;
    protected final Material material;
    protected final Category category;
}