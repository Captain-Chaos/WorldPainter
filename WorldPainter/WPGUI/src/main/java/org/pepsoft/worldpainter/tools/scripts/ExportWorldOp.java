/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.io.File;
import java.io.IOException;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.WorldExporter;

/**
 *
 * @author SchmitzP
 */
public class ExportWorldOp extends AbstractOperation<Void> {
    public ExportWorldOp(World2 world) throws ScriptException {
        if (world == null) {
            throw new ScriptException("world may not be null");
        }
        this.world = world;
    }

    public ExportWorldOp toDirectory(String directory) {
        this.directory = directory;
        return this;
    }
    
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
        
        // Select and create (if necessary) the backups directory
        File baseDir = new File(directory);
        if (! baseDir.isDirectory()) {
            throw new ScriptException("Directory " + directory + " does not exist or is not a directory");
        }
        File worldDir = new File(baseDir, FileUtils.sanitiseName(world.getName()));
        WorldExporter exporter = new WorldExporter(world);
        try {
            File backupDir = exporter.selectBackupDir(worldDir);
        
            // Export the world
            exporter.export(baseDir, world.getName(), backupDir, null);
        } catch (ProgressReceiver.OperationCancelled e) {
            // Can never happen since we don't pass a progress receiver in
            throw new InternalError();
        } catch (IOException e) {
            throw new ScriptException("I/O error while exporting world", e);
        }
        
        return null;
    }
    
    private final World2 world;
    private String directory = ".";
}