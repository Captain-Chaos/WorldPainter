package org.pepsoft.worldpainter.util;

import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.plugins.MapImporterProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import static java.lang.Boolean.FALSE;

/**
 * Utility methods for working with existing maps, such as Minecraft maps.
 */
public final class MapUtils {
    private MapUtils() {
        // Prevent instantiation
    }

    /**
     * Select an existing map supported by the installed platform providers from disk.
     *
     * @param parent     The parent component for the dialog.
     * @param defaultDir The default directory to start the dialog in. May be {@code null}, in which case the default
     *                   saves directory from the configuration will be used, or if that is not set, the Minecraft saves
     *                   directory will attempted to be found.
     * @return The selected map, or {@code null} if the user cancelled the dialog.
     */
    public static PlatformProvider.MapInfo selectMap(Window parent, File defaultDir) {
        File mySavesDir = (defaultDir != null) ? defaultDir : Configuration.getInstance().getSavesDirectory();
        if ((mySavesDir == null) && (MinecraftUtil.findMinecraftDir() != null)) {
            mySavesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
        }
        final PlatformManager platformManager = PlatformManager.getInstance();
        final Map<File, PlatformProvider.MapInfo> mapInfoCache = new Hashtable<>();
        final File selectedFile = FileUtils.selectDirectoryForOpen(parent, "Select an existing map directory", mySavesDir, "Map Directories", new FileView() {
            @Override
            public String getName(File f) {
                return null;
            }

            @Override
            public String getDescription(File f) {
                final PlatformProvider.MapInfo mapInfo = getMapInfo(f, mapInfoCache, platformManager);
                return (mapInfo != NOT_A_MAP) ? mapInfo.name : null;
            }

            @Override
            public String getTypeDescription(File f) {
                final PlatformProvider.MapInfo mapInfo = getMapInfo(f, mapInfoCache, platformManager);
                return (mapInfo != NOT_A_MAP) ? mapInfo.platform.displayName : null;
            }

            @Override
            public Icon getIcon(File f) {
                final PlatformProvider.MapInfo mapInfo = getMapInfo(f, mapInfoCache, platformManager);
                return (mapInfo != NOT_A_MAP) ? mapInfo.icon : null;
            }

            @Override
            public Boolean isTraversable(File f) {
                return (getMapInfo(f, mapInfoCache, platformManager) != NOT_A_MAP) ? FALSE : null;
            }

        });
        if (selectedFile != null) {
            final PlatformProvider.MapInfo mapInfo = getMapInfo(selectedFile, mapInfoCache, platformManager);
            return (mapInfo != NOT_A_MAP) ? mapInfo : null;
        } else {
            return null;
        }
    }

    static PlatformProvider.MapInfo getMapInfo(File dir, Map<File, PlatformProvider.MapInfo> mapInfoCache, PlatformManager platformManager) {
        return mapInfoCache.computeIfAbsent(dir, key -> {
            PlatformProvider.MapInfo mapInfo = platformManager.identifyMap(dir);
            if (mapInfo != null) {
                PlatformProvider platformProvider = platformManager.getPlatformProvider(mapInfo.platform);
                if (platformProvider instanceof MapImporterProvider) {
                    return mapInfo;
                }
            }
            return NOT_A_MAP;
        });
    }

    static final PlatformProvider.MapInfo NOT_A_MAP = new PlatformProvider.MapInfo(null, null, null, null, -1);
}