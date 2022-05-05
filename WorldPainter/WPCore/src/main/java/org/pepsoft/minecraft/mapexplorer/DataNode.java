package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.Node;

/**
 * A node which contains binary data.
 */
public interface DataNode extends Node {
    byte[] getData();
}