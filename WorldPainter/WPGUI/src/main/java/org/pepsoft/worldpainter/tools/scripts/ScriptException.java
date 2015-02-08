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