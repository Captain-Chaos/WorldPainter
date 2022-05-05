package org.pepsoft.minecraft;

import org.pepsoft.worldpainter.Generator;

public class SeededGenerator extends MapGenerator {
    public SeededGenerator(Generator type, long seed) {
        super(type);
        this.seed = seed;
    }

    public final long getSeed() {
        return seed;
    }

    public final void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public String toString() {
        return getType().name() + "(seed=" + seed + ")";
    }

    private long seed;

    private static final long serialVersionUID = 1L;
}