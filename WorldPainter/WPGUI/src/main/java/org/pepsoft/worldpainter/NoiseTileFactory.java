/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.io.IOException;
import java.io.ObjectInputStream;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.heightMaps.SumHeightMap;

/**
 *
 * @author pepijn
 */
@Deprecated
public class NoiseTileFactory extends HeightMapTileFactory {
    private NoiseTileFactory() {
        super(0, null, 0, false, null);
        throw new UnsupportedOperationException("Only exists for deserialising old worlds");
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy support
        if (! beachesMigrated) {
            setBeaches(beaches);
            setRandomise(true);
            beachesMigrated = true;
        }
        if (range == 0) {
            baseHeight++;
            range = 20;
            scale = 1.0;
        }
        if (getHeightMap() == null) {
            setHeightMap(new SumHeightMap(new ConstantHeightMap(baseHeight), new NoiseHeightMap(range, scale, 1)));
        }
        perlinNoise = null;
    }

    private int baseHeight;
    @Deprecated
    private PerlinNoise perlinNoise;
    @Deprecated
    private boolean beaches;
    private boolean beachesMigrated = true;
    private float range;
    private double scale;
    
    private static final long serialVersionUID = 2011032901L;
}