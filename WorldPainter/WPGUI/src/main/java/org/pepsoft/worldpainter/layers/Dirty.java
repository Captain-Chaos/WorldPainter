/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Dirty extends Layer {
    private Dirty() {
        super("Dirty", "Tracks chunks that have been changed since importing the map", Layer.DataSize.BIT_PER_CHUNK, 0);
    }
    
    public static final Dirty INSTANCE = new Dirty();
    
    private static final long serialVersionUID = 2011071701L;
}