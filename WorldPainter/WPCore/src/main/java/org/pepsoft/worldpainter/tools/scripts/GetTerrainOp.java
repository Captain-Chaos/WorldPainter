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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author SchmitzP
 */
public class GetTerrainOp extends AbstractOperation<MixedMaterial> {
    protected GetTerrainOp(ScriptingContext context) {
        super(context);
    }

    public GetTerrainOp fromFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public MixedMaterial go() throws ScriptException {
        goCalled();

        File file = sanityCheckFileName(fileName);
        try {
            try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                return (MixedMaterial) in.readObject();
            }
        } catch (IOException e) {
            throw new ScriptException("I/O error while loading terrain " + fileName, e);
        } catch (ClassCastException e) {
            throw new ScriptException(fileName + " is not a WorldPainter custom terrain file", e);
        } catch (ClassNotFoundException e) {
            throw new ScriptException("Class not found exception while loading terrain " + fileName + " (not a WorldPainter custom terrain?)", e);
        }
    }
    
    private String fileName;
}