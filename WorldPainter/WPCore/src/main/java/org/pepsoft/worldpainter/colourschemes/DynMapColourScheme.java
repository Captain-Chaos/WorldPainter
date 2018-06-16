/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.colourschemes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.jetbrains.annotations.NonNls;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

/**
 * An implementation of {@link ColourScheme} which can read
 * <a href="https://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/1286593-dynmap-dynamic-web-based-maps-for-minecraft">Dynmap</a>
 * "classic" colour scheme files, such as those included with Dynmap.
 *
 * @author pepijn
 */
public final class DynMapColourScheme implements ColourScheme {
    /**
     * Create a new Dynmap colour scheme, reading the Dynamp "classic" colour scheme-formatted colour map from an input
     * stream.
     *
     * @param in The input stream from which to read the colours.
     * @param bright Whether to use the brightest colours. When <code>false</code> the two block side colours are
     *               averaged and used instead. The brightest colours usually most approximate the colours as
     *               experienced in Minecraft, so when in doubt, use <code>true</code>.
     */
    public DynMapColourScheme(InputStream in, boolean bright) { 
        loadColours(in, bright);
    }

    /**
     * Create a new Dynmap colour scheme, reading the Dynamp "classic" colour scheme-formatted colour map from a file
     * on the classpath. The file should be named <code><em>name</em>.txt</code> and be in the
     * <code>org.pepsoft.worldpainter.colourschemes</code> package.
     *
     * @param name The name of the Dynmap "classic" colour map file to read from the classpath, without
     *             <code>.txt</code> extension.
     * @param bright Whether to use the brightest colours. When <code>false</code> the two block side colours are
     *               averaged and used instead. The brightest colours usually most approximate the colours as
     *               experienced in Minecraft, so when in doubt, use <code>true</code>.
     */
    public DynMapColourScheme(@NonNls String name, boolean bright) {
        loadColours(DynMapColourScheme.class.getResourceAsStream(name + ".txt"), bright);
    }
    
    @Override
    public int getColour(int blockType) {
        // TODO: migrate this information to Material
        return (blockType >= 0) && (blockType < 256) ? COLOURS[blockType] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }

    @Override
    public int getColour(int blockType, int dataValue) {
        // TODO: migrate this information to Material
        return (blockType >= 0) && (blockType < 256) ? COLOURS[blockType + dataValue * 256] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }

    @Override
    public int getColour(Material material) {
        // TODO: migrate this information to Material
        final int blockType = material.blockType;
        return (blockType >= 0) && (blockType < 256) ? COLOURS[blockType + material.data * 256] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }
    
    private void loadColours(InputStream in, boolean bright) {
        // Make all unknown blocks magenta instead of black
        Arrays.fill(COLOURS, UNKNOWN_BLOCK_TYPE_COLOUR);
        
        try {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip comments, empty lines, and non-block type colours
                    if ((line.length() == 0) || line.startsWith("#") || line.startsWith("[")) {
                        continue;
                    }
                    
                    // Skip lines without enough tokens
                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 17) {
                        continue;
                    }
                    
                    // Decode block ID and data value (if any)
                    int p = tokens[0].indexOf(':');
                    int blockID, dataValue;
                    if (p != -1) {
                        // Data value-specific colour
                        blockID = Integer.parseInt(tokens[0].substring(0, p));
                        dataValue = Integer.parseInt(tokens[0].substring(p + 1));
                    } else {
                        // Non-data value-specific colour
                        blockID = Integer.parseInt(tokens[0]);
                        dataValue = -1;
                    }
                    
                    // Decode colour
                    int[] colourComponents = new int[16];
                    for (int i = 0; i < 16; i++) {
                        colourComponents[i] = Integer.parseInt(tokens[i + 1]);
                    }
                    //  R  G  B  A
                    //  0  1  2  3 0
                    //  4  5  6  7 3
                    //  8  9 10 11 1
                    // 12 13 14 15 2
                    int red, green, blue;
                    if (bright) {
                        red   = colourComponents[0];
                        green = colourComponents[1];
                        blue  = colourComponents[2];
                    } else {
                        red   = (colourComponents[ 8] + colourComponents[4]) / 2;
                        green = (colourComponents[ 9] + colourComponents[5]) / 2;
                        blue  = (colourComponents[10] + colourComponents[6]) / 2;
                    }

                    int colour = (red << 16) | (green << 8) | blue;
                    
                    // Store the colour
                    if (dataValue == -1) {
                        for (int i = 0; i < 16; i++) {
                            COLOURS[blockID + i * 256] = colour;
                        }
                    } else {
                        COLOURS[blockID + dataValue * 256] = colour;
                    }
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading colour scheme", e);
        }
    }
    
    private final int COLOURS[] = new int[256 * 16];
    
    private static final int UNKNOWN_BLOCK_TYPE_COLOUR = 0xff00ff; // Magenta
}