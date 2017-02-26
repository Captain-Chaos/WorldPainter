package org.pepsoft.worldpainter.mapexplorer;

import java.io.File;

/**
 * Created by Pepijn on 25-2-2017.
 */
public interface MapRecognizer {
    boolean isMap(File dir);
    Node getMapNode(File mapDir);
}
