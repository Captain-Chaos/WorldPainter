/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import java.util.List;
import java.util.Set;

import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.layers.Layer;

/**
 * The "garden" concept is a way of dynamically creating complex random
 * realistic structures which adhere to a set of rules, for example villages
 * with roads, buildings, etc.
 *
 * <p>The garden contains "seeds", which can be planted by a WorldPainter
 * operation. Each seed has a germination time. The garden is ticked over, and
 * when a seed's germination time is reached it tries to sprout. This may or may
 * not succeed according to whether there is space in the world. Earlier
 * sprouted seeds may prevent the seed from sprouting.
 *
 * <p>The garden keeps track of which blocks are already occupied by sprouted
 * seeds, and by which category of seed they are occupied. When they sprout,
 * seeds may mark an area as occupied.
 *
 * <p>If it does sprout, the seed can plant new seeds, which may or may not
 * sprout according to whether there is room. In this way complex yet random
 * structures may be built up while still following complex rules.
 *
 * <p>When the world is exported each sprouted seed is allowed to render itself
 * to the map. This is done in two phases, so a seed may for instance render its
 * exterior in phase one and its interior in phase two, which may help with
 * keeping things realistic.
 *
 * @author pepijn
 */
public interface Garden {
    /**
     * Remove a layer from the world in a square area with a particular radius
     * around a particular location.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @param layer The layer to remove.
     * @param radius The radius of the square to remove.
     */
    void clearLayer(int x, int y, Layer layer, int radius);

    /**
     * Mark a location as being occupied by a particular category of seed.
     *
     * @param x The X coordinate to mark.
     * @param y The Y coordinate to mark.
     * @param category The category with which to mark the location as one
     *     of the <code>CATEGORY_*</code> constants in the
     *     {@link GardenCategory} class.
     */
    void setCategory(int x, int y, int category);

    /**
     * Get the category of seed, if any, which occupies a particular location.
     *
     * @param x The X coordinate to get.
     * @param y The Y coordinate to get.
     * @return The category of seed that occupies the specified location as one
     *     of the <code>CATEGORY_*</code> constants in the
     *     {@link GardenCategory} class, or 0 if it is unoccupied.
     */
    int getCategory(int x, int y);

    /**
     * Get all the seeds in the garden. Note that seeds which fail to sprout are
     * removed from the garden.
     *
     * @return A set containing all the seeds in the garden.
     */
    Set<Seed> getSeeds();

    /**
     * Find seeds of a particular type in a square area around a particular
     * location.
     *
     * @param type The type of seed to locate.
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @param radius The radius of the square.
     * @param <T> The type of seed to locate.
     * @return A list of the existent seeds of the specified type in the
     *     specified square. May be empty, but not <code>null</code>.
     */
    <T extends Seed> List<T> findSeeds(Class<T> type, int x, int y, int radius);

    /**
     * Determine whether a location is already marked as occupied, or is
     * flooded.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @return <code>true</code> if the location is marked as occupied, or is
     *     flooded.
     */
    boolean isOccupied(int x, int y);

    /**
     * Determine whether a location is marked as occupied with water, or is
     * actually under water.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @return <code>true</code> if the location is marked as occupied with
     *     water, or is actually under water.
     */
    boolean isWater(int x, int y);

    /**
     * Determine whether a location is flooded with lava.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @return <code>true</code> if the location is flooded with lava.
     */
    boolean isLava(int x, int y);

    /**
     * Plant a seed in the garden.
     *
     * @param seed The seed to plant.
     * @return <code>true</code> if the seed was planted; <code>false</code> if
     * it could not be planted for some reason.
     */
    boolean plantSeed(Seed seed);

    /**
     * Remove a seed from the garden.
     *
     * @param seed The seed to remove.
     */
    void removeSeed(Seed seed);

    /**
     * Get the precise terrain height of a location.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @return The precise terrain height of the specified location as a
     *     floating point number.
     */
    float getHeight(int x, int y);
    
    /**
     * Get the rounded terrain height of a location.
     *
     * @param x The X coordinate of the location.
     * @param y The Y coordinate of the location.
     * @return The rounded terrain height of the specified location as an
     *     integer number.
     */
    int getIntHeight(int x, int y);

    /**
     * Tick the garden over. This involves ticking over all seeds which haven't
     * germinated yet, allowing them to either sprout or die, optionally
     * planting new seeds in the process. Returns a flag indicating whether all
     * seeds have now either sprouted or died, meaning that the garden is in a
     * steady state until new seeds are planted, and does not need to be ticked
     * over anymore.
     *
     * @return <code>true</code> if all seeds have either sprouted or died,
     *     meaning that the garden is in a steady state until new seeds are
     *     planted, and does not need to be ticked over anymore.
     *     <code>false</code> if there are still living seeds and the garden
     *     needs further ticking over.
     */
    boolean tick();

    /**
     * Neutralise the garden by killing all seeds regardless of whether they
     * have germinated. After this call the garden is in a steady state and
     * needs no further ticking over.
     */
    void neutralise();
}