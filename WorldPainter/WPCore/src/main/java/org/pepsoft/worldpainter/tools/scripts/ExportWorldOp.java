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

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.WorldExporter;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author SchmitzP
 */
public class ExportWorldOp extends AbstractOperation<Void> {
    public ExportWorldOp(ScriptingContext context, World2 world) throws ScriptException {
        super(context);
        if (world == null) {
            throw new ScriptException("world may not be null");
        }
        this.world = world;
    }

    public ExportWorldOp toDirectory(String directory) {
        // TODO make optional, default to Minecraft saves dir
        this.directory = directory;
        return this;
    }
    
    @Override
    public Void go() throws ScriptException {
        goCalled();

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