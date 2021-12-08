package org.pepsoft.worldpainter.platforms;

import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MCRegionChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.awt.*;
import java.io.File;
import java.util.regex.Pattern;

import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;

public final class MCRegionPlatformProvider extends JavaPlatformProvider {
    public MCRegionPlatformProvider() {
        super(Version.VERSION, JAVA_MCREGION);
    }

    @Override
    public NBTChunk createChunk(Platform platform, Tag tag, int maxHeight, boolean readOnly) {
        ensurePlatformSupported(platform);
        return new MCRegionChunk((CompoundTag) tag, maxHeight, readOnly);
    }

    @Override
    public File[] getRegionFiles(Platform platform, File regionDir) {
        ensurePlatformSupported(platform);
        return regionDir.listFiles((dir, name) -> REGION_FILE_PATTERN.matcher(name).matches());
    }

    @Override
    protected File getRegionFileFile(Platform platform, File regionDir, Point coords) {
        ensurePlatformSupported(platform);
        return new File(regionDir, "r." + coords.x + "." + coords.y + ".mcr");
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        ensurePlatformSupported(platform);
        return new Java1_2PostProcessor();
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        ensurePlatformSupported(platform);
        return new MCRegionChunk(x, z, maxHeight);
    }

    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
}
