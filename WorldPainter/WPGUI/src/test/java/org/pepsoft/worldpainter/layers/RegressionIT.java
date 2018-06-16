package org.pepsoft.worldpainter.layers;

import org.junit.Test;
import org.pepsoft.worldpainter.AbstractRegressionIT;
import org.pepsoft.worldpainter.objects.WPObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;

public class RegressionIT extends AbstractRegressionIT {
    /**
     * Test whether a custom layer saved to a file in version 2.5.1 can still be
     * loaded.
     */
    @Test
    public void test2_5_1Layer() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(org.pepsoft.worldpainter.tools.scripts.RegressionIT.class.getResourceAsStream("/testset/Forest.layer")))) {
            Bo2Layer layer = (Bo2Layer) in.readObject();
            assertEquals("Forest", layer.getName());
            assertEquals(0x009900, layer.getColour() & 0xffffff);
            assertEquals(20, layer.getDensity());
            List<WPObject> allObjects = layer.getObjectProvider().getAllObjects();
            assertEquals(67, allObjects.size());
            for (WPObject object: allObjects) {
                scanObject(object);
            }
        }
    }
}