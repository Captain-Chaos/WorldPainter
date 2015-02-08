/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_9BiomeScheme;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;

/**
 *
 * @author pepijn
 */
public class DumpBiomes {
    public static void main(String[] args) throws IOException {
        BufferedImage image = new BufferedImage(1001, 1001, BufferedImage.TYPE_INT_RGB);
        BiomeScheme biomeScheme = new Minecraft1_9BiomeScheme(new File("/home/pepijn/.minecraft/bin/minecraft.jar"), null);
        biomeScheme.setSeed(1L);
        int[] biomes = biomeScheme.getBiomes(-500, -500, 1001, 1001);
        ColourScheme colourScheme = new DynMapColourScheme("default", true);
        int[] colours = new int[] {
            colourScheme.getColour(BLK_WATER),
            colourScheme.getColour(BLK_GRASS),
            colourScheme.getColour(BLK_SAND),
            colourScheme.getColour(BLK_GRASS),
            colourScheme.getColour(BLK_LEAVES),
            colourScheme.getColour(BLK_LEAVES),
            colourScheme.getColour(BLK_LEAVES),
            colourScheme.getColour(BLK_WATER),
            0,
            0,
            colourScheme.getColour(BLK_ICE),
            colourScheme.getColour(BLK_ICE),
            colourScheme.getColour(BLK_SNOW),
            colourScheme.getColour(BLK_SNOW),
            colourScheme.getColour(BLK_MYCELIUM),
            colourScheme.getColour(BLK_MYCELIUM)
        };
        for (int x = 0; x <= 1000; x++) {
            for (int y = 0; y <= 1000; y++) {
                image.setRGB(x, y, colours[biomes[x + y * 1001]]);
            }
        }
        ImageIO.write(image, "png", new File("biomes.png"));
    }
}
