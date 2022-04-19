package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.MC118AnvilChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.util.Set;

import static org.pepsoft.minecraft.DataType.ENTITIES;
import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_18;

public final class Anvil1_18PlatformProvider extends AnvilPlatformProvider {
    public Anvil1_18PlatformProvider() {
        super(Version.VERSION, JAVA_ANVIL_1_18);
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        ensurePlatformSupported(platform);
        return new Java1_15PostProcessor();
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        ensurePlatformSupported(platform);
        return new MC118AnvilChunk(x, z, maxHeight);
    }

    @Override
    public Set<DataType> getDataTypes() {
        return DATA_TYPES;
    }

    private static final Set<DataType> DATA_TYPES = ImmutableSet.of(REGION, ENTITIES);
}