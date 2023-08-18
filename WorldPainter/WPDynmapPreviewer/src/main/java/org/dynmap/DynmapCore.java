package org.dynmap;

import org.dynmap.common.DynmapServerInterface;
import org.pepsoft.util.mdc.MDCWrappingRuntimeException;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.dynmap.WPDynmapServer;

import java.io.File;
import java.io.IOException;

/**
 * Private implementation of {@code DynmapCore} from dynmap in order to work offline.
 *
 * Created by Pepijn Schmitz on 01-09-15.
 */
public class DynmapCore {
    public File getDataFolder() {
        return new File(Configuration.getConfigDir(), "dynmap");
    }

    public DynmapServerInterface getServer() {
        return server;
    }

    public MapManager getMapManager() {
        return MapManager.mapman;
    }

    public boolean isCTMSupportEnabled() {
        return false;
    }

    public boolean isCustomColorsSupportEnabled() {
        return false;
    }

    public String getDynmapPluginPlatformVersion() {
        return "1.20.1"; // TODO what should this be, exactly? It seems to control which blocks Dynmap support, so ideally it should keep track with the latest version supported by WorldPainter?
    }

    public boolean getLeafTransparency() {
        return true;
    }

    public File getPluginJarFile() {
        // Dynmap doesn't actually load the models or textures from this file, it seems to be able to find everything on
        // the class path, but it still needs it to exist. It does catch and ignore IOExceptions though, so create an
        // empty dummy file and return that so it will immediately fail.
        try {
            final File tmpFile = File.createTempFile("wp-empty", "zip");
            tmpFile.deleteOnExit();
            return tmpFile;
        } catch (IOException e) {
            throw new MDCWrappingRuntimeException(e);
        }
    }

    public boolean dumpMissingBlocks() {
        return false;
    }

    private final WPDynmapServer server = new WPDynmapServer();

    public static final DynmapCore INSTANCE = new DynmapCore();

    // Copied from dynmap
    public enum CompassMode {
        PRE19,  /* Default for 1.8 and earlier (east is Z+) */
        NEWROSE,    /* Use same map orientation, fix rose */
        NEWNORTH    /* Use new map orientation */
    }
}