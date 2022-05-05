package org.pepsoft.worldpainter.mapexplorer;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.io.File;

/**
 * @deprecated Will never be used. Do not implement.
 */
@Deprecated
public interface MapRecognizer {
    /**
     * @deprecated Replaced by {@link PlatformProvider#identifyMap(File)}.
     */
    @Deprecated
    Platform identifyPlatform(File dir);

    /**
     * @deprecated Replaced by {@link MapExplorerSupport#getMapNode(File)}.
     */
    @Deprecated
    Node getMapNode(File mapDir);
}
