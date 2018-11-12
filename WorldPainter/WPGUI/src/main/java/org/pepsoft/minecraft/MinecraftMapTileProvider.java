package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 * Created by Pepijn Schmitz on 27-10-16.
 */
public class MinecraftMapTileProvider implements TileProvider {
    public MinecraftMapTileProvider(File mapDir) throws IOException {
        this.mapDir = mapDir;

        // Read the metadata
        Level level = Level.load(new File(mapDir, "level.dat"));
        maxHeight = level.getMaxHeight();
        version = level.getVersion();
        dataVersion = level.getDataVersion();
        if (version == VERSION_MCREGION) {
            platform = JAVA_MCREGION;
        } else if (dataVersion <= DATA_VERSION_MC_1_12_2) {
            platform = JAVA_ANVIL;
        } else {
            platform = JAVA_ANVIL_1_13;
        }

        // Scan the region files to determine a rough extent
        File regionDir = new File(mapDir, "region");
        Pattern regionFilePattern = (version == VERSION_MCREGION)
            ? Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mcr")
            : Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
        File[] regionFiles = regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
        if ((regionFiles != null) && (regionFiles.length > 0)) {
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (File file: regionFiles) {
                Matcher matcher = regionFilePattern.matcher(file.getName());
                matcher.matches();
                int x = Integer.parseInt(matcher.group(1));
                int z = Integer.parseInt(matcher.group(2));
                MinecraftMapTileProvider.this.fileCache.put(new Point(x, z), file);
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (z < minZ) {
                    minZ = z;
                }
                if (z > maxZ) {
                    maxZ = z;
                }
            }
            extent = new Rectangle(minX << 2, minZ << 2, (maxX - minX + 1) << 2, (maxZ - minZ + 1) << 2);
        } else {
            extent = null;
        }

        colourScheme = new DynMapColourScheme("default", true);
    }

    @Override
    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        if (zoom == 0) {
            Point regionCoords = new Point(x >> 2, y >> 2);
            return fileCache.containsKey(regionCoords);
        } else {
            return true;
        }
    }

    @Override
    public boolean paintTile(Image tileImage, int x, int y, int dx, int dy) {
        final BufferedImage image = renderBufferRef.get();
        int scale = MathUtils.pow(2, -zoom);
        final int chunkX1 = x * 8 * scale, chunkY1 = y * 8 * scale;
        final int chunkX2 = chunkX1 + 8 * scale - 1, chunkY2 = chunkY1 + 8 * scale - 1;
        int previousRegionX = Integer.MIN_VALUE, previousRegionY = Integer.MIN_VALUE;
        RegionFile previousRegion = null;
        final int step = Math.max(scale / 16, 1);
        final Font font = Font.decode("Dialog-8");
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
                    DataInputStream dataIn = region.getChunkDataInputStream(chunkX & 0x1f, chunkY & 0x1f);
                    if (dataIn != null) {
                        MC113AnvilChunk.Status status = null;
                        Chunk chunk;
                        try (NBTInputStream in = new NBTInputStream(dataIn)) {
                            if ((platform == JAVA_MCREGION)) {
                                chunk = new MCRegionChunk((CompoundTag) in.readTag(), maxHeight);
                            } else if ((platform == JAVA_ANVIL)) {
                                chunk = new MC12AnvilChunk((CompoundTag) in.readTag(), maxHeight);
                            } else {
                                chunk = new MC113AnvilChunk((CompoundTag) in.readTag(), maxHeight);
                                status = ((MC113AnvilChunk) chunk).status;
                            }
                        }
                        for (int blockX = 0; blockX < 16; blockX += scale) {
                            for (int blockY = 0; blockY < 16; blockY += scale) {
                                image.setRGB((((chunkX - chunkX1) << 4) | blockX) / scale, (((chunkY - chunkY1) << 4) | blockY) / scale, 0xff000000 | getColour(chunk, blockX, blockY));
                            }
                        }
                        if (status != null) {
                            Graphics2D g2 = image.createGraphics();
                            try {
                                g2.setFont(font);
                                g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
                                g2.drawString(status.name(), (((chunkX - chunkX1) << 4) + 1) / scale, (((chunkY - chunkY1) << 4) + 15) / scale);
                            } finally {
                                g2.dispose();
                            }
                        }
                    } else {
                        for (int blockX = 0; blockX < 16; blockX += scale) {
                            for (int blockY = 0; blockY < 16; blockY += scale) {
                                image.setRGB((((chunkX - chunkX1) << 4) | blockX) / scale, (((chunkY - chunkY1) << 4) | blockY) / scale, 0);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while reading chunk data", e);
                }
            }
        }
        Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
        try {
            g2.setComposite(AlphaComposite.Src);
            g2.drawImage(image, dx, dy, null);
        } finally {
            g2.dispose();
        }
        return true;
    }

    @Override
    public int getTilePriority(int x, int y) {
        return 0; // All tiles have equal priority
    }

    @Override
    public Rectangle getExtent() {
        return extent;
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
            this.zoom = zoom;
        }
    }

    private synchronized RegionFile getRegionFile(int x, int y) throws IOException {
        Point coords = new Point(x, y);
        RegionFile regionFile = regionFileCache.get(coords);
        if (regionFile == null) {
            if (fileCache.containsKey(coords)) {
                regionFile = new RegionFile(fileCache.get(coords), true);
            } else {
                regionFile = NULL;
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
        for (int z = maxHeight - 1; z >= 0; z--) {
            Material material = chunk.getMaterial(x, z, y);
            if (material != AIR) {
                return colourScheme.getColour(material);
            }
        }
        return DEFAULT_VOID_COLOUR;
    }

    private final File mapDir;
    private final Platform platform;
    private final int maxHeight, version, dataVersion;
    private final ColourScheme colourScheme;
    private final List<TileListener> listeners = new ArrayList<>();
    private final Map<Point, File> fileCache = new HashMap<>();
    private final Map<Point, RegionFile> regionFileCache = new HashMap<>();
    private final Rectangle extent;
    private int zoom = 0;

    private static final int DEFAULT_VOID_COLOUR = 0x00FFFF;
    private static final RegionFile NULL = new RegionFile();
    private static final ThreadLocal<BufferedImage> renderBufferRef = ThreadLocal.withInitial(() -> new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
}