package org.pepsoft.worldpainter;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.MixedMaterial.Mode.BLOBS;

public class RegressionIT {
    /**
     * Test whether a custom terrain saved to a file in version 2.5.1 can still
     * be loaded.
     */
    @Test
    public void test2_5_1Terrain() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(RegressionIT.class.getResourceAsStream("/testset/Dirt_Grass_Sand_Gravel.terrain")))) {
            MixedMaterial terrain = (MixedMaterial) in.readObject();
            assertEquals("Dirt/Grass/Sand/Gravel", terrain.getName());
            assertEquals(BLOBS, terrain.getMode());
            assertEquals(4, terrain.getRows().length);
            assertEquals(DIRT, terrain.getRows()[0].material);
            assertEquals(250, terrain.getRows()[0].occurrence);
            assertEquals(1f, terrain.getRows()[0].scale, 0);
            assertEquals(GRASS, terrain.getRows()[1].material);
            assertEquals(250, terrain.getRows()[1].occurrence);
            assertEquals(1f, terrain.getRows()[1].scale, 0);
            assertEquals(SAND, terrain.getRows()[2].material);
            assertEquals(250, terrain.getRows()[2].occurrence);
            assertEquals(1f, terrain.getRows()[2].scale, 0);
            assertEquals(GRAVEL, terrain.getRows()[3].material);
            assertEquals(250, terrain.getRows()[3].occurrence);
            assertEquals(1f, terrain.getRows()[3].scale, 0);
        }
    }
}