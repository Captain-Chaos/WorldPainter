package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.MC115AnvilChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.util.Set;

import static org.pepsoft.minecraft.DataType.REGION;

final class Anvil1_15PlatformProvider extends AnvilPlatformProvider {
    @Override
    PostProcessor getPostProcessor() {
        return new Java1_15PostProcessor();
    }

    @Override
    Set<DataType> getDataTypes() {
        return DATA_TYPES;
    }

    @Override
    NBTChunk createChunk(int x, int z, int maxHeight) {
        return new MC115AnvilChunk(x, z, maxHeight);
    }

    private static final Set<DataType> DATA_TYPES = ImmutableSet.of(REGION);
}