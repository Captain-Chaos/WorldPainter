package org.pepsoft.worldpainter.exporting;

import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.World2;

import java.awt.*;
import java.io.File;

/**
 * A {@link World2} exporting service.
 */
public interface ExportService {
    /**
     * Whether the world is exported to local storage. This implies that the Export dialog should ask for an export
     * directory, clean up backups, etc.
     */
    boolean isLocalStorage();

    /**
     * If {@link #isLocalStorage()} returns false, the text to display in the export directory selection field instead.
     */
    String getDirectoryReplacementText();

    /**
     * Do the export.
     *
     * @return Whether the export was not cancelled. If this is {@code false} the Export dialog will close itself as
     * cancelled rather than successful.
     */
    boolean doExport(Window parent, TiledImageViewer view, World2 world, File baseDir, String name);
}
