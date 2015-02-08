/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.junit.Assert;
import org.junit.Test;
import org.pepsoft.minecraft.Material;
import static org.pepsoft.minecraft.Material.*;
import org.pepsoft.worldpainter.MixedMaterial.Row;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class MixedMaterialTest {
    @Test
    public void testBlobs() {
        MixedMaterial mixedMaterial = new MixedMaterial("Test", new Row[] {
            new Row(DIRT,      400, 1.0f),
            new Row(GRAVEL,    300, 1.0f),
            new Row(STONE,     200, 1.0f),
            new Row(SOUL_SAND, 100, 1.0f)
        }, -1, null);
        long[] buckets = new long[256];
        for (int x = 0; x < 500; x++) {
            for (int y = 0; y < 500; y++) {
                for (int z = 0; z < 256; z++) {
                    Material material = mixedMaterial.getMaterial(0, x, y, z);
                    Assert.assertTrue(material == DIRT || material == GRAVEL || material == STONE || material == SOUL_SAND);
                    buckets[material.getBlockType()]++;
                }
            }
            System.out.println(x);
        }
        long total = 500L * 500L * 256L;
        int dirtPerMillage =     (int) (buckets[BLK_DIRT]      * 1000L / total);
        int gravelPerMillage =   (int) (buckets[BLK_GRAVEL]    * 1000L / total);
        int stonePerMillage =    (int) (buckets[BLK_STONE]     * 1000L / total);
        int soulSandPerMillage = (int) (buckets[BLK_SOUL_SAND] * 1000L / total);
        System.out.println("Total blocks tested: " + total);
        System.out.println("Dirt: "      + dirtPerMillage     + "‰");
        System.out.println("Gravel: "    + gravelPerMillage   + "‰");
        System.out.println("Stone: "     + stonePerMillage    + "‰");
        System.out.println("Soul sand: " + soulSandPerMillage + "‰");
    }
}