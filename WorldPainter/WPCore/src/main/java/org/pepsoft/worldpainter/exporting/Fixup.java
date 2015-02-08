package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;

/**
 * @author SchmitzP
 */
public interface Fixup {
    void fixup(MinecraftWorld world, Dimension dimension);
}