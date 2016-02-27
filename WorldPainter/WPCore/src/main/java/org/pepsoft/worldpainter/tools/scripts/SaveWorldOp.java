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

import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldIO;
import org.pepsoft.worldpainter.util.BackupUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author SchmitzP
 */
public class SaveWorldOp extends AbstractOperation<Void> {
    public SaveWorldOp(ScriptingContext context, World2 world) throws ScriptException {
        super(context);
        if (world == null) {
            throw new ScriptException("world may not be null");
        }
        this.world = world;
    }
    
    public SaveWorldOp toFile(String fileName) {
        this.fileName = fileName;
        return this;
    }
    
    @Override
    public Void go() throws ScriptException {
        goCalled();

        // TODO: add .world if it is not there
        File file = new File(fileName);
        File dir = file.getAbsoluteFile().getParentFile();
        if (! dir.isDirectory()) {
            throw new ScriptException("Destination directory " + dir + " does not exist or is not a directory");
        }
        if (! dir.canWrite()) {
            throw new ScriptException("Access denied to destination directory " + dir);
        }
        
        try {
            Configuration config = Configuration.getInstance();
            if ((config.getWorldFileBackups() > 0) && file.isFile()) {
                for (int i = config.getWorldFileBackups(); i > 0; i--) {
                    File nextBackupFile = (i > 1) ? BackupUtil.getBackupFile(file, i - 1) : file;
                    if (nextBackupFile.isFile()) {
                        File backupFile = BackupUtil.getBackupFile(file, i);
                        if (backupFile.isFile()) {
                            if (! backupFile.delete()) {
                                throw new ScriptException("Could not delete old backup file " + backupFile);
                            }
                        }
                        if (! nextBackupFile.renameTo(backupFile)) {
                            throw new ScriptException("Could not move " + nextBackupFile + " to " + backupFile);
                        }
                    }
                }
            }

            WorldIO worldIO = new WorldIO(world);
            worldIO.save(new FileOutputStream(file));
        } catch (IOException e) {
            throw new ScriptException("I/O error saving file (message: " + e.getMessage() + ")", e);
        }
        
        return null;
    }
    
    private final World2 world;
    private String fileName;
}