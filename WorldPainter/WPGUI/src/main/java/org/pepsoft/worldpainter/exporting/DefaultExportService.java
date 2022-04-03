package org.pepsoft.worldpainter.exporting;

import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.ExportProgressDialog;
import org.pepsoft.worldpainter.World2;

import java.awt.*;
import java.io.File;

public class DefaultExportService implements ExportService {
    @Override
    public boolean isLocalStorage() {
        return false;
    }

    @Override
    public String getDirectoryReplacementText() {
        return null;
    }

    @Override
    public boolean doExport(Window parent, TiledImageViewer view, World2 world, File baseDir, String name) {
        ExportProgressDialog dialog = new ExportProgressDialog(parent, world, baseDir, name);
        view.setInhibitUpdates(true);
        try {
            dialog.setVisible(true);
        } finally {
            view.setInhibitUpdates(false);
        }
        return true;
    }

    public static final DefaultExportService INSTANCE = new DefaultExportService();
}
