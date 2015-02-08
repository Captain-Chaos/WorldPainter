/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Terrain;

/**
 *
 * @author pepijn
 */
public interface TerrainOperation extends Operation {
    Terrain getTerrain();
    void setTerrain(Terrain terrain);
}
