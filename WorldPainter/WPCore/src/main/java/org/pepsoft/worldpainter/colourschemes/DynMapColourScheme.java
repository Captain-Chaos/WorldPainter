/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.colourschemes;

import org.jetbrains.annotations.NonNls;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.UNKNOWN_MATERIAL_COLOUR;

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
     * @param bright Whether to use the brightest colours. When {@code false} the two block side colours are
     *               averaged and used instead. The brightest colours usually most approximate the colours as
     *               experienced in Minecraft, so when in doubt, use {@code true}.
     */
    public DynMapColourScheme(InputStream in, boolean bright) { 
        loadColours(in, bright);
    }

    /**
     * Create a new Dynmap colour scheme, reading the Dynamp "classic" colour scheme-formatted colour map from a file
     * on the classpath. The file should be named {@code <em>name</em>.txt} and be in the
     * {@code org.pepsoft.worldpainter.colourschemes} package.
     *
     * @param name The name of the Dynmap "classic" colour map file to read from the classpath, without
     *             {@code .txt} extension.
     * @param bright Whether to use the brightest colours. When {@code false} the two block side colours are
     *               averaged and used instead. The brightest colours usually most approximate the colours as
     *               experienced in Minecraft, so when in doubt, use {@code true}.
     */
    public DynMapColourScheme(@NonNls String name, boolean bright) {
        loadColours(DynMapColourScheme.class.getResourceAsStream(name + ".txt"), bright);
    }

    @Override
    public int getColour(Material material) {
        return COLOURS_BY_IDENTITY.getOrDefault(material, UNKNOWN_MATERIAL_COLOUR);
    }
    
    private void loadColours(InputStream in, boolean bright) {
        // Make all unknown blocks magenta instead of black
        Arrays.fill(COLOURS_BY_INDEX, UNKNOWN_MATERIAL_COLOUR);
        final int[] colourComponents = new int[16];

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments, empty lines, and non-block type colours
                if ((line.length() == 0) || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }

                // Skip lines without enough tokens
                final String[] tokens = line.split("\\s+");
                if (tokens.length < 17) {
                    continue;
                }

                // Get material name and properties
                final int p = tokens[0].indexOf('[');
                final String name;
                final Map<String, String> properties;
                if (p != -1) {
                    name = tokens[0].substring(0, p);
                    properties = stream(tokens[0].substring(p + 1, tokens[0].length() - 1).split(","))
                            .collect(Collectors.toMap(property -> property.substring(0, property.indexOf('=')),
                                    property -> property.substring(property.indexOf('=') + 1)));
                } else {
                    name = tokens[0];
                    properties = null;
                }
                if (name.equals(MC_WATER)) {
                    System.out.println("We're there!");
                }

                // Decode colour
                for (int i = 0; i < 16; i++) {
                    colourComponents[i] = Integer.parseInt(tokens[i + 1]);
                }
                //  R  G  B  A
                //  0  1  2  3 0
                //  4  5  6  7 3
                //  8  9 10 11 1
                // 12 13 14 15 2
                final int red, green, blue;
                if (bright) {
                    red   = colourComponents[0];
                    green = colourComponents[1];
                    blue  = colourComponents[2];
                } else {
                    red   = (colourComponents[ 8] + colourComponents[4]) / 2;
                    green = (colourComponents[ 9] + colourComponents[5]) / 2;
                    blue  = (colourComponents[10] + colourComponents[6]) / 2;
                }
                if ((red == green) && (red == blue)) {
                    System.out.println(name + " is grey");
                }

                final int colour;
                if (name.equals(MC_WATER)) {
                    // Hardcode the colour of water because it is gray in the DynMap colour schemes
                    colour = blue * 0xff / 256;
                } else {
                    colour = (red << 16) | (green << 8) | blue;
                }

                // Store the colour
                final Material material = (properties != null) ? Material.get(name, properties) : Material.getPrototype(name);
                COLOURS_BY_IDENTITY.put(material, colour);
                if ((material.blockType >= 0) && (material.blockType < 256)) {
                    if ((properties != null) || (material.data > 0)) {
                        COLOURS_BY_INDEX[material.blockType + material.data * 256] = colour;
                    } else {
                        for (int data = 0; data < 16; data++) {
                            COLOURS_BY_INDEX[material.blockType + data * 256] = colour;
                        }
                    }
                }
            }

            // Patch water and lava
            // TODO anything else need patching?
            for (int data = 0; data < 16; data++) {
                COLOURS_BY_INDEX[BLK_WATER + 256 * data] = COLOURS_BY_INDEX[BLK_STATIONARY_WATER + 256 * data];
                COLOURS_BY_INDEX[BLK_LAVA + 256 * data] = COLOURS_BY_INDEX[BLK_STATIONARY_LAVA + 256 * data];
            }
            COLOURS_BY_IDENTITY.put(WATER, COLOURS_BY_INDEX[BLK_WATER + 256]);
            COLOURS_BY_IDENTITY.put(STATIONARY_WATER, COLOURS_BY_INDEX[BLK_STATIONARY_WATER + 256]);
            COLOURS_BY_IDENTITY.put(LAVA, COLOURS_BY_INDEX[BLK_LAVA + 256]);
            COLOURS_BY_IDENTITY.put(STATIONARY_LAVA, COLOURS_BY_INDEX[BLK_STATIONARY_LAVA + 256]);
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading colour scheme", e);
        }
    }
    
    private final int COLOURS_BY_INDEX[] = new int[256 * 16];
    private final Map<Material, Integer> COLOURS_BY_IDENTITY = new HashMap<>();

    private static final int DEFAULT_WATER_COLOUR = 0x0000ff;
}