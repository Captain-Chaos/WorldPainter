package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.ExportSettingsEditor;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;

import java.io.File;

/**
 * A support provider for a WorldPainter {@link Platform}.
 *
 * Created by Pepijn on 12-2-2017.
 */
public interface PlatformProvider extends Provider<Platform> {
    /**
     * Obtain a {@link WorldExporter} for the platform currently configured in
     * the specified world.
     *
     * @param world2 The world to export.
     * @return A world exporter which will export the specified world.
     */
    WorldExporter getExporter(World2 world2);

    /**
     * Get the default directory to select on the Export screen for a
     * platform supported by this provider.
     *
     * @param platform The platform for which to provide the default export
     *                 directory.
     * @return The default export directory for the specified platform.
     */
    File getDefaultExportDir(Platform platform);

    /**
     * Obtain a {@link MapRecognizer} which can recognize directories as
     * containing maps corresponding to any platform supported by this
     * provider.
     *
     * @return A {@link MapRecognizer} which can recognize directories as
     * containing maps corresponding to any platform supported by this
     * plugin.
     */
    MapRecognizer getMapRecognizer();
    
    /**
     * Get the default {@link ExportSettings} for this platform, or {@code null} if the platform has no export settings.
     * 
     * <p>The default implementation returns {@code null}.
     * 
     * @return The default {@link ExportSettings} for this platform, or {@code null} if the platform has no export
     * settings.
     */
    default ExportSettings getDefaultExportSettings() {
        return null;
    }
    
    /**
     * Get an instance of an {@link ExportSettingsEditor} suitable for editing an {@link ExportSettings} object as
     * returned by {@link #getDefaultExportSettings()}. Will only be invoked if {@link #getDefaultExportSettings()} does
     * not return {@code null}.
     * 
     * <p>The default implementation throws an {@link UnsupportedOperationException}.
     * 
     * @return An instance of an {@link ExportSettingsEditor}.
     */
    default ExportSettingsEditor getExportSettingsEditor() {
        throw new UnsupportedOperationException("This platform has no export settings");
    }
}