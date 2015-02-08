/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Chasms extends Layer {
    private Chasms() {
        super("org.pepsoft.Chasms", "Chasms", "Generate underground tunnel- or ravine-like chasms of varying size", DataSize.NIBBLE, 21);
    }

    public static final Chasms INSTANCE = new Chasms();
    
    private static final long serialVersionUID = 1L;
}