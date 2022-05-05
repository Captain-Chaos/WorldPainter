package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Material.TILE_ENTITY_MAP;

public abstract class MCNamedBlocksChunk extends NBTChunk {
    protected MCNamedBlocksChunk(CompoundTag tag) {
        super(tag);
    }

    protected MCNamedBlocksChunk(Map<DataType, CompoundTag> tags) {
        super(tags);
    }

    protected void normalise() {
        // Check that all tile entities for which the chunk contains data are actually there
        for (Iterator<TileEntity> i = getTileEntities().iterator(); i.hasNext(); ) {
            final TileEntity tileEntity = i.next();
            final Set<String> blockNames = TILE_ENTITY_MAP.get(tileEntity.getId());
            if (blockNames == null) {
                logger.warn("Unknown tile entity ID \"" + tileEntity.getId() + "\" encountered @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + "; can't check whether the corresponding block is there!");
            } else {
                final Material existingMaterial = getMaterial(tileEntity.getX() & 0xf, tileEntity.getY(), tileEntity.getZ() & 0xf);
                if (! blockNames.contains(existingMaterial.name)) {
                    // The block at the specified location is not a tile entity, or a different tile entity. Remove the
                    // data
                    i.remove();
                    logger.warn("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location is " + existingMaterial);
                }
            }
        }
        // Check that there aren't multiple tile entities (of the same type, otherwise they would have been removed
        // above) in the same location
        Set<Point3i> occupiedCoords = new HashSet<>();
        for (Iterator<TileEntity> i = getTileEntities().iterator(); i.hasNext(); ) {
            TileEntity tileEntity = i.next();
            Point3i coords = new Point3i(tileEntity.getX(), tileEntity.getZ(), tileEntity.getY());
            if (occupiedCoords.contains(coords)) {
                // There is already tile data for that location in the chunk;
                // remove this copy
                i.remove();
                logger.warn("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because there is already a tile entity of the same type at that location");
            } else {
                occupiedCoords.add(coords);
            }
        }
        // TODO: find a fast way of checking for all tile entity blocks that there is actualy tile entity data present
    }

    private static final Logger logger = LoggerFactory.getLogger(MCNamedBlocksChunk.class);
}