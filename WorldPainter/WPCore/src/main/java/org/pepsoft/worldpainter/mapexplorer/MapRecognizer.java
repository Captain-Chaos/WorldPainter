package org.pepsoft.worldpainter.mapexplorer;

import org.pepsoft.worldpainter.Platform;

import java.io.File;

/**
 * A class which knows how to identify the platform of maps supported by the
 * WorldPainter plugin to which the recognizer belongs.
 *
 * Created by Pepijn on 25-2-2017.
 */
public interface MapRecognizer {
    /**
     * Identify the platform of the map in the specified directory, if it is a
     * map supported by the plugin to which this recognizer belongs.
     *
     * @param dir The directory to identify.
     * @return The platform of the specified map if this class could identify
     * it, or {@code null} if it was not recognised.
     */
    Platform identifyPlatform(File dir);

    /**
     * Returns a {@link Node} representing the contents of the specified map in
     * so that they can be inspected using the
     * {@link org.pepsoft.worldpainter.mapexplorer.MapExplorer} tool. Will only
     * be invoked for paths for which {@link #identifyPlatform(File)} returns a
     * value.
     *
     * @param mapDir The directory for which to return a node.
     * @return A {@link Node} representing the specified map.
     */
    Node getMapNode(File mapDir);
}
