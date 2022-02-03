package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.MinecraftCoords;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.importing.MapImporter;

import java.io.File;
import java.util.Set;

public interface MapImporterProvider {
    MapImporter getImporter(File dir, TileFactory tileFactory, Set<MinecraftCoords> chunksToSkip, MapImporter.ReadOnlyOption readOnlyOption, Set<Integer> dimensionsToImport);
}