package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.pepsoft.util.AttributeKey;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

import static org.pepsoft.minecraft.Constants.MC_GRASS_BLOCK;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.Platform.Capability.*;

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
                    Set<Capability> capabilities, Object... attributes) {
        this(id, displayName, defaultMaxHeightsFromTo(minMaxHeight, maxMaxHeight), standardMaxHeight, minX, maxX, minY, maxY, new int[] { 0 }, 0, supportedGameTypes, supportedGenerators, supportedDimensions, capabilities, attributes);
    }

    public Platform(String id, String displayName, int[] maxHeights, int standardMaxHeight, int minX, int maxX,
                    int minY, int maxY, int[] minHeights, int standardMinHeight, List<GameType> supportedGameTypes, List<Generator> supportedGeneratorTypes,
                    List<Integer> supportedDimensions, Set<Capability> capabilities, Object... attributes) {
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
            this.minMinHeight = minHeights[minHeights.length - 1];
            this.minHeights = minHeights;
            this.minZ = standardMinHeight;
            this.maxMinHeight = minHeights[0];
            this.supportedGameTypes = ImmutableList.copyOf(supportedGameTypes);
            this.supportedGenerators = ImmutableList.copyOf(supportedGeneratorTypes);
            this.supportedDimensions = ImmutableList.copyOf(supportedDimensions);
            this.capabilities = Sets.immutableEnumSet(capabilities);
            if ((attributes != null) && (attributes.length > 0)) {
                if (attributes.length % 2 != 0) {
                    throw new IllegalArgumentException("attributes array must have even length");
                }
                final ImmutableMap.Builder<String, Serializable> mapBuilder = ImmutableMap.builder();
                for (int i = 0; i < attributes.length; i += 2) {
                    if (attributes[i] == null) {
                        throw new NullPointerException("attributes[" + i + "]");
                    } else if (attributes[i + 1] == null) {
                        throw new NullPointerException("attributes[" + (i + 1) + "]");
                    }
                    mapBuilder.put(((AttributeKey<?>) attributes[i]).key, (Serializable) attributes[i + 1]);
                }
                this.attributes = mapBuilder.build();
            } else {
                this.attributes = null;
            }
            ALL_PLATFORMS.put(id, this);
        }
    }

    // Kept for backwards compatibility with existing plugins
    @Deprecated
    public Platform(String id, String displayName, int minMaxHeight, int standardMaxHeight, int maxMaxHeight, int minX,
                    int maxX, int minY, int maxY, List<GameType> supportedGameTypes,
                    List<Generator> supportedGenerators, List<Integer> supportedDimensions,
                    Set<Capability> capabilities) {
        this(id, displayName, defaultMaxHeightsFromTo(minMaxHeight, maxMaxHeight), standardMaxHeight, minX, maxX, minY, maxY, new int[] { 0 }, 0, supportedGameTypes, supportedGenerators, supportedDimensions, capabilities, (Object[]) null);
    }

    // Kept for backwards compatibility with existing plugins
    @Deprecated
    public Platform(String id, String displayName, int[] maxHeights, int standardMaxHeight, int minX, int maxX,
                    int minY, int maxY, int standardMinHeight, List<GameType> supportedGameTypes, List<Generator> supportedGeneratorTypes,
                    List<Integer> supportedDimensions, Set<Capability> capabilities, Object... attributes) {
        this(id, displayName, maxHeights, standardMaxHeight, minX, maxX, minY, maxY, new int[] { standardMinHeight }, standardMinHeight, supportedGameTypes, supportedGeneratorTypes, supportedDimensions, capabilities, attributes);
    }

    // Kept for backwards compatibility with existing plugins
    @Deprecated
    public Platform(String id, String displayName, int[] maxHeights, int standardMaxHeight, int minX, int maxX,
                    int minY, int maxY, int standardMinHeight, List<GameType> supportedGameTypes, List<Generator> supportedGeneratorTypes,
                    List<Integer> supportedDimensions, Set<Capability> capabilities) {
        this(id, displayName, maxHeights, standardMaxHeight, minX, maxX, minY, maxY, new int[] { standardMinHeight }, standardMinHeight, supportedGameTypes, supportedGeneratorTypes, supportedDimensions, capabilities, (Object[]) null);
    }

    /**
     * Determines whether a world could be retargeted to this platform without requiring any changes or edits.
     * 
     * @param world The world to check for compatibility.
     * @return {@code null} if the world could be trivially retargeted to this platform, or a short description of the
     * reason if it cannot.
     */
    public String isCompatible(World2 world) {
        if (world.getMinHeight() < minMinHeight) {
            return "World lower build limit (" + world.getMinHeight() + ") is lower than the minimum lower build limit supported by map format (" + minMinHeight + ")";
        } else if (world.getMinHeight() > maxMinHeight) {
            return "World lower build limit (" + world.getMinHeight() + ") is higher than the maximum lower build limit supported by map format (" + maxMinHeight + ")";
        }
        if (world.getMaxHeight() < minMaxHeight) {
            return "World upper build limit (" + world.getMaxHeight() + ") is lower than the minimum upper build limit supported by map format (" + minMaxHeight + ")";
        } else if (world.getMaxHeight() > maxMaxHeight) {
            return "World upper build limit (" + world.getMaxHeight() + ") is higher than the maximum upper build limit supported by map format (" + maxMaxHeight + ")";
        }
        for (Dimension dimension: world.getDimensions()) {
            final Dimension.Anchor anchor = dimension.getAnchor();
            if ((anchor.role == DETAIL) && (! anchor.invert) && (! supportedDimensions.contains(anchor.dim))) {
                return "Map format does not support dimension " + anchor.getDefaultName();
            }
        }
        return null;
    }

    /**
     * Convenience method for determining whether the platform supports <em>any</em> type of biomes
     * ({@link #capabilities} contains {@link Capability#BIOMES}, {@link Capability#BIOMES_3D} or
     * {@link Capability#NAMED_BIOMES}).
     */
    public boolean supportsBiomes() {
        return capabilities.contains(BIOMES) || capabilities.contains(BIOMES_3D) || capabilities.contains(NAMED_BIOMES);
    }

    /**
     * Convenience method for getting an attribute value. If the attribute is not set then {@code key.defaultValue} is
     * returned.
     */
    public <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        return (attributes != null) ? key.get(attributes) : key.defaultValue;
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
     * The lowest upper build limit supported by this platform.
     */
    public final int minMaxHeight;

    /**
     * The default upper build limit of maps for this platform.
     */
    public final int standardMaxHeight;

    /**
     * The highest upper build limit supported by this platform.
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

    /**
     * The default lower build limit of maps for this platform.
     */
    public final int minZ;

    /**
     * Additional custom defined attributes that do not apply to all platforms. May be {@code null}.
     */
    public final Map<String, Serializable> attributes;

    /**
     * The lowest lower build limit of maps for this platform.
     */
    public final int minMinHeight;

    /**
     * The highest lower build limit of maps for this platform.
     */
    public final int maxMinHeight;

    /**
     * The list of minHeights to present to the user. The plugin <em>may</em> support other minHeights, or may not, but
     * if it is in this list it must be supported. The list must be in descending order.
     */
    public final int[] minHeights;

    /**
     * The name of the "grass block" block type for the platform. Default value: {@code minecraft:grass_block}.
     */
    public static final AttributeKey<String> ATTRIBUTE_GRASS_BLOCK_NAME = new AttributeKey<>("blocks.grass_block.name", MC_GRASS_BLOCK);

    /**
     * The opacity of a water block. Default value: 1. A value of 1 also implies that the top layer of water has full
     * daylight lighting.
     */
    public static final AttributeKey<Integer> ATTRIBUTE_WATER_OPACITY = new AttributeKey<>("blocks.water.opacity", 1);

    private static final Map<String, Platform> ALL_PLATFORMS = new HashMap<>();

    private static final long serialVersionUID = 1L;

    private static int[] defaultMaxHeightsFromTo(int minMaxHeight, int maxMaxHeight) {
        int minExponent = (int) Math.ceil(Math.log(minMaxHeight)/Math.log(2));
        int maxExponent = (int) Math.floor(Math.log(maxMaxHeight)/Math.log(2));
        List<Integer> maxHeights = new ArrayList<>();
        for (int i = minExponent; i <= maxExponent; i++) {
            maxHeights.add((int) Math.pow(2, i));
        }
        return maxHeights.stream().mapToInt(Integer::intValue).toArray();
    }

    // TODO capabilities can be seen as attributes of type boolean; should we just migrate them to that?
    public enum Capability {
        /**
         * Has the concept of a 2D, per-column biome, identified by a number. This is mutually exclusive with
         * {@link #BIOMES_3D} and {@link #NAMED_BIOMES}. Note that the biomes may still be stored as 4x4x4 3D biomes (as
         * Minecraft 1.15 does). This will be determined per chunk based on the chunk capabilities (and can therefore
         * vary by chunk). But in-game the biome will still be the same throughout every vertical column.
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
         * {@link #BIOMES} and {@link #NAMED_BIOMES}.
         */
        BIOMES_3D,

        /**
         * Has the concept of named, namespaced biomes, stored per 4x4x4 cube of blocks (like {@link #BIOMES_3D},
         * identified by a string. This is mutually exclusive with {@link #BIOMES} and {@link #BIOMES_3D}.
         */
        NAMED_BIOMES,

        /**
         * Supports generator settings per dimension. Platforms without this capability only support generator settings
         * for {@link Constants#DIM_NORMAL DIM_NORMAL}.
         */
        GENERATOR_PER_DIMENSION,

        /**
         * {@code *_leaves} blocks have a {@code distance} property from 1 to 7, indicating the distance from the tree
         * trunk, where 7 means it is too far away and will decay.
         */
        LEAF_DISTANCES,

        /**
         * Leaf blocks have a {@code waterlogged} property and can therefore be placed in water.
         */
        WATERLOGGED_LEAVES,

        /**
         * Supports Minecraft-style data packs.
         */
        DATA_PACKS
    }
}