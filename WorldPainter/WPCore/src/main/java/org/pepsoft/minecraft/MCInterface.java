package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.pepsoft.worldpainter.BiomeScheme;

/**
 * A service provider of various Minecraft related services.
 *
 * <p>Created by Pepijn on 26-6-2016.
 */
public interface MCInterface extends BiomeScheme {
    /**
     * Decode block information from the palette in a structure file.
     *
     * @param tag The NBT compound tag from the palette in the structure file.
     * @return The decoded material.
     */
    Material decodeStructureMaterial(CompoundTag tag);
}