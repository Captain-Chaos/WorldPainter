/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.dynmap;

import org.dynmap.ColorScheme;
import org.dynmap.renderer.DynmapBlockState;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.WATER;

/**
 * An implementation of {@link ColourScheme} which can read
 * <a href="https://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/1286593-dynmap-dynamic-web-based-maps-for-minecraft">Dynmap</a>
 * "classic" colour scheme files, such as those included with Dynmap.
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
                return WATER.colour;
            }
            final DynmapBlockState blockState = DynmapBlockStateHelper.getDynmapBlockState(material);
            if ((blockState != null) && (blockState.globalStateIndex < dynmapColorScheme.colors.length)) {
                return dynmapColorScheme.colors[blockState.globalStateIndex][step].getARGB();
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
}