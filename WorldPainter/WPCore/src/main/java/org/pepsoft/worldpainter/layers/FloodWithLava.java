/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 *
 * @author pepijn
 */
public class FloodWithLava extends Layer {
    private FloodWithLava() {
        super("Lava", "Flood with lava instead of water", DataSize.BIT, 0);
    }

    public static final FloodWithLava INSTANCE = new FloodWithLava();

    private static final long serialVersionUID = 2011033001L;
}