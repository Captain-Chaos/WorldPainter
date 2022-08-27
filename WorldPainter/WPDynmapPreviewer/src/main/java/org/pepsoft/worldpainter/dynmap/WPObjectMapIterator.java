package org.pepsoft.worldpainter.dynmap;

import org.dynmap.common.BiomeMap;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.renderer.DynmapBlockState;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.MapIterator;

import static org.dynmap.renderer.DynmapBlockState.AIR;

/**
 * Implemenation of {@link MapIterator} used by {@link WPObjectDynmapWorld}.
 *
 * <p>Created by Pepijn Schmitz on 09-06-15.
 */
class WPObjectMapIterator implements MapIterator {
    WPObjectMapIterator(WPObjectDynmapWorld object, int x, int y, int z) {
        this.object = object;
        initialize(x, y, z);
    }

    @Override
    public void initialize(int x, int y, int z) {
        this.x = x;
        this.y = z;
        this.height = y;
        updateMaterial();
    }

    @Override
    public int getBlockSkyLight() {
        return 15;
    }

    @Override
    public int getBlockEmittedLight() {
        return object.getLightLevel(x, y, height);
    }

    @Override
    public int getBlockLight(BlockStep blockStep) {
        return getBlockLight(blockStep.xoff, blockStep.yoff, blockStep.zoff);
    }

    @Override
    public int getBlockLight(int xoff, int yoff, int zoff) {
        final int offsetX = x + xoff;
        final int offsetY = y + zoff;
        final int offsetHeight = height + yoff;
        return 3840 + object.getLightLevel(offsetX - object.xOffset, offsetY - object.yOffset, offsetHeight);
    }

    @Override
    public BiomeMap getBiome() {
        return BiomeMap.NULL;
    }

    @Override
    public int getSmoothGrassColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    @Override
    public int getSmoothFoliageColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    @Override
    public int getSmoothWaterColorMultiplier() {
        return 0xffffff;
    }

    @Override
    public int getSmoothWaterColorMultiplier(int[] colormap) {
        return colormap[0];
    }

    @Override
    public int getSmoothColorMultiplier(int[] colormap, int[] swampcolormap) {
        return colormap[0];
    }

    @Override
    public void stepPosition(BlockStep step) {
        x += step.xoff;
        y += step.zoff;
        height += step.yoff;
        lastStep = step;
        updateMaterial();
    }

    @Override
    public void unstepPosition(BlockStep step) {
        stepPosition(step.opposite());
    }

    @Override
    public BlockStep unstepPosition() {
        unstepPosition(lastStep);
        return lastStep.opposite();
    }

    @Override
    public void setY(int y) {
        if (y > this.height) {
            lastStep = BlockStep.Y_PLUS;
        } else if (y < this.height) {
            lastStep = BlockStep.Y_MINUS;
        }
        height = y;
        updateMaterial();
    }

    @Override
    public DynmapBlockState getBlockTypeAt(BlockStep blockStep) {
        return getBlockTypeAt(blockStep.xoff, blockStep.yoff, blockStep.zoff);
    }

    @Override
    public BlockStep getLastStep() {
        return lastStep;
    }

    @Override
    public int getWorldHeight() {
        return object.bounds.getHeight();
    }

    @Override
    public long getBlockKey() {
        return height | ((x & 0x00000fffL) << 20) | ((y & 0x00000fff) << 8);
    }

    @Override
    public long getInhabitedTicks() {
        return 0;
    }

    @Override
    public RenderPatchFactory getPatchFactory() {
        return HDBlockModels.getPatchDefinitionFactory();
    }

    @Override
    public DynmapBlockState getBlockType() {
        return material;
    }

    @Override
    public Object getBlockTileEntityField(String fieldId) {
        // TODO
        return null;
    }

    @Override
    public DynmapBlockState getBlockTypeAt(int xoff, int yoff, int zoff) {
        final int offsetX = x + xoff;
        final int offsetY = y + zoff;
        final int offsetHeight = height + yoff;
        if (object.bounds.contains(offsetX - object.xOffset, offsetY - object.yOffset, offsetHeight)) {
            return object.blockStates[offsetX - object.xOffset][offsetY - object.yOffset][offsetHeight];
        } else {
            return AIR;
        }
    }

    @Override
    public Object getBlockTileEntityFieldAt(String fieldId, int xoff, int yoff, int zoff) {
        // TODO
        return null;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return height;
    }

    @Override
    public int getZ() {
        return y;
    }

    private void updateMaterial() {
        if (object.bounds.contains(x, y, height)) {
            material = object.blockStates[x - object.xOffset][y - object.yOffset][height];
        } else {
            material = AIR;
        }
    }

    private final WPObjectDynmapWorld object;
    private int x, y, height;
    private BlockStep lastStep;
    private DynmapBlockState material;
}