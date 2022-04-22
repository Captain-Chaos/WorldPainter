package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

import java.util.Map;

/**
 * A {@link Chunk} whose data is stored as one or more NBT tags of type {@link CompoundTag}.
 */
public abstract class NBTChunk extends AbstractNBTItem implements Chunk {
    public NBTChunk(CompoundTag tag) {
        super(tag);
    }

    public NBTChunk(Map<DataType, CompoundTag> tags) {
        super(tags);
    }
}