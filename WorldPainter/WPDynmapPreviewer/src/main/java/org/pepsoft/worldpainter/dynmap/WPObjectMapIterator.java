package org.pepsoft.worldpainter.dynmap;

import org.dynmap.common.BiomeMap;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.MapIterator;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;

import static org.pepsoft.minecraft.Constants.BLK_WOOL;
import static org.pepsoft.minecraft.Constants.DATA_MAGENTA;

/**
 * Implemenation of {@link MapIterator} used by {@link WPObjectDynmapWorld}.
 *
 * <p>Created by Pepijn Schmitz on 09-06-15.
 */
class WPObjectMapIterator implements MapIterator {
    WPObjectMapIterator(WPObject object, int x, int y, int z) {
        this.object = object;
        Point3i offset = object.getOffset();
        xOffset = offset.x;
        yOffset = offset.y;
        Point3i dimensions = object.getDimensions();
        bounds = new Box(xOffset, dimensions.x + xOffset, yOffset, dimensions.y + yOffset, 0, dimensions.z);
        maxHeight = object.getDimensions().z;
        initialize(x, y, z);
    }

    public void initialize(int x, int y, int z) {
        this.x = x;
        this.y = z;
        this.height = y;
        updateMaterial();
    }

    public int getBlockSkyLight() {
        throw new UnsupportedOperationException();
    }

    public int getBlockEmittedLight() {
        return material.blockLight;
    }

    public BiomeMap getBiome() {
        return BiomeMap.NULL;
    }

    public int getSmoothGrassColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    public int getSmoothFoliageColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    public int getSmoothWaterColorMultiplier() {
        return 0xffffff;
    }

    public int getSmoothWaterColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    public int getSmoothColorMultiplier(int[] colormap, int[] swampcolormap) {
        return colormap[0];
    }

    public void stepPosition(BlockStep step) {
        x += step.xoff;
        y += step.zoff;
        height += step.yoff;
        lastStep = step;
        updateMaterial();
    }

    public void unstepPosition(BlockStep step) {
        stepPosition(step.opposite());
    }

    public BlockStep unstepPosition() {
        unstepPosition(lastStep);
        return lastStep.opposite();
    }

    public void setY(int y) {
        if (y > this.height) {
            lastStep = BlockStep.Y_PLUS;
        } else if (y < this.height) {
            lastStep = BlockStep.Y_MINUS;
        }
        height = y;
        updateMaterial();
    }

    public int getBlockTypeIDAt(BlockStep s) {
        return getBlockTypeIDAt(s.xoff, s.yoff, s.zoff);
    }

    public BlockStep getLastStep() {
        return lastStep;
    }

    public int getWorldHeight() {
        return maxHeight;
    }

    public long getBlockKey() {
        return height | ((x & 0x00000fff) << 20) | ((y & 0x00000fff) << 8);
    }

    public boolean isEmptySection() {
        return false;
    }

    public long getInhabitedTicks() {
        return 0;
    }

    public RenderPatchFactory getPatchFactory() {
        return HDBlockModels.getPatchDefinitionFactory();
    }

    public int getBlockTypeID() {
        return material.blockType >= 0 ? material.blockType : BLK_WOOL;
    }

    public int getBlockData() {
        return material.data >= 0 ? material.data : DATA_MAGENTA;
    }

    public Object getBlockTileEntityField(String fieldId) {
        return null;
    }

    public int getBlockTypeIDAt(int xoff, int yoff, int zoff) {
        int offsetX = x + xoff;
        int offsetY = y + zoff;
        int offsetHeight = height + yoff;
        if (object.getMask(offsetX - xOffset, offsetY - yOffset, offsetHeight)) {
            int blockType = object.getMaterial(offsetX - xOffset, offsetY - yOffset, offsetHeight).blockType;
            return (blockType >= 0) ? blockType : BLK_WOOL;
        } else {
            return 0;
        }
    }

    public int getBlockDataAt(int xoff, int yoff, int zoff) {
        int offsetX = x + xoff;
        int offsetY = y + zoff;
        int offsetHeight = height + yoff;
        if (object.getMask(offsetX - xOffset, offsetY - yOffset, offsetHeight)) {
            int data = object.getMaterial(offsetX - xOffset, offsetY - yOffset, offsetHeight).blockType;
            return (data >= 0) ? data : DATA_MAGENTA;
        } else {
            return 0;
        }
    }

    public Object getBlockTileEntityFieldAt(String fieldId, int xoff, int yoff, int zoff) {
        return null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return height;
    }

    public int getZ() {
        return y;
    }

    private void updateMaterial() {
        if (bounds.contains(x, y, height)) {
            material = object.getMask(x - xOffset, y - yOffset, height) ? object.getMaterial(x - xOffset, y - yOffset, height) : Material.AIR;
        } else {
            material = Material.AIR;
        }
    }

    private final WPObject object;
    private final Box bounds;
    private final int maxHeight, xOffset, yOffset;
    private int x, y, height;
    private BlockStep lastStep;
    private Material material;
}