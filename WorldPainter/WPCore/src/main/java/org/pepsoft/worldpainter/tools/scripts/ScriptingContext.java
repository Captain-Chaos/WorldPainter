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

import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.Layer;

import static org.pepsoft.worldpainter.Version.VERSION;

/**
 *
 * @author SchmitzP
 */
@SuppressWarnings("unused") // Used from scripts
public class ScriptingContext {
    public ScriptingContext(boolean commandLine) {
        this.commandLine = commandLine;
    }

    /**
     * Get the WorldPainter version string.
     * 
     * @return The WorldPainter version string.
     */
    public String getVersion() {
        return VERSION;
    }
    
    /**
     * Load a WorldPainter .world file from the file system.
     * 
     * @return
     * @throws java.io.IOException
     * @throws ClassNotFoundException 
     */
    public GetWorldOp getWorld() {
        checkGoCalled("getWorld");
        return new GetWorldOp(this);
    }
    
    /**
     * Get a WorldPainter layer from a file, an existing world, or from the
     * standard WorldPainter layers.
     * 
     * @return
     */
    public GetLayerOp getLayer() {
        checkGoCalled("getLayer");
        return new GetLayerOp(this);
    }

    /**
     * Load a WorldPainter .terrain file from the file system.
     * 
     * @return
     * @throws java.io.IOException
     * @throws ClassNotFoundException 
     */
    public GetTerrainOp getTerrain() {
        checkGoCalled("getTerrain");
        return new GetTerrainOp(this);
    }
    
    /**
     * Import a height map as a new WorldPainter world.
     * 
     * @return
     */
    public ImportHeightMapOp createWorld() {
        checkGoCalled("createWorld");
        return new ImportHeightMapOp(this);
    }
    
    /**
     * Export a WorldPainter world as a new Minecraft map. If there is already a
     * map with the same name in the specified directory, it is moved to the
     * backups directory.
     * 
     * @param world The world to export.
     * @throws java.io.IOException
     */
    public ExportWorldOp exportWorld(World2 world) throws ScriptException {
        checkGoCalled("exportWorld");
        return new ExportWorldOp(this, world);
    }
    
    // TODO
    public MergeWorldOp mergeWorld() {
        checkGoCalled("mergeWorld");
        return new MergeWorldOp(this);
    }
    
    /**
     * Save a world to disk.
     * 
     * @param world The world to save.
     * @return
     * @throws org.pepsoft.worldpainter.tools.scripts.ScriptException
     */
    public SaveWorldOp saveWorld(World2 world) throws ScriptException {
        checkGoCalled("saveWorld");
        return new SaveWorldOp(this, world);
    }
    
    public GetHeightMapOp getHeightMap() {
        checkGoCalled("getHeightMap");
        return new GetHeightMapOp(this);
    }
    
    /**
     * Map a height map to a layer or to the terrain type, optionally using a
     * filter or other conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyHeightMap(HeightMap heightMap) throws ScriptException {
        checkGoCalled("applyHeightMap");
        return new MappingOp(this, heightMap);
    }
    
    /**
     * Apply a layer to the world, optionally using a filter or other
     * conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyLayer(Layer layer) throws ScriptException {
        checkGoCalled("applyLayer");
        return new MappingOp(this, layer);
    }
    
    /**
     * Apply a layer to the world, optionally using a filter or other
     * conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyTerrain(int terrainIndex) throws ScriptException {
        checkGoCalled("applyTerrain");
        return new MappingOp(this, terrainIndex);
    }
    
    /**
     * Install a custom terrain in a world, returning the terrain index to use
     * for applying the terrain.
     * 
     * @param terrain The terrain to install.
     * @return 
     */
    public InstallCustomTerrainOp installCustomTerrain(MixedMaterial terrain) throws ScriptException {
        checkGoCalled("installCustomTerrain");
        return new InstallCustomTerrainOp(this, terrain);
    }
    
    public CreateFilterOp createFilter() {
        checkGoCalled("createFilter");
        return new CreateFilterOp(this);
    }

    public void checkGoCalled(String commandName) {
        if (! goCalled) {
            throw new IllegalStateException("You forgot to invoke go() on the " + lastCommandName + "() operation");
        }
        goCalled = false;
        lastCommandName = commandName;
    }

    void goCalled() {
        goCalled = true;
    }

    public final boolean commandLine;

    private boolean goCalled = true;
    private String lastCommandName;
}