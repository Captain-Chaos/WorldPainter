package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.importing.JavaMapImporter;

/**
 * Makes sure the block database is consistent.
 *
 * Created by pepijn on 29-3-15.
 */
public class TestBlocks {
    public static void main(String[] args) {
        Constants.TILE_ENTITY_MAP.get(0);
        JavaMapImporter.TERRAIN_MAPPING.get(0);
    }
}
