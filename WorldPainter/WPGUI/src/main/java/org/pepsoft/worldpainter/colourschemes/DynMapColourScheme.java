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
 *
 * @author pepijn
 */
public final class DynMapColourScheme implements ColourScheme {
    public DynMapColourScheme(InputStream in, boolean bright) { 
        loadColours(in, bright);
    }

    public DynMapColourScheme(@NonNls String name, boolean bright) {
        loadColours(DynMapColourScheme.class.getResourceAsStream(name + ".txt"), bright);
    }
    
    @Override
    public int getColour(int blockType) {
        return (blockType < 256) ? COLOURS[blockType] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }

    @Override
    public int getColour(int blockType, int dataValue) {
        return (blockType < 256) ? COLOURS[blockType + dataValue * 256] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }

    @Override
    public int getColour(Material material) {
        final int blockType = material.getBlockType();
        return (blockType < 256) ? COLOURS[blockType + material.getData() * 256] : UNKNOWN_BLOCK_TYPE_COLOUR;
    }
    
    private void loadColours(InputStream in, boolean bright) {
        // Make all unknown blocks cyan instead of black
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
    
    private static final int UNKNOWN_BLOCK_TYPE_COLOUR = 0x00ffff; // Cyan
}