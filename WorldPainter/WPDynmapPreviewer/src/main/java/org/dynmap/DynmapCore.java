package org.dynmap;

import org.dynmap.common.DynmapServerInterface;
import org.pepsoft.worldpainter.dynmap.WPDynmapServer;

import java.io.File;

/**
 * Private implementation of <code>DynmapCore</code> from dynmap in order to work offline.
 *
 * Created by Pepijn Schmitz on 01-09-15.
 */
public class DynmapCore {
    public File getDataFolder() {
        return new File("D:\\private\\workspace\\DynmapCore-master\\src\\main\\resources");
    }

    public DynmapServerInterface getServer() {
        return server;
    }

    public boolean isCTMSupportEnabled() {
        return false;
    }

    public boolean isCustomColorsSupportEnabled() {
        return false;
    }

    public String getDynmapPluginPlatformVersion() {
        return "1.8";
    }

    public boolean getLeafTransparency() {
        return true;
    }

    public File getPluginJarFile() {
        return new File("D:\\private\\workspace\\DynmapCore-master\\target\\DynmapCore-2.2-SNAPSHOT.jar");
    }

    public boolean dumpMissingBlocks() {
        return true;
    }

    private WPDynmapServer server = new WPDynmapServer();

    // Copied from dynmap
    public enum CompassMode {
        PRE19,  /* Default for 1.8 and earlier (east is Z+) */
        NEWROSE,    /* Use same map orientation, fix rose */
        NEWNORTH    /* Use new map orientation */
    }
}