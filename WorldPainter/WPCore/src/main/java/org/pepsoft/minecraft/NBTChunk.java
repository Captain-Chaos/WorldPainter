package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;

/**
 * A {@link Chunk} whose data is stored as an NBT tag of type
 * {@link CompoundTag}.
 */
public abstract class NBTChunk extends AbstractNBTItem implements Chunk {
    protected NBTChunk(CompoundTag tag) {
        super(tag);
    }
}