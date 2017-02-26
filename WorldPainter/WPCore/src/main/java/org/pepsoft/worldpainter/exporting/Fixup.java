package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;

/**
 * A delayed rendering operation, invoked after a region and all its neighbours
 * have been rendered, meant for operations which straddle region boundaries.
 *
 * @author SchmitzP
 */
public interface Fixup {
    /**
     * Perform the fixup. May do nothing if it is determined that the operation
     * no longer applies or is blocked due to earlier fixups.
     *
     * @param world The world in which to perform the fixup.
     * @param dimension The dimension for which to perform the fixup.
     * @param platform The platform for which the export is being performed.
     */
    void fixup(MinecraftWorld world, Dimension dimension, Platform platform);
}