package org.pepsoft.worldpainter.dynmap;

import org.dynmap.common.BiomeMap;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.MapIterator;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MC113AnvilChunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import static org.pepsoft.minecraft.Constants.BLK_WOOL;
import static org.pepsoft.minecraft.Constants.DATA_MAGENTA;

/**
 * Implementation of {@link MapIterator} used by {@link WPDynmapWorld}.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
class WPMapIterator implements MapIterator {
    @SuppressWarnings("unused")
    WPMapIterator(MinecraftWorld world, int x, int y, int z) {
        this.world = world;
        maxHeight = world.getMaxHeight();
        initialize(x, y, z);
    }

    public void initialize(int x, int y, int z) {
        this.x = x;
        this.y = z;
        this.height = y;
    }

    public int getBlockSkyLight() {
        return world.getSkyLightLevel(x, y, height);
    }

    public int getBlockEmittedLight() {
        return world.getBlockLightLevel(x, y, height);
    }

    public BiomeMap getBiome() {
        Chunk chunk = world.getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return BiomeMap.byBiomeID(chunk.getBiome(x & 0xf, y & 0xf));
        } else {
            return BiomeMap.NULL;
        }
    }

    public int getSmoothGrassColorMultiplier(int[] colormap) {
        return 0xffffff;
    }

    public int getSmoothFoliageColorMultiplier(int[] colormap) {
        return 0xffffff;
    }

    public int getSmoothWaterColorMultiplier() {
        return 0xffffff;
    }

    public int getSmoothWaterColorMultiplier(int[] colormap) {
        return 0xffffff;
    }

    public int getSmoothColorMultiplier(int[] colormap, int[] swampcolormap) {
        return 0xffffff;
    }

    public void stepPosition(BlockStep step) {
        x += step.xoff;
        y += step.zoff;
        height += step.yoff;
        lastStep = step;
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
    }

    public int getBlockTypeIDAt(BlockStep s) {
        int steppedHeight = height + s.yoff;
        if ((steppedHeight >= 0) && (steppedHeight < maxHeight)) {
            int blockType = world.getBlockTypeAt(x + s.xoff, y + s.zoff, height + s.yoff);
            return (blockType >= 0) ? blockType : BLK_WOOL;
        } else {
            return 0;
        }
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
        Chunk chunk = world.getChunk(x >> 4, y >> 4);
        if (chunk instanceof MC12AnvilChunk) {
            return ! ((MC12AnvilChunk) chunk).isSectionPresent(height >> 4);
        } else if (chunk instanceof MC113AnvilChunk) {
            return ! ((MC113AnvilChunk) chunk).isSectionPresent(height >> 4);
        } else {
            return chunk == null;
        }
    }

    public long getInhabitedTicks() {
        Chunk chunk = world.getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getInhabitedTime();
        } else {
            return 0;
        }
    }

    public RenderPatchFactory getPatchFactory() {
        return HDBlockModels.getPatchDefinitionFactory();
    }

    public int getBlockTypeID() {
        int blockType = world.getBlockTypeAt(x, y ,height);
        return (blockType >= 0) ? blockType : BLK_WOOL;
    }

    public int getBlockData() {
        int data = world.getDataAt(x, y ,height);
        return (data >= 0) ? data : DATA_MAGENTA;
    }

    public Object getBlockTileEntityField(String fieldId) {
        return null;
    }

    public int getBlockTypeIDAt(int xoff, int yoff, int zoff) {
        int offsetHeight = height + yoff;
        if ((offsetHeight >= 0) && (offsetHeight < maxHeight)) {
            int blockType = world.getBlockTypeAt(x + xoff, y + zoff, offsetHeight);
            return (blockType >= 0) ? blockType : BLK_WOOL;
        } else {
            return 0;
        }
    }

    public int getBlockDataAt(int xoff, int yoff, int zoff) {
        int offsetHeight = height + yoff;
        if ((offsetHeight >= 0) && (offsetHeight < maxHeight)) {
            int data = world.getDataAt(x + xoff, y + zoff, offsetHeight);
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

    private final MinecraftWorld world;
    private final int maxHeight;
    private int x, y, height;
    private BlockStep lastStep;
}