/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.JavaLevel;
import org.pepsoft.worldpainter.AbstractMain;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class Statistics extends AbstractMain {
    @SuppressWarnings("unchecked") // Java limitation
    public static void main(String[] args) throws IOException {
        initialisePlatform();

        final File worldDir = new File(args[0]);
        final int dim = Integer.parseInt(args[1]);
        final File levelDatFile = new File(worldDir, "level.dat");
        final JavaLevel level = JavaLevel.load(levelDatFile);
        if ((level.getVersion() != VERSION_MCREGION) && (level.getVersion() != VERSION_ANVIL)) {
            throw new UnsupportedOperationException("Level format version " + level.getVersion() + " not supported");
        }
        final int maxHeight = level.getMaxHeight();
        final int maxY = maxHeight - 1;

//        int totalBlockCount = 0, totalBlocksPerLevel = 0;
        System.out.println("Scanning " + worldDir);
        final PlatformManager platformManager = PlatformManager.getInstance();
        final Platform platform = platformManager.identifyPlatform(worldDir);
        final int minY = platform.minZ;
        final Map<String, AtomicInteger>[] blockTypeCounts = new Map[(maxHeight - minY) >> 4];
        for (int i = 0; i < blockTypeCounts.length; i++) {
            blockTypeCounts[i] = new HashMap<>();
        }
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, dim);
        chunkStore.visitChunks(chunk -> {
            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    for (int y = maxY; y >= minY; y--) {
                        final String material = chunk.getMaterial(xx, y, zz).name;
                        blockTypeCounts[(y - minY) >> 4].computeIfAbsent(material, i -> new AtomicInteger()).incrementAndGet();
//                        totalBlockCount++;
                    }
                }
            }
            return true;
        });

        switch (dim) {
            case DIM_NORMAL:
                System.out.println("\tGranite\tDiorite\tAndesiteTuff\tDirt\tGravel\tStone\tDeepslateCoal\tCopper\tLapis\tIron\tGold\tRedstoneDiamond\tEmerald\tWater*\tLava*");
                for (int y = 0; y < (maxHeight - minY) >> 4; y++) {
                    final int total = blockTypeCounts[y].values().stream().map(AtomicInteger::get).reduce(0, Integer::sum);
                    final int stoneLikeTotal = blockTypeCounts[y].computeIfAbsent(MC_STONE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_GRANITE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DIORITE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_ANDESITE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_TUFF, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DIRT, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_GRAVEL, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_BEDROCK, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_COAL_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_COPPER_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_LAPIS_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_IRON_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_GOLD_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_REDSTONE_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DIAMOND_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_EMERALD_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_COAL_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_COPPER_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_LAPIS_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_IRON_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_GOLD_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_REDSTONE_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_DIAMOND_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_DEEPSLATE_EMERALD_ORE, i -> new AtomicInteger()).get();
                    //            System.out.println("Total stonelike blocks: " + stoneLikeTotal);
                    System.out.print(((y << 4) + minY) + "\t");
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_GRANITE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_DIORITE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_ANDESITE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_TUFF).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_DIRT).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_GRAVEL).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_STONE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_DEEPSLATE).get() / stoneLikeTotal * 1000));

                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_COAL_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_COAL_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_COPPER_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_COPPER_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_LAPIS_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_LAPIS_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_IRON_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_IRON_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_GOLD_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_GOLD_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_REDSTONE_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_REDSTONE_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_DIAMOND_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_DIAMOND_ORE).get()) / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) (blockTypeCounts[y].get(MC_EMERALD_ORE).get() + blockTypeCounts[y].get(MC_DEEPSLATE_EMERALD_ORE).get()) / stoneLikeTotal * 1000));

                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].computeIfAbsent(MC_WATER, k -> new AtomicInteger()).get() / total * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].computeIfAbsent(MC_LAVA, k -> new AtomicInteger()).get() / total * 1000));
                    System.out.println("(* Water and Lava are ‰ of ALL blocks)");
                }
                break;
            case DIM_NETHER:
                System.out.println("\tNetherrackBasaltBedrock\tBlackstoneGlowstoneGravelMagma\tSand\tSoil\tGold\tQuarz\tDebris");
                for (int y = 0; y < (maxHeight - minY) >> 4; y++) {
                    final int stoneLikeTotal = blockTypeCounts[y].computeIfAbsent(MC_NETHERRACK, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_BASALT, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_BEDROCK, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_BLACKSTONE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_GLOWSTONE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_GRAVEL, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_MAGMA_BLOCK, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_SOUL_SAND, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_SOUL_SOIL, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_NETHER_GOLD_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_NETHER_QUARTZ_ORE, i -> new AtomicInteger()).get()
                            + blockTypeCounts[y].computeIfAbsent(MC_ANCIENT_DEBRIS, i -> new AtomicInteger()).get();
                    System.out.print(((y << 4) + minY) + "\t");
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_NETHERRACK).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_BASALT).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_BEDROCK).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_BLACKSTONE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_GLOWSTONE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_GRAVEL).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_MAGMA_BLOCK).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_SOUL_SAND).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_SOUL_SOIL).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_NETHER_GOLD_ORE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_NETHER_QUARTZ_ORE).get() / stoneLikeTotal * 1000));
                    System.out.printf("%6.2f‰\t", ((float) blockTypeCounts[y].get(MC_ANCIENT_DEBRIS).get() / stoneLikeTotal * 1000));
                    System.out.println();
                }
                break;
            case DIM_END:
                // TODO
                break;
        }
    }
}