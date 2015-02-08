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
    public InstallCustomTerrainOp(MixedMaterial terrain) throws ScriptException {
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
        if (world == null) {
            throw new ScriptException("world not set");
        }
        if (index > 0) {
            world.setMixedMaterial(index - 1, terrain);
            return (index >= 6) ? Terrain.CUSTOM_6.ordinal() - 6 + index : Terrain.CUSTOM_1.ordinal() + index - 1;
        } else {
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                if (world.getMixedMaterial(i) == null) {
                    world.setMixedMaterial(index, terrain);
                    return ((i >= 5) ? Terrain.CUSTOM_6.ordinal() + i - 5 : Terrain.CUSTOM_1.ordinal() + i) + 1;
                }
            }
            throw new ScriptException("No free custom terrain slots");
        }
    }
    
    private final MixedMaterial terrain;
    private World2 world;
    private int index = 0;
}