package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.Platform;

import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_16_5;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

public final class PlatformUtils {
    private PlatformUtils() {
        // Prevent instantiation
    }

    // TODO make this dynamic
    public static Set<Platform> determineNativePlatforms(Chunk chunk) {
        if (chunk instanceof MCRegionChunk) {
            return singleton(JAVA_MCREGION);
        } else if (chunk instanceof MC12AnvilChunk) {
            return singleton(JAVA_ANVIL);
        } else if (chunk instanceof MC115AnvilChunk) {
            if (((MC115AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_16_5) {
                return singleton(JAVA_ANVIL_1_17);
            } else {
                // These chunks could have been created by WorldPainter with platform 1.17, so return both
                return ImmutableSet.of(JAVA_ANVIL_1_15, JAVA_ANVIL_1_17);
            }
        } else if (chunk instanceof MC118AnvilChunk) {
            return singleton(JAVA_ANVIL_1_18);
        } else {
            return null;
        }
    }
}