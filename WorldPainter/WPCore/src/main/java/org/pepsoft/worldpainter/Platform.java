package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * A descriptor for a WorldPainter-supported map storage format. Implements the
 * Enumeration pattern, meaning there is only ever one instance of each unique
 * platform, allowing the {@code ==} operator to be used with it.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public final class Platform implements Serializable {
    public Platform(String id, String displayName, int minMaxHeight, int standardMaxHeight, int maxMaxHeight, int minX,
                    int maxX, int minY, int maxY, List<GameType> supportedGameTypes,
                    List<Generator> supportedGenerators, List<Integer> supportedDimensions,
                    Set<Capability> capabilities) {
        this(id, displayName, defaultMaxHeightsFromTo(minMaxHeight, maxMaxHeight), standardMaxHeight, minX, maxX, minY, maxY, supportedGameTypes, supportedGenerators, supportedDimensions, capabilities);
    }

    private static int[] defaultMaxHeightsFromTo(int minMaxHeight, int maxMaxHeight) {
        int minExponent = (int) Math.ceil(Math.log(minMaxHeight)/Math.log(2));
        int maxExponent = (int) Math.floor(Math.log(maxMaxHeight)/Math.log(2));
        List<Integer> maxHeights = new ArrayList<>();
        for (int i = minExponent; i <= maxExponent; i++) {
            maxHeights.add((int) Math.pow(2, i));
        }
        return maxHeights.stream().mapToInt(Integer::intValue).toArray();
    }

    public Platform(String id, String displayName, int[] maxHeights, int standardMaxHeight, int minX, int maxX,
                    int minY, int maxY, List<GameType> supportedGameTypes, List<Generator> supportedGenerators,
                    List<Integer> supportedDimensions, Set<Capability> capabilities) {
        synchronized (ALL_PLATFORMS) {
            if (ALL_PLATFORMS.containsKey(id)) {
                throw new IllegalStateException("There is already a platform with ID " + id);
            }
            this.id = id;
            this.displayName = displayName;
            this.minMaxHeight = maxHeights[0];
            this.maxHeights = maxHeights;
            this.standardMaxHeight = standardMaxHeight;
            this.maxMaxHeight = maxHeights[maxHeights.length - 1];
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.supportedGameTypes = ImmutableList.copyOf(supportedGameTypes);
            this.supportedGenerators = ImmutableList.copyOf(supportedGenerators);
            this.supportedDimensions = ImmutableList.copyOf(supportedDimensions);
            this.capabilities = Sets.immutableEnumSet(capabilities);
            ALL_PLATFORMS.put(id, this);
        }
    }

    /**
     * Determines whether a world could be retargeted to this platform without
     * requiring any changes or edits.
     * 
     * @param world The world to check for compatibility.
     * @return {@code true} if the world could be trivially retargeted to
     * this platform.
     */
    public boolean isCompatible(World2 world) {
        if ((world.getMaxHeight() < minMaxHeight)
                || (world.getMaxHeight() > maxMaxHeight)) {
            return false;
        }
        for (Dimension dimension: world.getDimensions()) {
            if ((dimension.getDim() >= 0) && (! supportedDimensions.contains(dimension.getDim()))) {
                return false;
            }
        }
        return true;
    }
    
    // Object

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Platform getById(String id) {
        return ALL_PLATFORMS.get(id);
    }

    private Object readResolve() throws ObjectStreamException {
        synchronized (ALL_PLATFORMS) {
            if (ALL_PLATFORMS.containsKey(id)) {
                return ALL_PLATFORMS.get(id);
            } else {
                ALL_PLATFORMS.put(id, this);
                return this;
            }
        }
    }

    /**
     * The globally unique ID of this platform.
     */
    public final String id;

    /**
     * The human-friendly display name of this platform.
     */
    public final String displayName;

    /**
     * Get the lowest map height supported by this platform.
     *
     * @return The lowest map height supported by this platform.
     */
    public final int minMaxHeight;

    /**
     * Get the default height of maps for this platform.
     *
     * @return The default height of maps for this platform.
     */
    public final int standardMaxHeight;

    /**
     * Get the highest map height supported by this platform.
     *
     * @return The highest map height supported by this platform.
     */
    public final int maxMaxHeight;

    /**
     * The lowest possible X coordinate (in blocks) for this platform.
     */
    public final int minX;

    /**
     * The highest possible X coordinate (in blocks) for this platform.
     */
    public final int maxX;

    /**
     * The lowest possible Y coordinate (in blocks) for this platform.
     */
    public final int minY;

    /**
     * The highest possible Y coordinate (in blocks) for this platform.
     */
    public final int maxY;

    /**
     * Get the list of game types supported by this platform.
     */
    public final List<GameType> supportedGameTypes;

    /**
     * The list of generators supported by this platform.
     */
    public final List<Generator> supportedGenerators;

    /**
     * The list of dimension IDs supported by this platform.
     */
    public final List<Integer> supportedDimensions;

    /**
     * The set of capabilities supported by this platform.
     */
    public final Set<Capability> capabilities;

    /**
     * The list of maxHeights to present to the user. The plugin <em>may</em> support other maxHeights, or may not, but
     * if it is in this list it must be supported. The list must be in ascending order.
     */
    public final int[] maxHeights;

    private static final Map<String, Platform> ALL_PLATFORMS = new HashMap<>();

    private static final long serialVersionUID = 1L;

    public enum Capability {
        /**
         * Has the concept of a 2D, per-column biome, identified by a number. This is mutually exclusive with
         * {@link #BIOMES_3D}.
         */
        BIOMES,

        /**
         * Stores precalculated light values, so a lighting pass is needed.
         */
        PRECALCULATED_LIGHT,

        /**
         * Can set the location where the player will initially spawn.
         */
        SET_SPAWN_POINT,

        /**
         * This is a Minecraft-like block based platform, based an a 3D map
         * made up of large cube-shaped voxels.
         */
        BLOCK_BASED,

        /**
         * The platform uses named blocks rather than numerical IDs.
         */
        NAME_BASED,

        /**
         * The platform uses a numerical world seed to procedurally generate
         * deterministic maps.
         */
        SEED,

        /**
         * The platform supports the "populate" flag to indicate that chunks
         * should be populated by the game according to the biome painted in
         * WorldPainter.
         */
        POPULATE,

        /**
         * Has the concept of a 3D, per-4x4x4-cube biome, identified by a number. This is mutually exclusive with
         * {@link #BIOMES}.
         */
        BIOMES_3D
    }
}