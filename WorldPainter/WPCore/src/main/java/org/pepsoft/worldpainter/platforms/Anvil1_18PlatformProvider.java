package org.pepsoft.worldpainter.platforms;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MC115AnvilChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.exporting.Java1_15PostProcessor;
import org.pepsoft.worldpainter.exporting.PostProcessor;

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
        return new MC115AnvilChunk(x, z, maxHeight);
    }

    /* TODO */
}