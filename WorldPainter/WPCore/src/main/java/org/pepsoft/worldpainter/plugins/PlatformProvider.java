package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.ExportSettingsEditor;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

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
     * @param world          The world to export.
     * @param exportSettings The export settings to use for this export. If this is {@code null} then the export
     *                       settings stored on the {@code world} object will be used, if any.
     * @return A world exporter which will export the specified world with the specified settings.
     */
    WorldExporter getExporter(World2 world, WorldExportSettings exportSettings);

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
     * Indicate into which directory the existing map, if any, would be backed up if it were being exported into
     * {@code exportDir}, but do <em>not</em> create it if it does not exist.
     *
     * @param exportDir The directory into which the world would be going to be exported.
     * @return The directory to which the existing map at that location, if any, should be backed up, or {@code null} if
     * this is not supported or it cannot be determined.
     * @throws IOException If an I/O error occurs while determining the backup directory.
     */
    default File selectBackupDir(File exportDir) throws IOException {
        return null;
    }

    /**
     * @deprecated Will never be called. Do not implement. The default implementation throws an
     * {@link UnsupportedOperationException}.
     */
    @Deprecated
    default MapRecognizer getMapRecognizer() {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * Identify the map in the specified directory, if it is a map supported by the plugin to which this provider
     * belongs, and provide some identifying information if so.
     *
     * <p>The default implementation returns {@code null}. This will make it harder to select maps of the type(s)
     * supported by this platform provider for users when they wish to Import a map. If the platform provider does not
     * support Importing this is not relevant.
     *
     * @param dir The directory to identify.
     * @return Identifying information of the specified map if this class could identify it, or {@code null} if it was
     * not recognised.
     */
    default MapInfo identifyMap(File dir) {
        return null;
    }

    class MapInfo {
        public MapInfo(File dir, Platform platform, String name, Icon icon, int minHeight, int maxHeight) {
            this.dir = dir;
            this.platform = platform;
            this.name = name;
            this.icon = icon;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }

        @Deprecated
        public MapInfo(File dir, Platform platform, String name, Icon icon, int maxHeight) {
            this(dir, platform, name, icon, platform.minZ, maxHeight);
        }

        public File dir;
        public Platform platform;
        public String name;
        public Icon icon;
        public int minHeight, maxHeight;
    }
    
    /**
     * Get the default {@link ExportSettings} for a supported platform, or {@code null} if the platform has no export
     * settings.
     * 
     * <p>The default implementation returns {@code null}.
     *
     * @param platform The platform for which to provide the default export settings.
     * @return The default {@link ExportSettings} for the specified platform, or {@code null} if the platform has no
     * export settings.
     */
    default ExportSettings getDefaultExportSettings(Platform platform) {
        return null;
    }
    
    /**
     * Get an instance of an {@link ExportSettingsEditor} suitable for editing an {@link ExportSettings} object as
     * returned by {@link #getDefaultExportSettings(Platform)}. Will only be invoked if
     * {@link #getDefaultExportSettings(Platform)} does not return {@code null}.
     * 
     * <p>The default implementation throws an {@link UnsupportedOperationException}.
     *
     * @param platform The platform for which to provide the export settings editor.
     * @return An instance of an {@link ExportSettingsEditor}.
     */
    default ExportSettingsEditor getExportSettingsEditor(Platform platform) {
        throw new UnsupportedOperationException("This platform has no export settings");
    }

    /**
     * Determines whether a world could be retargeted to a platform supported by this provider without requiring any
     * changes or edits.
     *
     * <p>The default implementation just returns {@link Platform#isCompatible(World2) platform.isCompatible(world)}.
     *
     * @param platform The platform with which to check compatibility.
     * @param world    The world to check for compatibility.
     * @return {@code null} if the world could be trivially retargeted to the specified platform, or a short description
     * of the reason if it cannot.
     */
    default String isCompatible(Platform platform, World2 world) {
        return platform.isCompatible(world);
    }
}