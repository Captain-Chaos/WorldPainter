package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableSet;
import org.jnbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.util.*;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;

public abstract class MCNumberedBlocksChunk extends NBTChunk {
    protected MCNumberedBlocksChunk(CompoundTag tag) {
        super(tag);
    }

    protected MCNumberedBlocksChunk(Map<DataType, CompoundTag> tags) {
        super(tags);
    }

    protected void normalise() {
        // Check that all tile entities for which the chunk contains data are actually there
        for (Iterator<TileEntity> i = getTileEntities().iterator(); i.hasNext(); ) {
            final TileEntity tileEntity = i.next();
            final Set<Integer> blockIds = LEGACY_TILE_ENTITY_MAP.get(tileEntity.getId());
            if (blockIds == null) {
                logger.warn("Unknown tile entity ID \"" + tileEntity.getId() + "\" encountered @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + "; can't check whether the corresponding block is there!");
            } else {
                final int existingBlockId = getBlockType(tileEntity.getX() & 0xf, tileEntity.getY(), tileEntity.getZ() & 0xf);
                if (! blockIds.contains(existingBlockId)) {
                    // The block at the specified location is not a tile entity, or a different tile entity. Remove the
                    // data
                    i.remove();
                    if (existingBlockId < BLOCK_TYPE_NAMES.length) {
                        logger.warn("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location is a " + BLOCK_TYPE_NAMES[existingBlockId]);
                    } else {
                        logger.warn("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location has ID " + existingBlockId);
                    }
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

    private static final Map<String, Set<Integer>> LEGACY_TILE_ENTITY_MAP = new HashMap<>();

    static {
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_AIRPORTAL, Collections.singleton(BLK_END_PORTAL));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_BEACON, Collections.singleton(BLK_BEACON));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CAULDRON, Collections.singleton(BLK_BREWING_STAND));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CHEST, ImmutableSet.of(BLK_CHEST, BLK_TRAPPED_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_COMPARATOR, ImmutableSet.of(BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_REDSTONE_COMPARATOR_POWERED));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CONTROL, ImmutableSet.of(BLK_COMMAND_BLOCK, BLK_CHAIN_COMMAND_BLOCK, BLK_REPEATING_COMMAND_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_DLDETECTOR, ImmutableSet.of(BLK_DAYLIGHT_SENSOR, BLK_DAYLIGHT_SENSOR_INVERTED));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_DROPPER, Collections.singleton(BLK_DROPPER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_ENCHANTTABLE, Collections.singleton(BLK_ENCHANTMENT_TABLE));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_ENDERCHEST, Collections.singleton(BLK_ENDER_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_FLOWERPOT, Collections.singleton(BLK_FLOWER_POT));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_FURNACE, ImmutableSet.of(BLK_FURNACE, BLK_BURNING_FURNACE));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_HOPPER, Collections.singleton(BLK_HOPPER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_MOBSPAWNER, Collections.singleton(BLK_MONSTER_SPAWNER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_MUSIC, Collections.singleton(BLK_NOTE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_PISTON, Collections.singleton(BLK_PISTON_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_RECORDPLAYER, Collections.singleton(BLK_JUKEBOX));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SIGN, ImmutableSet.of(BLK_SIGN, BLK_WALL_SIGN));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SKULL, Collections.singleton(BLK_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_TRAP, Collections.singleton(BLK_DISPENSER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_BANNER, ImmutableSet.of(BLK_STANDING_BANNER, BLK_WALL_BANNER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_STRUCTURE, Collections.singleton(BLK_STRUCTURE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SHULKER_BOX, ImmutableSet.of(BLK_WHITE_SHULKER_BOX, BLK_ORANGE_SHULKER_BOX, BLK_MAGENTA_SHULKER_BOX, BLK_LIGHT_BLUE_SHULKER_BOX, BLK_YELLOW_SHULKER_BOX, BLK_LIME_SHULKER_BOX, BLK_PINK_SHULKER_BOX, BLK_GREY_SHULKER_BOX, BLK_LIGHT_GREY_SHULKER_BOX, BLK_CYAN_SHULKER_BOX, BLK_PURPLE_SHULKER_BOX, BLK_BLUE_SHULKER_BOX, BLK_BROWN_SHULKER_BOX, BLK_GREEN_SHULKER_BOX, BLK_RED_SHULKER_BOX, BLK_BLACK_SHULKER_BOX));
        LEGACY_TILE_ENTITY_MAP.put(MC_END_PORTAL, Collections.singleton(BLK_END_PORTAL));
        LEGACY_TILE_ENTITY_MAP.put(MC_BEACON, Collections.singleton(BLK_BEACON));
        LEGACY_TILE_ENTITY_MAP.put(MC_BREWING_STAND, Collections.singleton(BLK_BREWING_STAND));
        LEGACY_TILE_ENTITY_MAP.put(ID_CHEST, ImmutableSet.of(BLK_CHEST, BLK_TRAPPED_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(MC_COMPARATOR, ImmutableSet.of(BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_REDSTONE_COMPARATOR_POWERED));
        LEGACY_TILE_ENTITY_MAP.put(MC_COMMAND_BLOCK, ImmutableSet.of(BLK_COMMAND_BLOCK, BLK_CHAIN_COMMAND_BLOCK, BLK_REPEATING_COMMAND_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(MC_DAYLIGHT_DETECTOR, ImmutableSet.of(BLK_DAYLIGHT_SENSOR, BLK_DAYLIGHT_SENSOR_INVERTED));
        LEGACY_TILE_ENTITY_MAP.put(MC_DROPPER, Collections.singleton(BLK_DROPPER));
        LEGACY_TILE_ENTITY_MAP.put(MC_ENCHANTING_TABLE, Collections.singleton(BLK_ENCHANTMENT_TABLE));
        LEGACY_TILE_ENTITY_MAP.put(MC_ENDER_CHEST, Collections.singleton(BLK_ENDER_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(MC_FLOWER_POT, Collections.singleton(BLK_FLOWER_POT));
        LEGACY_TILE_ENTITY_MAP.put(MC_FURNACE, ImmutableSet.of(BLK_FURNACE, BLK_BURNING_FURNACE));
        LEGACY_TILE_ENTITY_MAP.put(MC_HOPPER, Collections.singleton(BLK_HOPPER));
        LEGACY_TILE_ENTITY_MAP.put(ID_MOB_SPAWNER, Collections.singleton(BLK_MONSTER_SPAWNER));
        LEGACY_TILE_ENTITY_MAP.put(ID_NOTEBLOCK, Collections.singleton(BLK_NOTE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(MC_PISTON, Collections.singleton(BLK_PISTON_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(MC_JUKEBOX, Collections.singleton(BLK_JUKEBOX));
        LEGACY_TILE_ENTITY_MAP.put(ID_SIGN, ImmutableSet.of(BLK_SIGN, BLK_WALL_SIGN));
        LEGACY_TILE_ENTITY_MAP.put(ID_SKULL, Collections.singleton(BLK_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(MC_DISPENSER, Collections.singleton(BLK_DISPENSER));
        LEGACY_TILE_ENTITY_MAP.put(ID_BANNER, ImmutableSet.of(BLK_STANDING_BANNER, BLK_WALL_BANNER));
        LEGACY_TILE_ENTITY_MAP.put(MC_STRUCTURE_BLOCK, Collections.singleton(BLK_STRUCTURE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(MC_SHULKER_BOX, ImmutableSet.of(BLK_WHITE_SHULKER_BOX, BLK_ORANGE_SHULKER_BOX, BLK_MAGENTA_SHULKER_BOX, BLK_LIGHT_BLUE_SHULKER_BOX, BLK_YELLOW_SHULKER_BOX, BLK_LIME_SHULKER_BOX, BLK_PINK_SHULKER_BOX, BLK_GREY_SHULKER_BOX, BLK_LIGHT_GREY_SHULKER_BOX, BLK_CYAN_SHULKER_BOX, BLK_PURPLE_SHULKER_BOX, BLK_BLUE_SHULKER_BOX, BLK_BROWN_SHULKER_BOX, BLK_GREEN_SHULKER_BOX, BLK_RED_SHULKER_BOX, BLK_BLACK_SHULKER_BOX));

        // Make sure the tile entity flag in the block database is consistent
        // with the tile entity map:
        Set<Integer> allTileEntityIds = new HashSet<>();
        for (Set<Integer> blockIdSet: LEGACY_TILE_ENTITY_MAP.values()) {
            allTileEntityIds.addAll(blockIdSet);
            for (int blockId: blockIdSet) {
                if (! Block.BLOCKS[blockId].tileEntity) {
                    throw new AssertionError("Block " + blockId + " not marked as tile entity!");
                }
            }
        }
        for (Block block: Block.BLOCKS) {
            if (block.tileEntity && (! allTileEntityIds.contains(block.id))) {
                throw new AssertionError("Block " + block.id + " marked as tile entity but not present in tile entity map!");
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MCNumberedBlocksChunk.class);
}