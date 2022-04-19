package org.pepsoft.worldpainter.platforms;

import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.MCRegionChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;

public final class MCRegionPlatformProvider extends JavaPlatformProvider {
    public MCRegionPlatformProvider() {
        super(Version.VERSION, JAVA_MCREGION);
    }

    @Override
    public NBTChunk createChunk(Platform platform, Map<DataType, Tag> tags, int maxHeight, boolean readOnly) {
        ensurePlatformSupported(platform);
        return new MCRegionChunk((CompoundTag) tags.get(REGION), maxHeight, readOnly);
    }

    @Override
    public File[] getRegionFiles(Platform platform, File regionDir, DataType dataType) {
        ensurePlatformSupported(platform);
        if (dataType != REGION) {
            throw new IllegalArgumentException("Only REGION data type is supported");
        }
        return regionDir.listFiles((dir, name) -> REGION_FILE_PATTERN.matcher(name).matches());
    }

    @Override
    protected File getRegionFileFile(Platform platform, File regionDir, DataType dataType, Point coords) {
        ensurePlatformSupported(platform);
        if (dataType != REGION) {
            throw new IllegalArgumentException("Only REGION data type is supported");
        }
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
