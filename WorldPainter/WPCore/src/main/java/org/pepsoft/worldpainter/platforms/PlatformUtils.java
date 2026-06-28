package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.Platform;

import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

public final class PlatformUtils {
    private PlatformUtils() {
        // Prevent instantiation
    }

    /**
     * Determine the platform that would have originally created the chunk, and all platforms that the map could since
     * have been upgraded to and should therefore not be flagged as incompatible chunks.
     */
    // TODO make this dynamic
    public static Set<Platform> determineCompatiblePlatforms(Chunk chunk) {
        if (chunk instanceof MCRegionChunk) {
            return RESULT_MCREGION;
        } else if (chunk instanceof MC12AnvilChunk) {
            return RESULT_ANVIL;
        } else if (chunk instanceof MC115AnvilChunk) {
            if (((MC115AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_16_5) {
                return RESULT_ANVIL_1_17;
            } else {
                // These chunks could have been created by WorldPainter with platform 1.17, so return both
                return RESULT_ANVIL_1_15_AND_1_17;
            }
        } else if (chunk instanceof MC118AnvilChunk) {
            if (((MC118AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_21_11) {
                return RESULT_ANVIL_26_1;
            } else if (((MC118AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_21_10) {
                return RESULT_ANVIL_1_21_11_TO_26_1;
            } else if (((MC118AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_20_4) {
                return RESULT_ANVIL_1_20_5_TO_26_1;
            } else if (((MC118AnvilChunk) chunk).getInputDataVersion() > DATA_VERSION_MC_1_18_2) {
                return RESULT_ANVIL_1_19_TO_26_1;
            } else {
                return RESULT_ANVIL_1_18_TO_26_1;
            }
        } else {
            return null;
        }
    }

    private static final Set<Platform> RESULT_MCREGION              = singleton(JAVA_MCREGION);
    private static final Set<Platform> RESULT_ANVIL                 = singleton(JAVA_ANVIL);
    private static final Set<Platform> RESULT_ANVIL_1_17            = singleton(JAVA_ANVIL_1_17);
    private static final Set<Platform> RESULT_ANVIL_1_15_AND_1_17   = ImmutableSet.of(JAVA_ANVIL_1_15, JAVA_ANVIL_1_17);
    private static final Set<Platform> RESULT_ANVIL_1_18_TO_26_1    = ImmutableSet.of(JAVA_ANVIL_1_18, JAVA_ANVIL_1_19, JAVA_ANVIL_1_20_5, JAVA_ANVIL_1_21_11, JAVA_ANVIL_26_1);
    private static final Set<Platform> RESULT_ANVIL_1_19_TO_26_1    = ImmutableSet.of(JAVA_ANVIL_1_19, JAVA_ANVIL_1_20_5, JAVA_ANVIL_1_21_11, JAVA_ANVIL_26_1);
    private static final Set<Platform> RESULT_ANVIL_1_20_5_TO_26_1  = ImmutableSet.of(JAVA_ANVIL_1_20_5, JAVA_ANVIL_1_21_11, JAVA_ANVIL_26_1);
    private static final Set<Platform> RESULT_ANVIL_26_1            = singleton(JAVA_ANVIL_26_1);
    private static final Set<Platform> RESULT_ANVIL_1_21_11_TO_26_1 = ImmutableSet.of(JAVA_ANVIL_1_21_11, JAVA_ANVIL_26_1);
}