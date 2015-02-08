/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.io.IOException;
import java.io.ObjectInputStream;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;

/**
 *
 * @author pepijn
 */
@Deprecated
public class FlatTileFactory extends HeightMapTileFactory {
    private FlatTileFactory() {
        super(0, null, 0, false, null);
        height = 0;
        throw new UnsupportedOperationException("Only exists for deserialising old worlds");
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy support
        if (getHeightMap() == null) {
            setHeightMap(new ConstantHeightMap(height));
        }
    }
    
    private final int height;

    private static final long serialVersionUID = 2011032901L;
}