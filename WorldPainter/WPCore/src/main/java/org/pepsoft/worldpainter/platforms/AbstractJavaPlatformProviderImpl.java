package org.pepsoft.worldpainter.platforms;

import org.jnbt.Tag;
import org.pepsoft.minecraft.DataType;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.Set;

abstract class AbstractJavaPlatformProviderImpl {
    /**
     * Get the region storage data types required by this platform.
     */
    abstract Set<DataType> getDataTypes();

    /**
     * Create an existing chunk from map data.
     */
    abstract NBTChunk createChunk(Map<DataType, Tag> tags, int maxHeight, boolean readOnly);

    /**
     * Create a new, empty chunk.
     */
    abstract NBTChunk createChunk(int x, int z, int maxHeight);

    /**
     * Get all region files in a specific directory of the specified type(s).
     */
    abstract File[] getRegionFiles(File regionDir, DataType dataType);

    /**
     * Get an individual region file from a specific directory of a specific type.
     */
    abstract File getRegionFileFile(File regionDir, DataType dataType, Point coords);

    /**
     * Get the post-processor for this platform.
     */
    abstract PostProcessor getPostProcessor();
}