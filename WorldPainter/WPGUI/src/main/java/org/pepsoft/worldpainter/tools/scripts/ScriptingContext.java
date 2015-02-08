/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.io.IOException;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.MixedMaterial;
import static org.pepsoft.worldpainter.Version.*;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public class ScriptingContext {
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
     * @param filename
     * @return
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public GetWorldOp getWorld() {
        return new GetWorldOp();
    }
    
    /**
     * Get a WorldPainter layer from a file, an existing world, or from the
     * standard WorldPainter layers.
     * 
     * @return
     */
    public GetLayerOp getLayer() {
        return new GetLayerOp();
    }

    /**
     * Load a WorldPainter .terrain file from the file system.
     * 
     * @param filename
     * @return
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public GetTerrainOp getTerrain() {
        return new GetTerrainOp();
    }
    
    /**
     * Import a height map as a new WorldPainter world.
     * 
     * @param args
     * @return 
     */
    public ImportHeightMapOp createWorld() {
        return new ImportHeightMapOp();
    }
    
    /**
     * Export a WorldPainter world as a new Minecraft map. If there is already a
     * map with the same name in the specified directory, it is moved to the
     * backups directory.
     * 
     * @param world The world to export.
     * @throws IOException 
     */
    public ExportWorldOp exportWorld(World2 world) throws ScriptException {
        return new ExportWorldOp(world);
    }
    
    // TODO
    public MergeWorldOp mergeWorld() {
        return new MergeWorldOp();
    }
    
    /**
     * Save a world to disk.
     * 
     * @param world The world to save.
     * @return
     * @throws ScriptException 
     */
    public SaveWorldOp saveWorld(World2 world) throws ScriptException {
        return new SaveWorldOp(world);
    }
    
    public GetHeightMapOp getHeightMap() {
        return new GetHeightMapOp();
    }
    
    /**
     * Map a height map to a layer or to the terrain type, optionally using a
     * filter or other conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyHeightMap(HeightMap heightMap) throws ScriptException {
        return new MappingOp(heightMap);
    }
    
    /**
     * Apply a layer to the world, optionally using a filter or other
     * conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyLayer(Layer layer) throws ScriptException {
        return new MappingOp(layer);
    }
    
    /**
     * Apply a layer to the world, optionally using a filter or other
     * conditions.
     * 
     * @parm heightMap The height map to apply.
     * @return 
     */
    public MappingOp applyTerrain(int terrainIndex) throws ScriptException {
        return new MappingOp(terrainIndex);
    }
    
    /**
     * Install a custom terrain in a world, returning the terrain index to use
     * for applying the terrain.
     * 
     * @param terrain The terrain to install.
     * @return 
     */
    public InstallCustomTerrainOp installCustomTerrain(MixedMaterial terrain) throws ScriptException {
        return new InstallCustomTerrainOp(terrain);
    }
    
    public CreateFilterOp createFilter() {
        return new CreateFilterOp();
    }
}