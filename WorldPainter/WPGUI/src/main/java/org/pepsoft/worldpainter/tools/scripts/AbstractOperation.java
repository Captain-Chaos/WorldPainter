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
}