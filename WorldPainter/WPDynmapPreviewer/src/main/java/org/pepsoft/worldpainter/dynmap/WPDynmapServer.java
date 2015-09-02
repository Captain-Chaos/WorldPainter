package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapChunk;
import org.dynmap.DynmapWorld;
import org.dynmap.common.DynmapListenerManager;
import org.dynmap.common.DynmapPlayer;
import org.dynmap.common.DynmapServerInterface;
import org.dynmap.utils.MapChunkCache;
import org.pepsoft.worldpainter.Version;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Implementation of <code>DynmapServerInterface</code> in support of the
 * private implementations of dynmap classes in this module. This is
 * <em>NOT</em> a general purpose implementation and most methods throw an
 * {@link UnsupportedOperationException}!
 *
 * <p>Created by Pepijn Schmitz on 01-09-15.
 */
public class WPDynmapServer extends DynmapServerInterface {
    @Override
    public void scheduleServerTask(Runnable run, long delay) {
        // Do nothing
    }

    @Override
    public <T> Future<T> callSyncMethod(Callable<T> task) {
        return null;
    }

    @Override
    public DynmapPlayer[] getOnlinePlayers() {
        return new DynmapPlayer[0];
    }

    @Override
    public void reload() {
        // Do nothing
    }

    @Override
    public DynmapPlayer getPlayer(String name) {
        return null;
    }

    @Override
    public DynmapPlayer getOfflinePlayer(String name) {
        return null;
    }

    @Override
    public Set<String> getIPBans() {
        return null;
    }

    @Override
    public String getServerName() {
        return "WorldPainter " + Version.VERSION + " (" + Version.BUILD + ")";
    }

    @Override
    public boolean isPlayerBanned(String pid) {
        return false;
    }

    @Override
    public String stripChatColor(String s) {
        return null;
    }

    @Override
    public boolean requestEventNotification(DynmapListenerManager.EventType type) {
        return false;
    }

    @Override
    public boolean sendWebChatEvent(String source, String name, String msg) {
        return false;
    }

    @Override
    public void broadcastMessage(String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getBiomeIDs() {
        return new String[0];
    }

    @Override
    public double getCacheHitRate() {
        return 0;
    }

    @Override
    public void resetCacheStats() {

    }

    @Override
    public DynmapWorld getWorldByName(String wname) {
        return null;
    }

    @Override
    public Set<String> checkPlayerPermissions(String player, Set<String> perms) {
        return null;
    }

    @Override
    public boolean checkPlayerPermission(String player, String perm) {
        return false;
    }

    @Override
    public MapChunkCache createMapChunkCache(DynmapWorld w, List<DynmapChunk> chunks, boolean blockdata, boolean highesty, boolean biome, boolean rawbiome) {
        return null;
    }

    @Override
    public int getMaxPlayers() {
        return 0;
    }

    @Override
    public int getCurrentPlayers() {
        return 0;
    }

    @Override
    public int getBlockIDAt(String wname, int x, int y, int z) {
        return 0;
    }

    @Override
    public double getServerTPS() {
        return 0;
    }

    @Override
    public String getServerIP() {
        return null;
    }
}
