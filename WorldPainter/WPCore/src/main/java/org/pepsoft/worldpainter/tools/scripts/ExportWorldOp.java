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
package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;

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
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        if (minecraftDir != null) {
            File savesDir = new File(minecraftDir, "saves");
            if (savesDir.isDirectory() && savesDir.canWrite()) {
                directory = savesDir.getAbsolutePath();
            }
        }
    }

    public ExportWorldOp toDirectory(String directory) {
        this.directory = directory;
        return this;
    }
    
    @Override
    public Void go() throws ScriptException {
        goCalled();

        // Set the file format if it was not set yet (because this world was
        // not exported before)
        if (world.getPlatform() == null) {
            world.setPlatform((world.getMaxHeight() == DEFAULT_MAX_HEIGHT_2) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_MCREGION);
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
        JavaWorldExporter exporter = new JavaWorldExporter(world);
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