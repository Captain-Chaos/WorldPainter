package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;

import java.util.List;

/**
 * A {@link Chunk} that is backed by a {@link MinecraftWorld} and a chunk offset.
 */
public class VirtualChunk implements Chunk {
    public VirtualChunk(MinecraftWorld world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkOffsetX = chunkX << 4;
        this.chunkOffsetZ = chunkZ << 4;
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        return world.getBlockLightLevel(chunkOffsetX + x, chunkOffsetZ + z, y);
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        world.setBlockLightLevel(chunkOffsetX + x, chunkOffsetZ + z, y, blockLightLevel);
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        return world.getBlockTypeAt(chunkOffsetX + x, chunkOffsetZ + z, y);
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        world.setBlockTypeAt(chunkOffsetX + x, chunkOffsetZ + z, y, blockType);
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        return world.getDataAt(chunkOffsetX + x, chunkOffsetZ + z, y);
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        world.setDataAt(chunkOffsetX + x, chunkOffsetZ + z, y, dataValue);
    }

    @Override
    public int getHeight(int x, int z) {
        return world.getMinHeight();
    }

    @Override
    public void setHeight(int x, int z, int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        return world.getSkyLightLevel(chunkOffsetX + x, chunkOffsetZ + z, y);
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        world.setSkyLightLevel(chunkOffsetX + x, chunkOffsetZ + z, y, skyLightLevel);
    }

    @Override
    public int getxPos() {
        return chunkOffsetX >> 4;
    }

    @Override
    public int getzPos() {
        return chunkOffsetZ >> 4;
    }

    @Override
    public MinecraftCoords getCoords() {
        return new MinecraftCoords(chunkOffsetX >> 4, chunkOffsetZ >> 4);
    }

    @Override
    public boolean isTerrainPopulated() {
        return false;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return world.getMaterialAt(chunkOffsetX + x, chunkOffsetZ + z, y);
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        world.setMaterialAt(chunkOffsetX + x, chunkOffsetZ + z, y, material);
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
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    /**
     * Returns {@code true} in order to support generating 3D previews, but this class does not actually store any
     * biomes.
     */
    @Override
    public boolean is3DBiomesSupported() {
        return true;
    }

    /**
     * Implemented in order to support generating 3D previews, but this class does not actually store any biomes.
     */
    @Override
    public void set3DBiome(int x, int y, int z, int biome) {
        // Do nothing
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isLightPopulated() {
        return false;
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getInhabitedTime() {
        return 0;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        return world.getHighestNonAirBlock(chunkOffsetX + x, chunkOffsetZ + z);
    }

    @Override
    public int getHighestNonAirBlock() {
        final int maxValue = world.getMaxHeight() - 1;
        int highestNonAirBlock = Integer.MIN_VALUE;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int worldHighestNonAirBlock = world.getHighestNonAirBlock(chunkOffsetX + x, chunkOffsetZ + z);
                if (worldHighestNonAirBlock > highestNonAirBlock) {
                    if (worldHighestNonAirBlock == maxValue) {
                        return maxValue;
                    } else {
                        highestNonAirBlock = worldHighestNonAirBlock;
                    }
                }
            }
        }
        return highestNonAirBlock;
    }

    private final MinecraftWorld world;
    private final int chunkOffsetX, chunkOffsetZ;
}