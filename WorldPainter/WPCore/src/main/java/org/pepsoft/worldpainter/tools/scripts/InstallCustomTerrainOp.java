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

import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;

/**
 * An operation which installs a custom terrain on a world.
 * 
 * @author SchmitzP
 */
public class InstallCustomTerrainOp extends AbstractOperation<Integer> {
    public InstallCustomTerrainOp(ScriptingContext context, MixedMaterial terrain) throws ScriptException {
        super(context);
        if (terrain == null) {
            throw new ScriptException("terrain may not be null");
        }
        this.terrain = terrain;
    }
    
    public InstallCustomTerrainOp toWorld(World2 world) {
        this.world = world;
        return this;
    }
    
    public InstallCustomTerrainOp inSlot(int index) {
        this.index = index;
        return this;
    }
    
    @Override
    public Integer go() throws ScriptException {
        goCalled();

        if (world == null) {
            throw new ScriptException("world not set");
        }
        if (index > 0) {
            world.setMixedMaterial(index - 1, terrain);
            return Terrain.getCustomTerrain(index - 1).ordinal();
        } else {
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                if (world.getMixedMaterial(i) == null) {
                    world.setMixedMaterial(i, terrain);
                    return Terrain.getCustomTerrain(i).ordinal();
                }
            }
            throw new ScriptException("No free custom terrain slots");
        }
    }
    
    private final MixedMaterial terrain;
    private World2 world;
    private int index = 0;
}