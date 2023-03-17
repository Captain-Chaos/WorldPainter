package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.MC118AnvilChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.util.Set;

import static org.pepsoft.minecraft.DataType.ENTITIES;
import static org.pepsoft.minecraft.DataType.REGION;

final class Anvil1_18PlatformProvider extends AnvilPlatformProvider {
    @Override
    PostProcessor getPostProcessor() {
        return new Java1_15PostProcessor();
    }

    @Override
    NBTChunk createChunk(int x, int z, int minHeight, int maxHeight) {
        return new MC118AnvilChunk(x, z, minHeight, maxHeight);
    }

    @Override
    Set<DataType> getDataTypes() {
        return DATA_TYPES;
    }

    private static final Set<DataType> DATA_TYPES = ImmutableSet.of(REGION, ENTITIES);
}