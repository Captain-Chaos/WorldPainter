/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.awt.Point;
import org.pepsoft.minecraft.RegionFile;

/**
 *
 * @author pepijn
 */
public class FindOutliers {
    public static void main(String[] args) throws IOException {
        File[] worldDirs = new File(args[0]).listFiles();
        for (File worldDir: worldDirs) {
            System.out.println("Scanning " + worldDir);
            File[] regionFiles = new File(worldDir, "region").listFiles();
            if ((regionFiles == null) || (regionFiles.length == 0)) {
                System.out.println("  No regions found");
                continue;
            }
            int regionCount = 0, chunkCount = 0;
            int lowestChunkX = Integer.MAX_VALUE, highestChunkX = Integer.MIN_VALUE, lowestChunkZ = Integer.MAX_VALUE, highestChunkZ = Integer.MIN_VALUE;
            List<Integer> xValues = new ArrayList<>(), zValues = new ArrayList<>();
            List<Point> chunks = new ArrayList<>();
            for (File file: regionFiles) {
                String[] nameFrags = file.getName().split("\\.");
                int regionX = Integer.parseInt(nameFrags[1]);
                int regionZ = Integer.parseInt(nameFrags[2]);
                regionCount++;
                RegionFile regionFile = new RegionFile(file);
                try {
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            if (regionFile.containsChunk(x, z)) {
                                chunkCount++;
                                int chunkX = regionX * 32 + x, chunkZ = regionZ * 32 + z;
                                if (chunkX < lowestChunkX) {
                                    lowestChunkX = chunkX;
                                }
                                if (chunkX > highestChunkX) {
                                    highestChunkX = chunkX;
                                }
                                if (chunkZ < lowestChunkZ) {
                                    lowestChunkZ = chunkZ;
                                }
                                if (chunkZ > highestChunkZ) {
                                    highestChunkZ = chunkZ;
                                }
                                xValues.add(chunkX);
                                zValues.add(chunkZ);
                                chunks.add(new Point(chunkX, chunkZ));
                            }
                        }
                    }
                } finally {
                    regionFile.close();
                }
            }
//            System.out.println(args[0]);
//            System.out.println("Region count: " + regionCount);
//            System.out.println("Chunk count: " + chunkCount);
//            System.out.println("Size in chunks: " + (highestChunkX - lowestChunkX + 1) + " by " + (highestChunkZ - lowestChunkZ + 1));
//            System.out.println("Lowest chunk x: " + lowestChunkX + ", highest chunk x: " + highestChunkX);
//            System.out.println("Lowest chunk z: " + lowestChunkZ + ", highest chunk z: " + highestChunkZ);

            Collections.sort(xValues);
            int p1 = xValues.size() / 4;
            float q1 = xValues.get(p1) * 0.75f + xValues.get(p1 + 1) * 0.25f;
            int p2 = xValues.size() / 2;
            float q2 = (xValues.get(p2) + xValues.get(p2 + 1)) / 2f;
            int p3 = xValues.size() * 3 / 4;
            float q3 = xValues.get(p3) * 0.25f + xValues.get(p3 + 1) * 0.75f;
            float iqr = q3 - q1;
            int lowerLimit = (int) (q2 - iqr * 1.5f);
            int upperLimit = (int) (q2 + iqr * 1.5f);
            Set<Point> outlyingChunks = new HashSet<>();
            for (Point chunk: chunks) {
                if ((chunk.x < lowerLimit) || (chunk.x > upperLimit)) {
                    outlyingChunks.add(chunk);
                }
            }

            Collections.sort(zValues);
            p1 = zValues.size() / 4;
            q1 = zValues.get(p1) * 0.75f + zValues.get(p1 + 1) * 0.25f;
            p2 = zValues.size() / 2;
            q2 = (zValues.get(p2) + zValues.get(p2 + 1)) / 2f;
            p3 = zValues.size() * 3 / 4;
            q3 = zValues.get(p3) * 0.25f + zValues.get(p3 + 1) * 0.75f;
            iqr = q3 - q1;
            lowerLimit = (int) (q2 - iqr * 1.5f);
            upperLimit = (int) (q2 + iqr * 1.5f);
            for (Point chunk: chunks) {
                if ((chunk.y < lowerLimit) || (chunk.y > upperLimit)) {
                    outlyingChunks.add(chunk);
                }
            }

            if (! outlyingChunks.isEmpty()) {
                System.out.println("  Outlying chunk count: " + outlyingChunks.size());
//                for (Point outlyingChunk: outlyingChunks) {
//                    System.out.println(outlyingChunk);
//                }
            } else {
                System.out.println("  No outlying chunks found: " + outlyingChunks.size());
            }
        }
    }
}