/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.util.Set;

/**
 *
 * @author pepijn
 */
public class GardenExporter {
    /**
     * First pass, executed after all the second pass exporters for other layers
     * have executed, meant for building exteriors of buildings, etc.
     *
     * @param dimension
     * @param tile
     * @param platform
     * @param minecraftWorld
     */
    public void firstPass(Dimension dimension, Tile tile, Platform platform, MinecraftWorld minecraftWorld, Set<Seed> processedSeeds) {
        // Process the seed. If it has a parent, process it first, etc.,
        // regardless of whether the ancestors are in this tile.
        // If the seed is already processed then all its ancestors are also
        // already processed.
        tile.getSeeds().stream()
            .filter(seed -> !processedSeeds.contains(seed))
            .forEach(seed -> processSeedFirstPass(seed, processedSeeds, dimension, tile, platform, minecraftWorld));
    }

    /**
     * Second pass, executed after the first pass has finished, meant for
     * building interiors of buildings, for instance.
     *
     * @param dimension
     * @param tile
     * @param platform
     * @param minecraftWorld
     */
    public void secondPass(Dimension dimension, Tile tile, Platform platform, MinecraftWorld minecraftWorld, Set<Seed> processedSeeds) {
        // Process the seed. If it has a parent, process it first, etc.,
        // regardless of whether the ancestors are in this tile.
        // If the seed is already processed then all its ancestors are also
        // already processed.
        tile.getSeeds().stream()
            .filter(seed -> !processedSeeds.contains(seed))
            .forEach(seed -> processSeedSecondPass(seed, processedSeeds, dimension, tile, platform, minecraftWorld));
    }
    
    private void processSeedFirstPass(Seed seed, Set<Seed> processedSeeds, Dimension dimension, Tile tile, Platform platform, MinecraftWorld world) {
        if ((seed.parent != null) && (! processedSeeds.contains(seed.parent))) {
            processSeedFirstPass(seed.parent, processedSeeds, dimension, tile, platform, world);
        }
        if (seed.isSprouted()) {
//            System.out.println("Building " + seed.getClass().getSimpleName() + " at " + seed.location.x + "," + seed.location.y);
            seed.buildFirstPass(dimension, tile, platform, world);
            processedSeeds.add(seed);
        }
    }
    
    private void processSeedSecondPass(Seed seed, Set<Seed> processedSeeds, Dimension dimension, Tile tile, Platform platform, MinecraftWorld world) {
        if ((seed.parent != null) && (! processedSeeds.contains(seed.parent))) {
            processSeedSecondPass(seed.parent, processedSeeds, dimension, tile, platform, world);
        }
        if (seed.isSprouted()) {
//            System.out.println("Building " + seed.getClass().getSimpleName() + " at " + seed.location.x + "," + seed.location.y);
            seed.buildSecondPass(dimension, tile, platform, world);
            processedSeeds.add(seed);
        }
    }
}