package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class DumpBlockData {
    public static void main(String[] args) {
        Set<Material> allMaterials = new HashSet<>();
        Map<String, Map<String, Set<String>>> allProperties = new HashMap<>();
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyMap(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            for (int y = 0; y < DEFAULT_MAX_HEIGHT_ANVIL; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        Material material = chunk.getMaterial(x, y, z);
                        allMaterials.add(material);
                        Map<String, Set<String>> matProps = allProperties.computeIfAbsent(material.name, key -> new HashMap<>());
                        if (material.getProperties() != null) {
                            for (Map.Entry<String, String> entry: material.getProperties().entrySet()) {
                                matProps.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).add(entry.getValue());
                            }
                        }
                    }
                }
            }
            return true;
        });
        System.out.println("All blocks, properties and values encountered:");
        allProperties.forEach((name, matProps) -> {
            System.out.println(name);
            matProps.forEach((propName, values) -> {
                System.out.println("   " + propName);
                System.out.println("       " + values);
            });
        });
    }
}