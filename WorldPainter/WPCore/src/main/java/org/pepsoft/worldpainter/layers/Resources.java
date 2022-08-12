/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Resources extends Layer {
    private Resources() {
        super("Resources", "Underground pockets of coal, ores, gravel and dirt, lava and water, etc.", DataSize.NIBBLE, false, 10, 'r');
    }
    
    public static final Resources INSTANCE = new Resources();
    
    private static final long serialVersionUID = 1L;
}