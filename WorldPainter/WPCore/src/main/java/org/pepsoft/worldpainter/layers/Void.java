/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Void extends Layer {
    private Void() {
        super("Void", "Just the long drop into nothingness", DataSize.BIT, false, 0);
    }
    
    public static final Void INSTANCE = new Void();
    
    private static final long serialVersionUID = 2011100801L;
}