package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MC115AnvilChunk;
import org.pepsoft.minecraft.MC118AnvilChunk;
import org.pepsoft.minecraft.SectionedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pepsoft.minecraft.Constants.STATUS_BIOMES;
import static org.pepsoft.minecraft.Constants.STATUS_STRUCTURE_STARTS;

public final class ChunkUtils {
    private ChunkUtils() {
        // Prevent instantiation
    }

    public static boolean skipChunk(Chunk chunk) {
        if ((chunk instanceof MC115AnvilChunk) || (chunk instanceof MC118AnvilChunk)) {
            final String status = (chunk instanceof MC115AnvilChunk) ? ((MC115AnvilChunk) chunk).getStatus() : ((MC118AnvilChunk) chunk).getStatus();
            if (status.equals(STATUS_STRUCTURE_STARTS) || status.equals(STATUS_BIOMES)) {
                boolean nonEmptySectionFound = false;
                for (SectionedChunk.Section section: ((SectionedChunk) chunk).getSections()) {
                    if ((section != null) && (! section.isEmpty())) {
                        nonEmptySectionFound = true;
                        break;
                    }
                }
                if (! nonEmptySectionFound) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping chunk {},{} because the status is {} and it is empty", chunk.getxPos(), chunk.getzPos(), status);
                    }
                    // Minecraft 1.18 seems to put lots of these empty chunks around the already generated parts; skip them
                    return true;
                }
            }
        }
        if (chunk instanceof SectionedChunk) {
            boolean sectionFound = false;
            for (SectionedChunk.Section section: ((SectionedChunk) chunk).getSections()) {
                if (section != null) {
                    sectionFound = true;
                    break;
                }
            }
            if (! sectionFound) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping chunk {},{} because it has no sections, or no sections with y >= minHeight", chunk.getxPos(), chunk.getzPos());
                }
                return true;
            }
        }
        return false;
    }

    private static final Logger logger = LoggerFactory.getLogger(ChunkUtils.class);
}