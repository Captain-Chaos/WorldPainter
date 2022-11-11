/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.dynmap;

import org.dynmap.ColorScheme;
import org.dynmap.renderer.DynmapBlockState;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.WATER;

/**
 * An implementation of {@link ColourScheme} which delegates to a Dynmap {@link ColorScheme}.
 *
 * @author pepijn
 */
public final class DynmapColourScheme implements ColourScheme {
    private DynmapColourScheme(ColorScheme dynmapColorScheme, int step) {
        this.dynmapColorScheme = dynmapColorScheme;
        this.step = step;
    }

    @Override
    public int getColour(Material material) {
        // TODO: optimise this further?
        return cache.computeIfAbsent(material, k -> {
            if (material.isNamed(MC_WATER)) {
                // TODO find out how to properly determine water colour from Dynmap
                return WATER.colour;
            }
            final DynmapBlockState blockState = DynmapBlockStateHelper.getDynmapBlockState(material);
            if (blockState != null) {
                if (blockState.globalStateIndex < dynmapColorScheme.colors.length) {
                    if (dynmapColorScheme.colors[blockState.globalStateIndex] != null) {
                        return dynmapColorScheme.colors[blockState.globalStateIndex][step].getARGB();
                    } else {
                        logger.warn("Colour table contains null for global state index {}\nMaterial: {}\nDynmapBlockState: {}", blockState.globalStateIndex, material.toFullString(), blockState);
                    }
                } else {
                    logger.warn("Global state index {} exceeds colour table bounds\nMaterial: {}\nDynmapBlockState: {}", blockState.globalStateIndex, material.toFullString(), blockState);
                }
            } else {
                logger.warn("DynmapBlockState missing for material: {}", material.toFullString());
            }
            return material.colour;
        });
    }

    public static DynmapColourScheme loadDynMapColourScheme(String name, int step) {
        DynmapBlockStateHelper.initialise();
        final ColorScheme colorScheme = ColorScheme.getScheme(null, name);
        return new DynmapColourScheme(colorScheme, step);
    }

    private final ColorScheme dynmapColorScheme;
    private final int step;
    private final Map<Material, Integer> cache = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DynmapColourScheme.class);
}