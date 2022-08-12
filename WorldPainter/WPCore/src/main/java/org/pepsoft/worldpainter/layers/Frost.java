/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Frost extends Layer {
    private Frost() {
        super("Frost", "Cover the ground with snow and turn water to ice", DataSize.BIT, false, 60, 'o');
    }

    public static final Frost INSTANCE = new Frost();
    
    private static final long serialVersionUID = 2011032901L;
}
