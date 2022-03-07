package org.pepsoft.minecraft;

public interface SectionedChunk extends Chunk {
    Section[] getSections();

    interface Section {
        // Empty
    };
}