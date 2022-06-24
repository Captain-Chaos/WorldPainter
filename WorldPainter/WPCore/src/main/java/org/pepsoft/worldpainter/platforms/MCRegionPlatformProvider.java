package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.MCRegionChunk;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.DataType.REGION;

final class MCRegionPlatformProvider extends AbstractJavaPlatformProviderImpl {
    @Override
    Set<DataType> getDataTypes() {
        return DATA_TYPES;
    }

    @Override
    NBTChunk createChunk(Map<DataType, Tag> tags, int maxHeight, boolean readOnly) {
        return new MCRegionChunk((CompoundTag) tags.get(REGION), maxHeight, readOnly);
    }

    @Override
    File[] getRegionFiles(File regionDir, DataType dataType) {
        if (dataType != REGION) {
            throw new IllegalArgumentException("Only REGION data type is supported");
        }
        return regionDir.listFiles((dir, name) -> REGION_FILE_PATTERN.matcher(name).matches());
    }

    @Override
    File getRegionFileFile(File regionDir, DataType dataType, Point coords) {
        if (dataType != REGION) {
            throw new IllegalArgumentException("Only REGION data type is supported");
        }
        return new File(regionDir, "r." + coords.x + "." + coords.y + ".mcr");
    }

    @Override
    PostProcessor getPostProcessor() {
        return new Java1_2PostProcessor();
    }

    @Override
    NBTChunk createChunk(int x, int z, int maxHeight) {
        return new MCRegionChunk(x, z, maxHeight);
    }

    private static final Set<DataType> DATA_TYPES = ImmutableSet.of(REGION);
    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
}
