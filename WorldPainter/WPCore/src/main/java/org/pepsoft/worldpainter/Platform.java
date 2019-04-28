package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A descriptor for a WorldPainter-supported map storage format. Implements the
 * Enumeration pattern, meaning there is only ever one instance of each unique
 * platform, allowing the <code>==</code> operator to be used with it.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public final class Platform implements Serializable {
    public Platform(String id, String displayName, int minMaxHeight, int standardMaxHeight,
                    int maxMaxHeight, int minX, int maxX, int minY, int maxY, List<GameType> supportedGameTypes,
                    List<Generator> supportedGenerators, List<Integer> supportedDimensions, Set<Capability> capabilities) {
        synchronized (ALL_PLATFORMS) {
            if (ALL_PLATFORMS.containsKey(id)) {
                throw new IllegalStateException("There is already a platform with ID " + id);
            }
            this.id = id;
            this.displayName = displayName;
            this.minMaxHeight = minMaxHeight;
            this.standardMaxHeight = standardMaxHeight;
            this.maxMaxHeight = maxMaxHeight;
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
     * @return <code>true</code> if the world could be trivially retargeted to
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

    private static final Map<String, Platform> ALL_PLATFORMS = new HashMap<>();

    private static final long serialVersionUID = 1L;

    public enum Capability {
        /**
         * Has the concept of a per-column biome, identified by a number.
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
        SEED
    }
}