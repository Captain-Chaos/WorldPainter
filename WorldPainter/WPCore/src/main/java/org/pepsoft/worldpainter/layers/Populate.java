/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class Populate extends Layer {
    public Populate() {
        super("Populate", "Let Minecraft populate the land with vegetation, snow, resources (coal, ores, etc.) and water and lava pools", Layer.DataSize.BIT_PER_CHUNK, false, 0);
    }

    public static final Populate INSTANCE = new Populate();

    private static final long serialVersionUID = 2011040701L;
}