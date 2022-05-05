package org.pepsoft.minecraft;

import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.util.swing.TiledImageViewer.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.UNKNOWN_MATERIAL_COLOUR;

/**
 * Created by Pepijn Schmitz on 27-10-16.
 */
public class MinecraftMapTileProvider implements TileProvider {
    public MinecraftMapTileProvider(File mapDir) throws IOException {

        // Read the metadata
        JavaLevel level = JavaLevel.load(new File(mapDir, "level.dat"));
        maxHeight = level.getMaxHeight();
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyPlatform(mapDir);

        // Open the map
        chunkStore = platformManager.getChunkStore(platform, mapDir, DIM_NORMAL);

        // Scan the region files to determine a rough extent
        Set<MinecraftCoords> chunkCoords = chunkStore.getChunkCoords();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (MinecraftCoords coords: chunkCoords) {
            int tileX = coords.x >> 3;
            int tileY = coords.z >> 3 ;
            presentTiles.add(new Point(tileX, tileY));
            if (tileX < minX) {
                minX = tileX;
            }
            if (tileX > maxX) {
                maxX = tileX;
            }
            if (tileY < minY) {
                minY = tileY;
            }
            if (tileY > maxY) {
                maxY = tileY;
            }
        }
        extent = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);

        colourScheme = ColourScheme.DEFAULT;
    }

    @Override
    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        return presentTiles.contains(new Point(x, y));
    }

    @Override
    public boolean paintTile(Image tileImage, int x, int y, int dx, int dy) {
        final BufferedImage image = renderBufferRef.get();
        final int chunkX1 = x * 8, chunkY1 = y * 8;
        final int chunkX2 = chunkX1 + 8 - 1, chunkY2 = chunkY1 + 8 - 1;
        final Font font = Font.decode("Dialog-9");
        for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
            for (int chunkY = chunkY1; chunkY <= chunkY2; chunkY++) {
                Chunk chunk = chunkStore.getChunk(chunkX, chunkY);
                if (chunk != null) {
                    String status = null;
                    if (chunk instanceof MC115AnvilChunk) {
                        status = ((MC115AnvilChunk) chunk).status;
                    } else if (chunk instanceof MC118AnvilChunk) {
                        status = ((MC118AnvilChunk) chunk).status;
                    }
                    for (int blockX = 0; blockX < 16; blockX++) {
                        for (int blockY = 0; blockY < 16; blockY++) {
                            image.setRGB(chunkX - chunkX1 << 4 | blockX, chunkY - chunkY1 << 4 | blockY, 0xff000000 | getColour(chunk, blockX, blockY));
                        }
                    }
                    if (status != null) {
                        String symbol;
                        synchronized (statusSymbols) {
                            symbol = statusSymbols.get(status);
                            if (symbol == null) {
                                if (status.length() <= 4) {
                                    symbol = status;
                                } else {
                                    final String[] parts = status.split("_+");
                                    final int partLength;
                                    if (parts.length == 1) {
                                        partLength = 4;
                                    } else if (parts.length == 2) {
                                        partLength = 2;
                                    } else {
                                        partLength = 1;
                                    }
                                    symbol = stream(parts).map(part -> part.substring(0, 1).toUpperCase() + part.substring(1, partLength)).collect(joining());
                                }
                                statusSymbols.put(status, symbol);
                                logger.info("Status symbols: {}", statusSymbols);
                            }
                        }
                        Graphics2D g2 = image.createGraphics();
                        try {
                            g2.setFont(font);
                            g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
                            if (symbol.length() > 2) {
                                g2.drawString(symbol.substring(0, 2), (chunkX - chunkX1 << 4) + 1, (chunkY - chunkY1 << 4) + 7);
                                g2.drawString(symbol.substring(2), (chunkX - chunkX1 << 4) + 1, (chunkY - chunkY1 << 4) + 15);
                            } else {
                                g2.drawString(symbol, (chunkX - chunkX1 << 4) + 1, (chunkY - chunkY1 << 4) + 7);
                            }
                        } finally {
                            g2.dispose();
                        }
                    }
                } else {
                    for (int blockX = 0; blockX < 16; blockX++) {
                        for (int blockY = 0; blockY < 16; blockY++) {
                            image.setRGB(chunkX - chunkX1 << 4 | blockX, chunkY - chunkY1 << 4 | blockY, 0);
                        }
                    }
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
        return false;
    }

    @Override
    public int getZoom() {
        return 0;
    }

    @Override
    public void setZoom(int zoom) {
        if (zoom != 0) {
            throw new UnsupportedOperationException();
        }
    }

    private int getColour(Chunk chunk, int x, int y) {
        final int minHeight = chunk.getMinHeight();
        for (int z = maxHeight - 1; z >= minHeight; z--) {
            final Material material = chunk.getMaterial(x, z, y);
            if (material != AIR) {
                final int colour = colourScheme.getColour(material);
                if ((colour == UNKNOWN_MATERIAL_COLOUR) && (! reportedMaterials.contains(material))){
                    logger.warn("Don't know colour of material {}", material);
                    reportedMaterials.add(material);
                }
                return colour;
            }
        }
        return DEFAULT_VOID_COLOUR;
    }

    private final ChunkStore chunkStore;
    private final int maxHeight;
    private final ColourScheme colourScheme;
    private final List<TileListener> listeners = new ArrayList<>();
    private final Rectangle extent;
    private final Set<Point> presentTiles = new HashSet<>();
    private final Map<String, String> statusSymbols = new HashMap<>();
    private final Set<Material> reportedMaterials = new HashSet<>();

    private static final int DEFAULT_VOID_COLOUR = 0x00FFFF;
    private static final ThreadLocal<BufferedImage> renderBufferRef = ThreadLocal.withInitial(() -> new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
    private static final Logger logger = LoggerFactory.getLogger(MinecraftMapTileProvider.class);
}