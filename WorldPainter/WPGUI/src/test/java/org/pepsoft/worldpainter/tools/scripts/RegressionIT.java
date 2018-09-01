package org.pepsoft.worldpainter.tools.scripts;

import org.junit.BeforeClass;
import org.junit.Test;
import org.pepsoft.worldpainter.AbstractRegressionIT;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.bo2.Bo2Object;
import org.pepsoft.worldpainter.layers.bo2.Schematic;
import org.pepsoft.worldpainter.layers.bo2.Structure;

import java.io.IOException;

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
    @Test
    public void testNbts() throws IOException {
        testObjects("/testset/nbts", in -> Structure.load(null, in), this::scanObject);
    }

    // TODO: add schems
}