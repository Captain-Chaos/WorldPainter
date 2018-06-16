package org.pepsoft.worldpainter.tools.scripts;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.AbstractRegressionIT;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.bo2.Bo2Object;
import org.pepsoft.worldpainter.layers.bo2.Schematic;
import org.pepsoft.worldpainter.layers.bo2.Structure;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import static org.pepsoft.worldpainter.Constants.BIOME_ALGORITHM_1_7_DEFAULT;

public class RegressionIT extends AbstractRegressionIT {
    @BeforeClass
    public static void init() {
        Configuration.setInstance(new Configuration());
        BiomeSchemeManager.initialiseInBackground();
    }

    /**
     * Test whether custom objects of type Schematic can still be loaded
     */
    @Test
    public void testSchematics() throws IOException {
        testObjects("/testset/schematics", in -> Schematic.load(null, in), this::scanObject);
    }

    /**
     * Test whether custom objects of type Bo2 can still be loaded
     */
    @Test
    public void testBo2s() throws IOException {
        testObjects("/testset/bo2s", in -> Bo2Object.load(null, in), this::scanObject);
    }

    /**
     * Test whether custom objects of type NBT can still be loaded
     */
    @Ignore // Ignored because loading nbts requires access to a Minecraft jar
    @Test
    public void testNbts() throws IOException {
        testObjects("/testset/nbts", in -> {
            BiomeScheme biomeScheme = BiomeSchemeManager.getSharedBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT);
            if (biomeScheme instanceof MCInterface) {
                return Structure.load(null, in, (MCInterface) biomeScheme);
            } else {
                throw new UnsupportedOperationException("No supported Minecraft jar found for loading nbts");
            }
        }, this::scanObject);
    }
}