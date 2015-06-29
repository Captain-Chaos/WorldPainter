/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.*;

/**
 *
 * @author pepijn
 */
public class BiomesTileProvider implements TileProvider {
    public BiomesTileProvider(BiomeScheme biomeScheme, ColourScheme colourScheme) {
        this(biomeScheme, colourScheme, 2, false);
    }

    public BiomesTileProvider(BiomeScheme biomeScheme, ColourScheme colourScheme, int zoom, boolean fade) {
        this.biomeScheme = biomeScheme;
        this.zoom = zoom;
        int biomeCount = biomeScheme.getBiomeCount();
        biomeColours = new int[biomeCount];
        biomePatterns = new boolean[biomeCount][][];
        for (int i = 0; i < biomeCount; i++) {
            if (biomeScheme.isBiomePresent(i)) {
                biomeColours[i] = 0xff000000 | (fade ? ColourUtils.mix(0xffffff, biomeScheme.getColour(i, colourScheme)) : biomeScheme.getColour(i, colourScheme));
                biomePatterns[i] = biomeScheme.getPattern(i);
            }
        }
        patternColour = fade ? 0xff808080 : 0xff000000;
    }
    
    @Override
    public int getTileSize() {
        return TILE_SIZE;
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
            if (zoom > 0) {
                throw new UnsupportedOperationException("Zooming in not supported");
            }
            this.zoom = zoom;
            bufferRef = new ThreadLocal<>();
        }
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        return true;
    }

    @Override
    public void paintTile(Image image, int tileX, int tileY, int imageX, int imageY) {
        try {
            BufferedImage tile = renderBufferRef.get();
            final int scale = 1 << -zoom;
            int[] buffer = bufferRef.get();
            if (buffer == null) {
                buffer = new int[TILE_SIZE * TILE_SIZE * scale * scale];
                bufferRef.set(buffer);
            }
            biomeScheme.getBiomes(tileX * TILE_SIZE * scale, tileY * TILE_SIZE * scale, TILE_SIZE * scale, TILE_SIZE * scale, buffer);
            int[][] biomeCounts = biomeCountsRef.get();
            if (biomeCounts == null) {
                biomeCounts = new int[][] {new int[256], new int[256], new int[256]};
                biomeCountsRef.set(biomeCounts);
            }
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < biomeCounts[i].length; j++) {
                            biomeCounts[i][j] = 0;
                        }
                    }
                    for (int dx = 0; dx < scale; dx++) {
                        for (int dz = 0; dz < scale; dz++) {
                            int biome = buffer[x * scale + dx + (y * scale + dz) * TILE_SIZE * scale];
                            biomeCounts[BIOME_PRIORITIES[biome]][biome]++;
                        }
                    }
                    int mostCommonBiome = -1;
                    for (int i = 2; i >= 0; i--) {
                        int mostCommonBiomeCount = 0;
                        for (int j = 0; j < biomeCounts[i].length; j++) {
                            if (biomeCounts[i][j] > mostCommonBiomeCount) {
                                mostCommonBiome = j;
                                mostCommonBiomeCount = biomeCounts[i][j];
                            }
                        }
                        if (mostCommonBiome != -1) {
                            break;
                        }
                    }
                    if ((biomePatterns[mostCommonBiome] != null) && (biomePatterns[mostCommonBiome][x % 16][y % 16])) {
                        tile.setRGB(x, y, patternColour);
                    } else {
                        tile.setRGB(x, y, biomeColours[mostCommonBiome]);
                    }
                }
            }
            Graphics2D g2 = (Graphics2D) image.getGraphics();
            try {
                g2.drawImage(tile, imageX, imageY, null);
            } finally {
                g2.dispose();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public int getTilePriority(int x, int y) {
        return 0; // All tiles have equal priority
    }

    @Override
    public Rectangle getExtent() {
        return null;
    }

    @Override
    public void addTileListener(TileListener tileListener) {
        // Do nothing (tiles never change)
    }

    @Override
    public void removeTileListener(TileListener tileListener) {
        // Do nothing (tiles never change)
    }
    
    private final BiomeScheme biomeScheme;
    private final ThreadLocal<int[][]> biomeCountsRef = new ThreadLocal<>();
    private int zoom;
    private volatile ThreadLocal<int[]> bufferRef = new ThreadLocal<>();
    
    private final int[] biomeColours;
    private final int patternColour;
    private final boolean[][][] biomePatterns;

    private static final ThreadLocal<BufferedImage> renderBufferRef = new ThreadLocal<BufferedImage>() {
        @Override
        protected BufferedImage initialValue() {
            return new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        }
    };
    private static final int[] BIOME_PRIORITIES = new int[256];
    static {
        // Every biome should have medium priority except the exceptions below
        Arrays.fill(BIOME_PRIORITIES, 1);
        
        // Ocean should have low priority:
        BIOME_PRIORITIES[BIOME_OCEAN] = 0;
        BIOME_PRIORITIES[BIOME_FROZEN_OCEAN] = 0;
        BIOME_PRIORITIES[BIOME_DEEP_OCEAN] = 0;

        // River should have high priority
        BIOME_PRIORITIES[BIOME_RIVER] = 2;
        BIOME_PRIORITIES[BIOME_FROZEN_RIVER] = 2;
    }
}
