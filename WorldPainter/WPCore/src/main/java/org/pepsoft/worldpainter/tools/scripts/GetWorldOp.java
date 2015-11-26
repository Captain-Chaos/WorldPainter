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

import org.pepsoft.worldpainter.UnloadableWorldException;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldIO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author SchmitzP
 */
public class GetWorldOp extends AbstractOperation<World2> {
    protected GetWorldOp(ScriptingContext context) {
        super(context);
    }

    public GetWorldOp fromFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public World2 go() throws ScriptException {
        goCalled();

        File file = sanityCheckFileName(fileName);
        WorldIO worldIO = new WorldIO();
        try {
            worldIO.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new ScriptException("I/O error while loading world " + fileName, e);
        } catch (UnloadableWorldException e) {
            throw new ScriptException("Unloadable world " + fileName + " (not a WorldPainter world?)", e);
        }
        return worldIO.getWorld();
    }
    
    private String fileName;
}