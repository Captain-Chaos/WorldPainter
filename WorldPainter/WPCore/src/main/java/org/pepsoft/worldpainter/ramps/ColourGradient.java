/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.pepsoft.worldpainter.ramps;

import java.io.Serializable;

import static java.lang.Math.PI;
import static org.pepsoft.worldpainter.ramps.ColourGradient.Transition.COSINE;

/**
 *
 * @author pepijn
 */
public class ColourGradient implements ColourRamp, Serializable {
    public ColourGradient(int colour1, int colour2) {
        this(0.0f, colour1, 1.0f, colour2, COSINE);
    }

    public ColourGradient(float n1, int colour1, float n2, int colour2) {
        this(n1, colour1, n2, colour2, COSINE);
    }

    public ColourGradient(float n1, int colour1, float n2, int colour2, Transition transition) {
        if (n1 >= n2) {
            throw new IllegalArgumentException("n1 (" + n1 + ") >= n2 (" + n2 + ")");
        }
        this.n1 = n1;
        this.colour1 = colour1;
        this.n2 = n2;
        this.colour2 = colour2;
        this.transition = transition;
        red1   = (colour1 & 0xff0000) >> 16;
        green1 = (colour1 & 0x00ff00) >>  8;
        blue1  =  colour1 & 0x0000ff;
        red2   = (colour2 & 0xff0000) >> 16;
        green2 = (colour2 & 0x00ff00) >>  8;
        blue2  =  colour2 & 0x0000ff;
        δn = n2 - n1;
    }

    @Override
    public int getColour(float n) {
        if (n < n1) {
            return colour1;
        } else if (n <= n2) {
            final float α;
            switch (transition) {
                case LINEAR:
                    α = (n - n1) / δn;
                    break;
                case COSINE:
                    α = (float) (Math.cos((n - n1) / δn * PI + PI) / 2 + 0.5);
                    break;
                default:
                    throw new InternalError();
            }
            return    (((int) (((1 - α) * red1)   + (α * red2)))   << 16)
                    | (((int) (((1 - α) * green1) + (α * green2))) <<  8)
                    |  ((int) (((1 - α) * blue1)  + (α * blue2)));
        } else {
            return colour2;
        }
    }
    
    private final float n1, n2, δn;
    private final int colour1, red1, green1, blue1, colour2, red2, green2, blue2;
    private final Transition transition;

    public enum Transition { LINEAR, COSINE }
}