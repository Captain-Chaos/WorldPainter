/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;
import org.pepsoft.worldpainter.MixedMaterial;

/**
 *
 * @author SchmitzP
 */
public class GetTerrainOp extends AbstractOperation<MixedMaterial> {
    public GetTerrainOp fromFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public MixedMaterial go() throws ScriptException {
        File file = sanityCheckFileName(fileName);
        try {
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
            try {
                return (MixedMaterial) in.readObject();
            } finally {
                in.close();
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