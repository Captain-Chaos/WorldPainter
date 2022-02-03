package org.pepsoft.worldpainter.mapexplorer;

import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.io.File;

public interface MapExplorerSupport extends PlatformProvider {
    Node getMapNode(File mapDir);
}