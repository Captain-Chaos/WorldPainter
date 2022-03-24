package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.MinecraftCoords;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.layers.ReadOnly;

import java.io.File;
import java.util.Set;

/**
 * A provider of {@link MapImporter map importing functionality} for a particular WorldPainter {@link Platform platform}.
 */
public interface MapImporterProvider {
    /**
     * Get a {@link MapImporter} configured for the specified map and settings.
     *
     * @param dir                The directory of the map to import.
     * @param tileFactory        The {@link TileFactory} to use, optionally, for creating the tiles into which the map will be imported.
     * @param chunksToSkip       A set of coordinates of chunks to skip. The importer should act as if these chunks do not exist in the map.
     * @param readOnlyOption     Whether and how to apply the {@link ReadOnly} layer to the imported world.
     * @param dimensionsToImport The set of dimensions (specified as a set of {@link Constants#DIM_NORMAL}, {@link Constants#DIM_NETHER} and/or {@link Constants#DIM_END}) to import.
     * @return A map importer configured as specified.
     */
    MapImporter getImporter(File dir, TileFactory tileFactory, Set<MinecraftCoords> chunksToSkip, MapImporter.ReadOnlyOption readOnlyOption, Set<Integer> dimensionsToImport);
}