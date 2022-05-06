/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.junit.Assert;
import org.junit.Test;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.MixedMaterial.Row;

import static org.junit.Assert.assertEquals;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;

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
                    buckets[material.blockType]++;
                }
            }
        }
        long total = 500L * 500L * 256L;
        float dirtPerMillage =     ((float) buckets[BLK_DIRT]      * 1000 / total);
        float gravelPerMillage =   ((float) buckets[BLK_GRAVEL]    * 1000 / total);
        float stonePerMillage =    ((float) buckets[BLK_STONE]     * 1000 / total);
        float soulSandPerMillage = ((float) buckets[BLK_SOUL_SAND] * 1000 / total);
        assertEquals(400, dirtPerMillage,     1.0f);
        assertEquals(300, gravelPerMillage,   1.0f);
        assertEquals(200, stonePerMillage,    1.0f);
        assertEquals(100, soulSandPerMillage, 1.0f);
    }
}