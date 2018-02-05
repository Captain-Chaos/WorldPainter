package org.pepsoft.worldpainter.tools.scripts;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.pepsoft.minecraft.MCInterface;
import org.pepsoft.minecraft.Material;
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

public class RegressionIT {
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

    interface Loader { WPObject load(InputStream in) throws IOException; }
    interface Tester { void test(WPObject object); }

    private void testObjects(String path, Loader loader, Tester tester) throws IOException {
        URL baseURL = RegressionIT.class.getResource(path);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(baseURL.openStream()))) {
            String fileName;
            while ((fileName = in.readLine()) != null) {
                WPObject object = loader.load(RegressionIT.class.getResourceAsStream(path + "/" + fileName));
                tester.test(object);
                System.out.println("Tested " + fileName);
            }
        }
    }

    private void scanObject(WPObject object) {
        Point3i dim = object.getDimensions();
        for (int x = 0; x < dim.x; x++) {
            for (int y = 0; y < dim.y; y++) {
                for (int z= 0; z < dim.z; z++) {
                    if (object.getMask(x, y, z)) {
                        Material material = object.getMaterial(x, y, z);
                        // This is here solely to prevent the above from being
                        // optimised away. TODO: is that necessary?
                        if (material.blockType == -1) {
                            System.out.println("Well that's not possible!");
                        }
                    }
                }
            }
        }
    }
}
