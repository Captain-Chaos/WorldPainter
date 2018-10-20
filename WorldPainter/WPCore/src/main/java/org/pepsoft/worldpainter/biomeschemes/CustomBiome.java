/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.io.Serializable;

import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes.FIRST_UNALLOCATED_ID;

/**
 *
 * @author pepijn
 */
public class CustomBiome implements Serializable {
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
    
    private String name;
    private int id, colour;

    private static final long serialVersionUID = 1L;
}