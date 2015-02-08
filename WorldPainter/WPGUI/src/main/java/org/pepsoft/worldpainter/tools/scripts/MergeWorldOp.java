/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;

/**
 *
 * @author SchmitzP
 */
public class MergeWorldOp extends AbstractOperation<Void> {
    // TODO
    
    @Override
    public Void go() throws ScriptException {
        // Set the file format if it was not set yet (because this world was
        // not exported before)
        if (world.getVersion() == 0) {
            world.setVersion((world.getMaxHeight() == DEFAULT_MAX_HEIGHT_2) ? SUPPORTED_VERSION_2 : SUPPORTED_VERSION_1);
        }

        // Load any custom materials defined in the world
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }
        
        return null;
    }
    
    private World2 world;
    private String levelDatFile;
    private MergeType type;

    public enum MergeType {MERGE_CHUNKS, BIOMES_ONLY, REPLACE_CHUNKS}
}