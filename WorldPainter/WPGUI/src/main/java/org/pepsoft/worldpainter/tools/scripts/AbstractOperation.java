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

import java.io.File;

/**
 *
 * @author SchmitzP
 */
public abstract class AbstractOperation<T> implements Operation<T> {
    protected AbstractOperation(ScriptingContext context) {
        this.context = context;
    }

    protected final void goCalled() {
        context.goCalled();
    }

    protected final File sanityCheckFileName(String fileName) throws ScriptException {
        if ((fileName == null) || (fileName.isEmpty())) {
            throw new ScriptException("Source file not specified");
        }
        File file = new File(fileName);
        if (! file.exists()) {
            throw new ScriptException("File " + fileName + " not found");
        }
        if (! file.isFile()) {
            throw new ScriptException("File " + fileName + " is not a file");
        }
        if (! file.canRead()) {
            throw new ScriptException("Access denied to file " + fileName);
        }
        return file;
    }

    private final ScriptingContext context;
}