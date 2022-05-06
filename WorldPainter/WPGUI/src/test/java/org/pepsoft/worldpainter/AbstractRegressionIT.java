package org.pepsoft.worldpainter;

import org.junit.BeforeClass;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.tools.scripts.RegressionIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public abstract class AbstractRegressionIT {
    @BeforeClass
    public static void init() {
        logger.info("user.dir: {}", System.getProperty("user.dir"));
        logger.info("user.home: {}", System.getProperty("user.home"));
        logger.info("$HOME: {}", System.getenv("HOME"));
    }

    protected void testObjects(String path, Loader loader, Tester tester) throws IOException {
        URL baseURL = org.pepsoft.worldpainter.tools.scripts.RegressionIT.class.getResource(path);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(baseURL.openStream()))) {
            String fileName;
            while ((fileName = in.readLine()) != null) {
                WPObject object = loader.load(RegressionIT.class.getResourceAsStream(path + "/" + fileName));
                tester.test(object);
            }
        }
    }

    /**
     * Sanity check of basic WPObject functionality by invoking
     * {@link WPObject#getMask(int, int, int)} and
     * {@link WPObject#getMaterial(int, int, int)} on its entire volume.
     *
     * @param object The object to scan.
     */
    protected void scanObject(WPObject object) {
        Point3i dim = object.getDimensions();
        for (int x = 0; x < dim.x; x++) {
            for (int y = 0; y < dim.y; y++) {
                for (int z= 0; z < dim.z; z++) {
                    if (object.getMask(x, y, z)) {
                        object.getMaterial(x, y, z);
                    }
                }
            }
        }
    }

    public interface Loader { WPObject load(InputStream in) throws IOException; }
    public interface Tester { void test(WPObject object); }

    private static final Logger logger = LoggerFactory.getLogger(AbstractRegressionIT.class);
}