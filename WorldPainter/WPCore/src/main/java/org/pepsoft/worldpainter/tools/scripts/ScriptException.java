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

/**
 * A checked exception, thrown if an exception occurs during the execution of a
 * script.
 * 
 * @author SchmitzP
 */
public class ScriptException extends Exception {
    public ScriptException() {
        // Do nothing
    }

    public ScriptException(String string) {
        super(string);
    }

    public ScriptException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public ScriptException(Throwable thrwbl) {
        super(thrwbl);
    }
}