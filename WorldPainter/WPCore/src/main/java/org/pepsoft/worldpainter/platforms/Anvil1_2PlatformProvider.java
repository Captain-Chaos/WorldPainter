package org.pepsoft.worldpainter.platforms;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.exporting.Java1_2PostProcessor;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;

public final class Anvil1_2PlatformProvider extends AnvilPlatformProvider {
    public Anvil1_2PlatformProvider() {
        super(Version.VERSION, JAVA_ANVIL);
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        ensurePlatformSupported(platform);
        return new Java1_2PostProcessor();
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        ensurePlatformSupported(platform);
        return new MC12AnvilChunk(x, z, maxHeight);
    }
}