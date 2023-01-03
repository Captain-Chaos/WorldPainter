/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.pepsoft.worldpainter.ramps;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The default colour ramp for WorldPainter, with the following ranges:
 *
 * <ul>
 *     <li>From {@code minHeight} to 0: black to blue
 *     <li>From 0 to {@code waterLevel}: blue to cyan
 *     <li>From {@code waterLevel} to 256: dark green to green to yellow to orange to red to white
 *     <li>From 256 to {@code maxHeight}: white to purple
 * </ul>
 *
 * @author pepijn
 */
public class DefaultColourRamp implements ColourRamp {
    public DefaultColourRamp(int minHeight, int waterLevel, int maxHeight) {
        this.waterLevel = waterLevel;
        if (minHeight < 0) {
            waterRamp.put(-Float.MAX_VALUE, new ColourGradient(minHeight, 0x000000, 0, 0x0000ff));
        }
        if (waterLevel > 0) {
            waterRamp.put(waterRamp.isEmpty() ? -Float.MAX_VALUE : 0.0f, new ColourGradient(0.0f, 0x0000ff, waterLevel, 0x00ffff));
        }
        if (waterLevel < 255) {
            final float step = (255 - waterLevel) / 5.0f;
            int colour = 0x007f00; // dark green
            for (int i = 0; i < 5; i++) {
                final int nextColour;
                switch (i) {
                    case 0:
                        nextColour = 0x00ff00; // green
                        break;
                    case 1:
                        nextColour = 0xffff00; // yellow
                        break;
                    case 2:
                        nextColour = 0xff8000; // orange
                        break;
                    case 3:
                        nextColour = 0xff0000; // red
                        break;
                    case 4:
                        nextColour = 0xffffff; // white
                        break;
                    default:
                        throw new InternalError();
                }
                landRamp.put((i > 0) ? (waterLevel + i * step) : -Float.MAX_VALUE,
                        new ColourGradient(waterLevel + i * step,
                                colour,
                                (i < 4) ? waterLevel + (i + 1) * step : 255.0f,
                                nextColour));
                colour = nextColour;
            }
        }
        if (maxHeight > 256) {
            landRamp.put(landRamp.isEmpty() ? -Float.MAX_VALUE : 255.0f, new ColourGradient(Math.max(255, waterLevel), 0xffffff, maxHeight, 0x8000ff)); // purple
        }
    }

    @Override
    public int getColour(float n) {
        if (Math.round(n) < waterLevel) {
            return waterRamp.floorEntry(n).getValue().getColour(n);
        } else {
            return landRamp.floorEntry(n).getValue().getColour(n);
        }
    }

    final int waterLevel;
    final NavigableMap<Float, ColourGradient> waterRamp = new TreeMap<>(), landRamp = new TreeMap<>();
}