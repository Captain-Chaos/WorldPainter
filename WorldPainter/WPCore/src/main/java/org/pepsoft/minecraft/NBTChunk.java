package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import java.util.List;

/**
 * A {@link Chunk} whose data is stored as an NBT tag of type
 * {@link CompoundTag}.
 */
public class NBTChunk extends AbstractNBTItem implements Chunk {
    public NBTChunk(CompoundTag tag) {
        super(tag);
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        return 0;
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {

    }

    @Override
    public int getBlockType(int x, int y, int z) {
        return 0;
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {

    }

    @Override
    public int getDataValue(int x, int y, int z) {
        return 0;
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {

    }

    @Override
    public int getHeight(int x, int z) {
        return 0;
    }

    @Override
    public void setHeight(int x, int z, int height) {

    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        return 0;
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {

    }

    @Override
    public int getxPos() {
        return 0;
    }

    @Override
    public int getzPos() {
        return 0;
    }

    @Override
    public MinecraftCoords getCoords() {
        return null;
    }

    @Override
    public boolean isTerrainPopulated() {
        return false;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {

    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return null;
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {

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
        return 0;
    }

    @Override
    public boolean isBiomesAvailable() {
        return false;
    }

    @Override
    public int getBiome(int x, int z) {
        return 0;
    }

    @Override
    public void setBiome(int x, int z, int biome) {

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

    }

    @Override
    public long getInhabitedTime() {
        return 0;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {

    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        return 0;
    }

    @Override
    public int getHighestNonAirBlock() {
        return 0;
    }
}