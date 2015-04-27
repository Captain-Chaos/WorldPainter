/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class JungleRenderer extends ColouredPatternRenderer {
    public JungleRenderer() {
        super(0x003000, new boolean[][] {
            {true,   true,  true,  true,  true, false, false, false},
            {true,   true,  true,  true,  true, false, false,  true},
            {true,   true,  true,  true,  true, false, false,  true},
            {false, false,  true,  true,  true, false, false, false},
            {false, false,  true,  true,  true,  true,  true, false},
            {false, false,  true,  true,  true,  true,  true,  true},
            {false, false,  true,  true,  true,  true,  true,  true},
            {false, false,  true,  true,  true, false, false, false},
        });
    }
}