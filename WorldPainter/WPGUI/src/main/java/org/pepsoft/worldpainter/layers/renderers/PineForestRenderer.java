/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class PineForestRenderer extends ColouredPatternRenderer {
    public PineForestRenderer() {
        super(0x002000, new boolean[][] {
            {false, false, false, true,  false, false, false, false},
            {false, false, true,  true,  true,  false, false, false},
            {false, false, true,  true,  true,  false, false, false},
            {false, true,  true,  true,  true,  true,  false, false},
            {false, true,  true,  true,  true,  true,  false, false},
            {true,  true,  true,  true,  true,  true,  true,  false},
            {false, false, false, true,  false, false, false, false},
            {false, false, false, false, false, false, false, false},
        }, 4, 5);
    }
}