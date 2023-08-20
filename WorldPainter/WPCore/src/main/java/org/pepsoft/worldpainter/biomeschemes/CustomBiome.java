/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.pepsoft.util.ImageUtils.fromBytes;
import static org.pepsoft.util.ImageUtils.toBytes;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.FIRST_UNALLOCATED_ID;

/**
 *
 * @author pepijn
 */
public class CustomBiome implements Serializable {
    public CustomBiome(String name, int id) {
        this(name, id, pickColour(id));
    }

    public CustomBiome(String name, int id, int colour) {
        setName(name);
        setId(id);
        setColour(colour);
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        if (id < FIRST_UNALLOCATED_ID) {
            throw new IllegalArgumentException();
        }
        this.id = id;
    }

    public final int getColour() {
        return colour;
    }

    public final void setColour(int colour) {
        this.colour = colour;
    }

    public final BufferedImage getPattern() {
        return pattern;
    }

    public final void setPattern(BufferedImage pattern) {
        this.pattern = pattern;
    }

    public static int pickColour(int biomeId) {
        return ((((biomeId & 0x02) == 0x02)
                ? (((biomeId & 0x01) == 0x01) ? 255 : 192)
                : (((biomeId & 0x01) == 0x01) ? 128 :   0)) << 16)
            | ((((biomeId & 0x08) == 0x08)
                ? (((biomeId & 0x04) == 0x04) ? 255 : 192)
                : (((biomeId & 0x04) == 0x04) ? 128 :   0)) << 8)
            | (((biomeId & 0x20) == 0x20)
                ? (((biomeId & 0x10) == 0x10) ? 255 : 192)
                : (((biomeId & 0x10) == 0x10) ? 128 :   0));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        storedPattern = (pattern != null) ? toBytes(pattern) : null;
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (storedPattern != null) {
            pattern = fromBytes(storedPattern);
        }
    }

    private String name;
    private int id, colour;
    private byte[] storedPattern;
    private transient BufferedImage pattern;

    private static final long serialVersionUID = 1L;
}