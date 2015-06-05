/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.pepsoft.minecraft.*;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.BLK_AIR;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2;

/**
 *
 * @author pepijn
 */
public class WorldRegion implements MinecraftWorld {
    public WorldRegion(int regionX, int regionZ, int maxHeight, int version) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.maxHeight = maxHeight;
        this.version = version;
    }
    
    public WorldRegion(File regionDir, int regionX, int regionZ, int maxHeight, int version) throws IOException {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.maxHeight = maxHeight;
        this.version = version;
        int lowestX = (regionX << 5) - 1;
        int highestX = lowestX + 33;
        int lowestZ = (regionZ << 5) - 1;
        int highestZ = lowestZ + 33;
        Map<Point, RegionFile> regionFiles = new HashMap<Point, RegionFile>();
//        synchronized (DISK_ACCESS_MONITOR) {
            try {
                for (int x = lowestX; x <= highestX; x++) {
                    for (int z = lowestZ; z <= highestZ; z++) {
                        Point regionCoords = new Point(x >> 5, z >> 5);
                        RegionFile regionFile = regionFiles.get(regionCoords);
                        if (regionFile == null) {
                            File file = new File(regionDir, "r." + regionCoords.x + "." + regionCoords.y + ((version == SUPPORTED_VERSION_2) ? ".mca" : ".mcr"));
                            if (file.isFile()) {
                                regionFile = new RegionFile(file);
                                regionFiles.put(regionCoords, regionFile);
                            }
                        }
                        if (regionFile != null) {
                            DataInputStream chunkIn = regionFile.getChunkDataInputStream((x - (regionX << 5)) & 0x1f, (z - (regionZ << 5)) & 0x1f);
                            if (chunkIn != null) {
                                CompoundTag tag;
                                NBTInputStream in = new NBTInputStream(chunkIn);
                                try {
                                    tag = (CompoundTag) in.readTag();
                                } finally {
                                    in.close();
                                }
                                Chunk chunk = (version == SUPPORTED_VERSION_2) ? new ChunkImpl2(tag, maxHeight) : new ChunkImpl(tag, maxHeight);
                                chunks[x - (regionX << 5) + 1][z - (regionZ << 5) + 1] = chunk;
                            }
                        }
                    }
                }
            } finally {
                for (RegionFile regionFile: regionFiles.values()) {
                    regionFile.close();
                }
            }
//        }
    }
    
    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        if (height >= maxHeight) {
            return BLK_AIR;
        }
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getBlockType(x & 0xf, height, y & 0xf);
        } else {
            return BLK_AIR;
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        if (height >= maxHeight) {
            return 0;
        }
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getDataValue(x & 0xf, height, y & 0xf);
        } else {
            return 0;
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        if (height >= maxHeight) {
            return Material.AIR;
        }
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getMaterial(x & 0xf, height, y & 0xf);
        } else {
            return Material.AIR;
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        if (height >= maxHeight) {
            // Fail silently
            return;
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setBlockType(x & 0xf, height, y & 0xf, blockType);
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        if (height >= maxHeight) {
            // Fail silently
            return;
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setDataValue(x & 0xf, height, y & 0xf, data);
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        if (height >= maxHeight) {
            // Fail silently
            return;
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setMaterial(x & 0xf, height, y & 0xf, material);
        }
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void addEntity(int x, int y, int height, Entity entity) {
        addEntity(x + 0.5, y + 0.5, height + 1.5, entity);
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        Chunk chunk = getChunkForEditing(((int) x) >> 4, ((int) y) >> 4);
        if (chunk != null) {
            Entity clone = (Entity) entity.clone();
            clone.setPos(new double[] {x, height, y});
            chunk.getEntities().add(clone);
        }
    }
    
    @Override
    public void addTileEntity(int x, int y, int height, TileEntity tileEntity) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            TileEntity clone = (TileEntity) tileEntity.clone();
            clone.setX(x);
            clone.setY(height);
            clone.setZ(y);
            chunk.getTileEntities().add(clone);
        }
    }

    @Override
    public int getBlockLightLevel(int x, int y, int height) {
        if (height >= maxHeight) {
            return 0;
        }
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getBlockLightLevel(x & 0xf, height, y & 0xf);
        } else {
            return 0;
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int height, int blockLightLevel) {
        if (height >= maxHeight) {
            // Fail silently
            return;
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setBlockLightLevel(x & 0xf, height, y & 0xf, blockLightLevel);
        }
    }

    @Override
    public int getSkyLightLevel(int x, int y, int height) {
        if (height >= maxHeight) {
            return 15;
        }
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getSkyLightLevel(x & 0xf, height, y & 0xf);
        } else {
            return 0;
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int height, int skyLightLevel) {
        if (height >= maxHeight) {
            // Fail silently
            return;
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setSkyLightLevel(x & 0xf, height, y & 0xf, skyLightLevel);
        }
    }
    
    public boolean containsChunk(int x, int z) {
        x -= regionX << 5;
        z -= regionZ << 5;
        if ((x < -1) || (x >= (CHUNKS_PER_SIDE + 1)) || (z < -1) || (z >= (CHUNKS_PER_SIDE + 1))) {
            return false;
        } else {
            return chunks[x + 1][z + 1] != null;
        }
    }

    @Override
    public Chunk getChunk(int x, int z) {
        x -= regionX << 5;
        z -= regionZ << 5;
        if ((x < -1) || (x >= (CHUNKS_PER_SIDE + 1)) || (z < -1) || (z >= (CHUNKS_PER_SIDE + 1))) {
            return null;
        } else {
            return chunks[x + 1][z + 1];
        }
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        Chunk chunk = getChunk(x, z);
        if (chunkCreationMode && (chunk == null)) {
            int localX = x - (regionX << 5);
            int localZ = z - (regionZ << 5);
            if ((localX >= 0) && (localX < CHUNKS_PER_SIDE) && (localZ >= 0) && (localZ < CHUNKS_PER_SIDE)) {
                chunk = (version == SUPPORTED_VERSION_2) ? new ChunkImpl2(x, z, maxHeight) : new ChunkImpl(x, z, maxHeight);
                chunks[x + 1][z + 1] = chunk;
            }
        }
        return chunk;
    }

    @Override
    public void addChunk(Chunk chunk) {
        int localX = chunk.getxPos() - (regionX << 5);
        int localZ = chunk.getzPos() - (regionZ << 5);
        if ((localX >= -1) && (localX <= CHUNKS_PER_SIDE) && (localZ >= -1) && (localZ <= CHUNKS_PER_SIDE)) {
            chunks[localX + 1][localZ + 1] = chunk;
        }
    }

    @Override
    public int getHighestNonAirBlock(int x, int y) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getHighestNonAirBlock(x & 0xf, y & 0xf);
        } else {
            return -1;
        }
    }

    public void save(File dimensionDir) throws IOException {
        File file = new File(dimensionDir, "region/r." + regionX + "." + regionZ + ((version == SUPPORTED_VERSION_2) ? ".mca" : ".mcr"));
//        synchronized (DISK_ACCESS_MONITOR) {
            RegionFile regionFile = new RegionFile(file);
            try {
                for (int x = 0; x < CHUNKS_PER_SIDE; x++) {
                    for (int z = 0; z < CHUNKS_PER_SIDE; z++) {
                        final Chunk chunk = chunks[x + 1][z + 1];
                        if (chunk != null) {
                            // Do some sanity checks first
                            // Check that all tile entities for which the chunk
                            // contains data are actually there
                            for (Iterator<TileEntity> i = chunk.getTileEntities().iterator(); i.hasNext(); ) {
                                final TileEntity tileEntity = i.next();
                                final Set<Integer> blockIds = Constants.TILE_ENTITY_MAP.get(tileEntity.getId());
                                if (blockIds == null) {
                                    logger.warning("Unknown tile entity ID \"" + tileEntity.getId() + "\" encountered @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + "; can't check whether the corresponding block is there!");
                                } else {
                                    final int existingBlockId = chunk.getBlockType(tileEntity.getX() & 0xf, tileEntity.getY(), tileEntity.getZ() & 0xf);
                                    if (! blockIds.contains(existingBlockId)) {
                                        // The block at the specified location
                                        // is not a tile entity, or a different
                                        // tile entity. Remove the data
                                        i.remove();
                                        if (logger.isLoggable(Level.FINE)) {
                                            logger.fine("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location is a " + BLOCK_TYPE_NAMES[existingBlockId]);
                                        }
                                    }
                                }
                            }
                            // Check that there aren't multiple tile entities (of the same type,
                            // otherwise they would have been removed above) in the same location
                            Set<Point3i> occupiedCoords = new HashSet<Point3i>();
                            for (Iterator<TileEntity> i = chunk.getTileEntities().iterator(); i.hasNext(); ) {
                                TileEntity tileEntity = i.next();
                                Point3i coords = new Point3i(tileEntity.getX(), tileEntity.getZ(), tileEntity.getY());
                                if (occupiedCoords.contains(coords)) {
                                    // There is already tile data for that location in the chunk;
                                    // remove this copy
                                    i.remove();
                                    logger.warning("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because there is already a tile entity of the same type at that location");
                                } else {
                                    occupiedCoords.add(coords);
                                }
                            }
                            
                            NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x, z));
                            try {
                                out.writeTag(chunk.toNBT());
                            } finally {
                                out.close();
                            }
                        }
                    }
                }
            } finally {
                regionFile.close();
            }
//        }
    }

    public boolean isChunkCreationMode() {
        return chunkCreationMode;
    }

    public void setChunkCreationMode(boolean chunkCreationMode) {
        this.chunkCreationMode = chunkCreationMode;
    }
 
    private final int maxHeight, version;
    private final Chunk[][] chunks = new Chunk[CHUNKS_PER_SIDE + 2][CHUNKS_PER_SIDE + 2];
    private final int regionX, regionZ;
    private boolean chunkCreationMode;
    
//    private static final Object DISK_ACCESS_MONITOR = new Object();
    
    public static final int CHUNKS_PER_SIDE = 32;
    
    private static final Logger logger = Logger.getLogger(WorldRegion.class.getName());
}