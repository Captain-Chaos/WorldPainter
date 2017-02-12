package org.pepsoft.minecraft;

import org.pepsoft.worldpainter.GameType;
import org.pepsoft.worldpainter.Generator;

import java.io.Serializable;
import java.util.List;

/**
 * A WorldPainter-supported block-based game platform.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public final class Platform implements Serializable {
    public Platform(String id, String displayName, boolean supportsBiomes, int minMaxHeight, int standardMaxHeight,
                    int maxMaxHeight, List<GameType> supportedGameTypes, List<Generator> supportedGenerators) {
        this.id = id;
        this.displayName = displayName;
        this.supportsBiomes = supportsBiomes;
        this.minMaxHeight = minMaxHeight;
        this.standardMaxHeight = standardMaxHeight;
        this.maxMaxHeight = maxMaxHeight;
        this.supportedGameTypes = supportedGameTypes;
        this.supportedGenerators = supportedGenerators;
    }

    /**
     * Indicates whether this platform supports storing biome IDs.
     *
     * @return <code>true</code> iff the platform supports storing biome IDs.
     */
    public boolean supportsBiomes() {
        return supportsBiomes;
    }

    /**
     * Get the lowest map height supported by this platform.
     *
     * @return The lowest map height supported by this platform.
     */
    public int getMinMaxHeight() {
        return minMaxHeight;
    }

    /**
     * Get the default height of maps for this platform.
     *
     * @return The default height of maps for this platform.
     */
    public int getStandardMaxHeight() {
        return standardMaxHeight;
    }

    /**
     * Get the highest map height supported by this platform.
     *
     * @return The highest map height supported by this platform.
     */
    public int getMaxMaxHeight() {
        return maxMaxHeight;
    }

    /**
     * Get the list of game types supported by this platform.
     *
     * @return The list of game types supported by this platform.
     */
    public List<GameType> getGameTypes() {
        return supportedGameTypes;
    }

    /**
     * Get the list of generators supported by this platform.
     *
     * @return The list of generators supported by this platform.
     */
    public List<Generator> getGenerators() {
        return supportedGenerators;
    }

    /**
     * Get the human-friendly display name of this platform.
     *
     * @return The human-friendly display name of this platform.
     */
    public String getDisplayName() {
        return displayName;
    }

    // Object

    @Override
    public boolean equals(Object o) {
        return (o instanceof Platform) && id.equals(((Platform) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private final String id, displayName;
    private final boolean supportsBiomes;
    private final int minMaxHeight, standardMaxHeight, maxMaxHeight;
    private final List<GameType> supportedGameTypes;
    private final List<Generator> supportedGenerators;

    private static final long serialVersionUID = 1L;
}