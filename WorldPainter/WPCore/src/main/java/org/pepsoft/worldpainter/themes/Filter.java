/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.io.Serializable;

/**
 *
 * @author SchmitzP
 */
public interface Filter extends Serializable {
    /**
     * Determine the level at which some operation should be applied for some
     * specific location in the world.
     * 
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param inputLevel The level (0-15) to start with.
     * @return The level (0-15) at which the operation should be applied.
     */
    int getLevel(int x, int y, int z, int inputLevel);
}