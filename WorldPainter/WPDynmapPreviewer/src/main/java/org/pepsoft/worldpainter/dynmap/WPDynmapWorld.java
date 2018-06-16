package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A {@link DynmapWorld} implementation which wraps a {@link MinecraftWorld} for
 * use with the dynmap API.
 *
 * <p>Use the static factory methods to obtain correctly configured instances
 * for various kinds of <code>MinecraftWorld</code>
 *
 * <p>Created by Pepijn Schmitz on 05-06-15.
 */
public class WPDynmapWorld extends DynmapWorld {
    private WPDynmapWorld(MinecraftWorld world, String name, int waterLevel) {
        super(name, world.getMaxHeight(), waterLevel);
        this.world = world;
        chunkCache = new WPMapChunkCache(this, world);
    }

    @Override
    public boolean isNether() {
        return dim == Constants.DIM_NETHER || dim == Constants.DIM_NETHER_CEILING;
    }

    @Override
    public DynmapLocation getSpawnLocation() {
        return spawnLocation;
    }

    @Override
    public long getTime() {
        return 10000; // Noon
    }

    @Override
    public boolean hasStorm() {
        return false;
    }

    @Override
    public boolean isThundering() {
        return false;
    }

    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public void setWorldUnloaded() {
        // Do nothing
    }

    @Override
    public int getLightLevel(int x, int y, int z) {
        return world.getBlockLightLevel(x, z, y);
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        return world.getHighestNonAirBlock(x, z);
    }

    @Override
    public boolean canGetSkyLightLevel() {
        return true;
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        return world.getSkyLightLevel(x, z, y);
    }

    @Override
    public String getEnvironment() {
        switch (dim) {
            case Constants.DIM_NORMAL:
            case Constants.DIM_NORMAL_CEILING:
                return "normal";
            case Constants.DIM_END:
            case Constants.DIM_END_CEILING:
                return "the_end";
            case Constants.DIM_NETHER:
            case Constants.DIM_NETHER_CEILING:
                return "nether";
            default:
                throw new IllegalArgumentException("Dimension " + dim + " not supported");
        }
    }

    @Override
    public MapChunkCache getChunkCache(List<DynmapChunk> chunks) {
        return chunkCache;
    }

    public static WPDynmapWorld forDimension(MinecraftWorld minecraftWorld, Dimension dimension) {
        int waterLevel;
        TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            waterLevel = ((HeightMapTileFactory) tileFactory).getWaterHeight();
        } else {
            waterLevel = 62;
        }
        World2 wpWorld = dimension.getWorld();
        Point spawnPoint = wpWorld.getSpawnPoint();
        return forMinecraftWorld(minecraftWorld, wpWorld.getName() + " - " + dimension.getName(), dimension.getDim(), waterLevel, new Point3i(spawnPoint.x, spawnPoint.y, dimension.getIntHeightAt(spawnPoint.x, spawnPoint.y)));
    }

    public static WPDynmapWorld forMinecraftMap(File worldDir, int dim) throws IOException {
        File levelDatFile = new File(worldDir, "level.dat");
        Level level = Level.load(levelDatFile);
        return forMinecraftWorld(new JavaMinecraftWorld(worldDir, dim, level.getMaxHeight(), level.getVersion() == org.pepsoft.minecraft.Constants.VERSION_MCREGION ? DefaultPlugin.JAVA_MCREGION : DefaultPlugin.JAVA_ANVIL, true, 256), level.getName(), dim, 62, new Point3i(level.getSpawnX(), level.getSpawnZ(), level.getSpawnY()));
    }

    public static WPDynmapWorld forMinecraftWorld(MinecraftWorld minecraftWorld, String name, int dim, int waterLevel, Point3i spawnPoint) {
        WPDynmapWorld world = new WPDynmapWorld(minecraftWorld, name, waterLevel);
        world.dim = dim;
        if (spawnPoint != null) {
            world.spawnLocation = new DynmapLocation(name, spawnPoint.x, spawnPoint.z, spawnPoint.y);
        }
        return world;
    }

    private final MinecraftWorld world;
    private final WPMapChunkCache chunkCache;
    private int dim;
    private DynmapLocation spawnLocation;
}