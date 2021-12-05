package org.pepsoft.worldpainter.platforms;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.MC115AnvilChunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.Platform;

import java.awt.*;
import java.io.File;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.TAG_DATA_VERSION;

public abstract class AnvilPlatformProvider extends JavaPlatformProvider {
    protected AnvilPlatformProvider(String version, Platform platform) {
        super(version, platform);
    }

    @Override
    public final NBTChunk createChunk(Platform platform, Tag tag, int maxHeight, boolean readOnly) {
        ensurePlatformSupported(platform);
        Tag dataVersionTag = ((CompoundTag) tag).getTag(TAG_DATA_VERSION);
        if ((dataVersionTag == null) || ((IntTag) dataVersionTag).getValue() <= DATA_VERSION_MC_1_12_2) {
            return new MC12AnvilChunk((CompoundTag) tag, maxHeight, readOnly);
        } else {
            return new MC115AnvilChunk((CompoundTag) tag, maxHeight, readOnly);
        }
    }

    @Override
    public File[] getRegionFiles(Platform platform, File regionDir) {
        ensurePlatformSupported(platform);
        return regionDir.listFiles((dir, name) -> REGION_FILE_PATTERN.matcher(name).matches());
    }

    @Override
    protected File getRegionFileFile(Platform platform, File regionDir, Point coords) {
        ensurePlatformSupported(platform);
        return new File(regionDir, "r." + coords.x + "." + coords.y + ".mca");
    }

    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
}