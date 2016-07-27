package org.pepsoft.worldpainter.dynmap;

import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapWorld;
import org.dynmap.hdmap.HDMap;
import org.dynmap.hdmap.HDMapTile;
import org.pepsoft.util.Box;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.util.swing.TiledImageViewer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TileProvider} for {@link TiledImageViewer} which provides an
 * isometric 3D view of a {@link DynmapWorld} by using dynmap to render the
 * view.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
public class DynMapTileProvider implements TileProvider {
    public DynMapTileProvider(DynmapWorld dmWorld) {
        this.dmWorld = dmWorld;
        refreshMap();
    }

    @Override
    public int getTileSize() {
        return 128;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        return true;
    }

    @Override
    public boolean paintTile(Image image, int x, int y, int dx, int dy) {
        HDMapTile tile = new HDMapTile(dmWorld, map.getPerspective(), x, -y, 0);
        BufferedImage tileImage = rendererRef.get().render(dmWorld.getChunkCache(null), tile);
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        try {
            g2.drawImage(tileImage, dx, dy, null);
        } finally {
            g2.dispose();
        }
        return true;
    }

    @Override
    public int getTilePriority(int x, int y) {
        return 0;
    }

    @Override
    public Rectangle getExtent() {
        return null;
    }

    @Override
    public void addTileListener(TileListener tileListener) {
        // Do nothing
    }

    @Override
    public void removeTileListener(TileListener tileListener) {
        // Do nothing
    }

    @Override
    public boolean isZoomSupported() {
        return true;
    }

    @Override
    public int getZoom() {
        return zoom;
    }

    @Override
    public void setZoom(int zoom) {
        if (zoom != this.zoom) {
            this.zoom = zoom;
            scale = MathUtils.pow(2, 4 + zoom);
            refreshRenderers();
        }
    }

    private void refreshRenderers() {
        rendererRef = new ThreadLocal<DynMapRenderer>() {
            @Override
            protected DynMapRenderer initialValue() {
                return new DynMapRenderer(map.getPerspective(), map, scale, inclination, azimuth);
            }
        };
    }

    private void refreshMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "WorldPainter");
        config.put("image-format", "png");
        if (caves) {
            config.put("shader", "caves");
        }
        ConfigurationNode configNode = new ConfigurationNode(config);
        map = new HDMap(null, configNode);
        refreshRenderers();
    }

    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        if (azimuth != this.azimuth) {
            if (azimuth == 360.0) {
                azimuth = 0.0;
            } else if ((azimuth < 0.0) || (azimuth >= 360.0)) {
                throw new IllegalArgumentException("Azimuth must be >= 0 and < 360");
            }
            this.azimuth = azimuth;
            refreshRenderers();
        }
    }

    public double getInclination() {
        return inclination;
    }

    public void setInclination(double inclination) {
        if (inclination != this.inclination) {
            if ((inclination < 30.0) || (inclination > 90.0)) {
                throw new IllegalArgumentException("Inclination must be >= 30 and <= 90");
            }
            this.inclination = inclination;
            refreshRenderers();
        }
    }

    public boolean isCaves() {
        return caves;
    }

    public void setCaves(boolean caves) {
        if (caves != this.caves) {
            this.caves = caves;
            refreshMap();
        }
    }

    /**
     * Get the bounds (in tiles) of a rectangle which would completely encompass
     * a specific 3D volume of the world in the current projection.
     *
     * @param volume The 3D volume which must be encompassed.
     * @return The rectangle in tile coordinates which will completely encompass
     * the specified volume.
     */
    public Rectangle getBounds(Box volume) {
        Rectangle rect = rendererRef.get().getTileCoords(volume.getX1(), volume.getZ1(), volume.getY1(), volume.getX2() + 1, volume.getZ2() + 1, volume.getY2() + 1);
        rect.setLocation(rect.x, -rect.y - rect.height);
        return rect;
    }

    private final DynmapWorld dmWorld;
    private int zoom;
    private boolean caves;
    private volatile double inclination = 60.0, azimuth = 135.0;
    private volatile int scale = 16;
    private volatile HDMap map;
    private volatile ThreadLocal<DynMapRenderer> rendererRef;
}