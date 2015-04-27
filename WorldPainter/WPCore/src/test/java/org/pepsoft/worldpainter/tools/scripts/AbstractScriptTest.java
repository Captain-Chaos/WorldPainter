/*
 * Copyright (C) 2014 pepijn
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Ignore;
import org.pepsoft.worldpainter.tools.ScriptingTool;

/**
 *
 * @author pepijn
 */
@Ignore
abstract class AbstractScriptTest {
    void testScript(String name, Object... args) throws IOException, ClassNotFoundException, javax.script.ScriptException {
        URL scriptURL = ClassLoader.getSystemResource(name);
        File scriptFile = new File(scriptURL.getPath());
        File scriptDir = scriptFile.getParentFile();
//        System.setProperty("user.dir", scriptDir.getAbsolutePath());
        String[] mainArgs = new String[args.length + 1];
        mainArgs[0] = scriptFile.getAbsolutePath();
        for (int i = 0; i < args.length; i++) {
            if ((args[i] instanceof File) && (! ((File) args[i]).isAbsolute())) {
                mainArgs[i + 1] = new File(scriptDir, args[i].toString()).getAbsolutePath();
            } else {
                mainArgs[i + 1] = args[i].toString();
            }
        }
        ScriptingTool.main(mainArgs);
    }
}