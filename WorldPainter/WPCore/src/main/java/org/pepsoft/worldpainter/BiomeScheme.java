/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.Set;

/**
 *
 * @author pepijn
 */
public interface BiomeScheme {
    /**
     * Set the seed for which to determine the biomes.
     * 
     * @param seed The seed for which to determine the biomes.
     */
    void setSeed(long seed);
    
    /**
     * Get the number of biomes. The biomes must be consecutively numbered from
     * zero to one lower than this number.
     * 
     * @return The number of biomes.
     */
    int getBiomeCount();
    
    /**
     * Get the biomes for a specific rectangular area of the world.
     * 
     * <p><b>Note:</b> the returned buffer is only valid until the next
     * invocation of this method. If the method is invoked again the results of
     * the buffer may be overwritten or otherwise become invalid.
     * 
     * @param x The X coordinate in the WorldPainter coordinate system.
     * @param y The Y coordinate in the WorldPainter coordinate system.
     * @param width The width along the X axis of the area.
     * @param height The height along the Y axis of the area.
     * @return An array with biome information indexed first by X and then by Y
     *     coordinate.
     */
    int[] getBiomes(int x, int y, int width, int height);
    
    /**
     * Get the biomes for a specific rectangular area of the world.
     * 
     * @param x The X coordinate in the WorldPainter coordinate system.
     * @param y The Y coordinate in the WorldPainter coordinate system.
     * @param width The width along the X axis of the area.
     * @param height The height along the Y axis of the area.
     * @param buffer The array in which the biome information will be returned,
     *     indexed first by X and then by Y coordinate.
     */
    void getBiomes(int x, int y, int width, int height, int[] buffer);
    
    /**
     * Get the list of biome names. The numbers returned by
     * {@link #getBiomes(int[], int, int, int, int)} can be used as index into
     * this array to find the name of the biome.
     * 
     * @return The list of biome names.
     */
    String[] getBiomeNames();
    
    /**
     * Get the set of dry biomes (biomes in which no precipitation falls), as
     * a set of integers, corresponding to the integers returned by the
     * {@link #getBiomes(int[], int, int, int, int)} method.
     * 
     * @return The set of dry biomes.
     */
    Set<Integer> getDryBiomes();

    /**
     * Get the set of cold biomes (biomes in which water will freeze and any
     * precipitation will fall as snow), as a set of integers, corresponding to
     * the integers returned by the
     * {@link #getBiomes(int[], int, int, int, int)} method.
     * 
     * @return The set of cold biomes.
     */
    Set<Integer> getColdBiomes();

    /**
     * Get the set of forested biomes (biomes in which many trees grow), as a
     * set of integers, corresponding to the integers returned by the
     * {@link #getBiomes(int[], int, int, int, int)} method.
     * 
     * @return The set of forested biomes.
     */
    Set<Integer> getForestedBiomes();

    /**
     * Get the set of swampy biomes (biomes in which swamp trees grow and the
     * water is green), as a set of integers, corresponding to the integers
     * returned by the {@link #getBiomes(int[], int, int, int, int)} method.
     * 
     * @return The set of swampy biomes.
     */
    Set<Integer> getSwampyBiomes();
    
    /**
     * Get the colour of the specified biome. At its option the biome scheme may
     * use the specified colour scheme to determine the colour, but it may also
     * ignore it completely.
     * 
     * @param biome The biome for which to determine the colour.
     * @param colourScheme A colour scheme which the biome scheme may use, at
     *     its option, for determining the colour.
     * @return The colour to be used for painting the specified biome in
     *     0xRRGGBB format.
     */
    int getColour(int biome, ColourScheme colourScheme);
    
    /**
     * The two dimensional pattern to use for painting the specified biome, if
     * any. May be null. If not null, must be a 16 by 16 array of booleans,
     * where <code>true</code> indicates a foreground pixel.
     * 
     * @param biome The biome for which to return the pattern.
     * @return The pattern to use for painting the specified biome, or
     *     <code>null</code> if no pattern should be used.
     */
    boolean[][] getPattern(int biome);
    
    /**
     * Get the name of the specified biome in this biome scheme.
     * 
     * @param biome The biome for which to retrieve the name.
     * @return The name of the specified biome.
     */
    String getBiomeName(int biome);
    
    /**
     * Indicates whether the specified biome ID is present in this biome scheme.
     * If this returns false, the other methods may thrown an exception if
     * invoked for that ID.
     * 
     * @param biome The biome ID to check.
     * @return <code>true</code> if this biome scheme contains the specified
     *     biome.
     */
    boolean isBiomePresent(int biome);
}