/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class DeciduousForestRenderer extends ColouredPatternRenderer {
    public DeciduousForestRenderer() {
        super(0x004000, new boolean[][] {
            {false, false, true,  true,  true,  false, false, false},
            {false, true,  true,  true,  true,  true,  false, false},
            {true,  true,  true,  true,  true,  true,  true,  false},
            {true,  true,  true,  true,  true,  true,  true,  false},
            {false, true,  true,  true,  true,  true,  false, false},
            {false, false, true,  true,  true,  false, false, false},
            {false, false, false, true,  false, false, false, false},
            {false, false, false, false, false, false, false, false},
        });
    }
}