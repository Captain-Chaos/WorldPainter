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
    DEFAULT("Default"), FLAT("Superflat"), LARGE_BIOMES("Large Biomes"), AMPLIFIED("Amplified"), BUFFET("Buffet"), CUSTOM("Custom"), CUSTOMIZED("Customized"), UNKNOWN("Unknown"), NETHER("The Nether"), END("The End");

    Generator(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private final String displayName;
}