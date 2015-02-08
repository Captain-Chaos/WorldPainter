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
import org.pepsoft.worldpainter.World2;

/**
 *
 * @author SchmitzP
 */
public class GetWorldOp extends AbstractOperation<World2> {
    public GetWorldOp fromFile(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public World2 go() throws ScriptException {
        File file = sanityCheckFileName(fileName);
        try {
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
            try {
                return (World2) in.readObject();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new ScriptException("I/O error while loading world " + fileName, e);
        } catch (ClassCastException e) {
            throw new ScriptException(fileName + " is not a WorldPainter world file", e);
        } catch (ClassNotFoundException e) {
            throw new ScriptException("Class not found exception while loading world " + fileName + " (not a WorldPainter world?)", e);
        }
    }
    
    private String fileName;
}