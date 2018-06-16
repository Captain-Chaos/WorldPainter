/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author SchmitzP
 */
public class MergeWorldOp extends AbstractOperation<Void> {
    protected MergeWorldOp(ScriptingContext context) {
        super(context);
    }

    // TODO
    
    @Override
    public Void go() throws ScriptException {
        goCalled();

        // Set the file format if it was not set yet (because this world was
        // not exported before)
        if (world.getPlatform() == null) {
            world.setPlatform((world.getMaxHeight() == DEFAULT_MAX_HEIGHT_ANVIL) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_MCREGION);
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