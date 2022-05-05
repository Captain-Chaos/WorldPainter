package org.pepsoft.minecraft;

/**
 * A chunk that stores block (and possibly more) data in 16x16x16 sections.
 */
public interface SectionedChunk extends Chunk {
    /**
     * Get the sections contained in the chunk. Some or all entries <em>may</em> be {@code null}.
     */
    Section[] getSections();

    /**
     * A 16x16x16 chunk section.
     */
    interface Section {
        /**
         * Indicates whether the secion is empty (contains only air and minecraft:plains biome).
         */
        boolean isEmpty();
    };
}