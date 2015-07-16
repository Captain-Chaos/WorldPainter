/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class MapViewer {
    public static void main(String[] args) throws IOException {
        File mySavesDir = null;
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        if (minecraftDir != null) {
            mySavesDir = new File(minecraftDir, "saves");
        }
        File levelDatFile = FileUtils.selectFileForOpen(null, "Select Minecraft map level.dat file", mySavesDir, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equalsIgnoreCase("level.dat");
            }

            @Override
            public String getDescription() {
                return "Minecraft levels (level.dat)";
            }
        });
        if (levelDatFile != null) {
            final File worldDir = levelDatFile.getParentFile();
            Level level = Level.load(levelDatFile);
            final int maxHeight = level.getMaxHeight();
            final int version = level.getVersion();
            final ColourScheme colourScheme = new DynMapColourScheme("default", true);
            TileProvider tileProvider = new TileProvider() {
                @Override
                public int getTileSize() {
                    return TILE_SIZE;
                }

                @Override
                public boolean isTilePresent(int x, int y) {
                    return true;
                }

                @Override
                public void paintTile(Image tileImage, int x, int y, int dx, int dy) {
    //                System.out.println("Painting tile " + x + ", " + y);
                    final BufferedImage image = renderBufferRef.get();
                    final int chunkX1 = x * 8 * zoom, chunkY1 = y * 8 * zoom;
                    final int chunkX2 = chunkX1 + 8 * zoom - 1, chunkY2 = chunkY1 + 8 * zoom - 1;
    //                System.out.println("Chunk coords: " + chunkX1 + ", " + chunkY1 + " -> " + chunkX2 + ", " + chunkY2);
                    int previousRegionX = Integer.MIN_VALUE, previousRegionY = Integer.MIN_VALUE;
                    RegionFile previousRegion = null;
                    final int step = Math.max(zoom / 16, 1);
                    for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX += step) {
                        for (int chunkY = chunkY1; chunkY <= chunkY2; chunkY += step) {
                            try {
                                int regionX = chunkX >> 5, regionY = chunkY >> 5;
                                RegionFile region;
                                if ((regionX != previousRegionX) || (regionY != previousRegionY)) {
                                    region = getRegionFile(regionX, regionY);
                                    previousRegion = region;
                                    previousRegionX = regionX;
                                    previousRegionY = regionY;
                                } else {
                                    region = previousRegion;
                                }
                                if (region == null) {
                                    continue;
                                }
    //                            System.out.println("Chunk coords: " + chunkX + ", " + chunkY);
    //                            System.out.println("Chunk coords in region: " + (chunkX & 0x1f) + ", " + (chunkY & 0x1f));
                                DataInputStream dataIn = region.getChunkDataInputStream(chunkX & 0x1f, chunkY & 0x1f);
                                if (dataIn != null) {
    //                                System.out.println("    Chunk found");
                                    Chunk chunk;
                                    try (NBTInputStream in = new NBTInputStream(dataIn)) {
                                        chunk = (version == Constants.SUPPORTED_VERSION_2)
                                                ? new ChunkImpl2((CompoundTag) in.readTag(), maxHeight)
                                                : new ChunkImpl((CompoundTag) in.readTag(), maxHeight);
                                    }
                                    for (int blockX = 0; blockX < 16; blockX += zoom) {
                                        for (int blockY = 0; blockY < 16; blockY += zoom) {
                                            image.setRGB((((chunkX - chunkX1) << 4) | blockX) / zoom, (((chunkY - chunkY1) << 4) | blockY) / zoom, getColour(chunk, blockX, blockY));
                                        }
                                    }
    //                            } else {
    //                                System.out.println("    No chunk found");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                    try {
                        g2.drawImage(image, dx, dy, null);
                    } finally {
                        g2.dispose();
                    }
                }

                @Override
                public int getTilePriority(int x, int y) {
                    return 0; // All tiles have equal priority
                }

                @Override
                public Rectangle getExtent() {
                    return null; // We don't know how large the map is, so just say we're endless
                }

                @Override
                public void addTileListener(TileListener tileListener) {
                    listeners.add(tileListener);
                }

                @Override
                public void removeTileListener(TileListener tileListener) {
                    listeners.remove(tileListener);
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
                        if (! ((zoom & (zoom - 1)) == 0)) {
                            throw new IllegalArgumentException("Zoom must be a power of two");
                        }
                        this.zoom = zoom;
                    }
                }

                private synchronized RegionFile getRegionFile(int x, int y) throws IOException {
                    Point coords = new Point(x, y);
                    RegionFile regionFile = regionFileCache.get(coords);
                    if (regionFile == null) {
    //                    System.out.println("Looking for region " + x + ", " + y);
                        regionFile = RegionFileCache.getRegionFileIfExists(worldDir, x << 5, y << 5, version);
                        if (regionFile == null) {
    //                        System.out.println("    Region does not exist");
                            regionFile = NULL;
    //                    } else {
    //                        System.out.println("    Region found");
                        }
                        regionFileCache.put(coords, regionFile);
                    }
                    if (regionFile == NULL) {
                        return null;
                    } else {
                        return regionFile;
                    }
                }

                private int getColour(Chunk chunk, int x, int y) {
    //                System.out.println("        Finding colour for column " + x + ", " + y);
                    for (int z = maxHeight - 1; z >= 0; z--) {
                        int blockType = chunk.getBlockType(x, z, y);
                        if (blockType != Constants.BLK_AIR) {
                            return colourScheme.getColour(blockType, chunk.getDataValue(x, z, y));
                        }
                    }
                    return DEFAULT_VOID_COLOUR;
                }

                private int zoom = 1;
                private final List<TileListener> listeners = new ArrayList<>();
                private final Map<Point, RegionFile> regionFileCache = new HashMap<>();

                private static final int DEFAULT_VOID_COLOUR = 0x00FFFF;
            };

            JFrame frame = new JFrame("Map Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            final TiledImageViewer viewer = new TiledImageViewer(true, Math.max(Runtime.getRuntime().availableProcessors() - 1, 1), true);
            viewer.setTileProvider(tileProvider);
            viewer.addMouseWheelListener(e -> {
                int zoom = viewer.getZoom();
                zoom = (int) Math.max(zoom * Math.pow(2, e.getWheelRotation()), 1);
                System.out.println("Setting zoom to " + zoom);
                viewer.setZoom(zoom);
            });
            frame.getContentPane().add(viewer, BorderLayout.CENTER);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    private static final RegionFile NULL = new RegionFile();
    private static final int TILE_SIZE = 128;
    private static final ThreadLocal<BufferedImage> renderBufferRef = new ThreadLocal<BufferedImage>() {
        @Override
        protected BufferedImage initialValue() {
            return new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        }
    };
}