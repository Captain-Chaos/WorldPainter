/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public enum Generator {
    DEFAULT("Default"), FLAT("Superflat"), LARGE_BIOMES("Large Biomes"), CUSTOM("Custom");

    Generator(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private final String displayName;
}