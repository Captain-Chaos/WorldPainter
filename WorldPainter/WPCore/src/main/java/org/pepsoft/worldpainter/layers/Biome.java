/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

/**
 * A legacy (numbered) biome.
 *
 * @author pepijn
 */
public class Biome extends Layer {
    private Biome() {
        super("Biome", "Displays the biome Minecraft will generate", Layer.DataSize.BYTE, 70);
    }

    @Override
    public int getDefaultValue() {
        return 255;
    }

    public static final Biome INSTANCE = new Biome();

    private static final long serialVersionUID = -5510962172433402363L;
}