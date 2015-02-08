/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import java.util.List;
import java.util.Set;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author pepijn
 */
public interface Garden {
    void clearLayer(int x, int y, Layer layer, int radius);
    
    void setCategory(int x, int y, int category);
    
    int getCategory(int x, int y);

    Set<Seed> getSeeds();
    
    <T extends Seed> List<T> findSeeds(Class<T> type, int x, int y, int radius);

    boolean isOccupied(int x, int y);

    boolean isWater(int x, int y);

    boolean isLava(int x, int y);

    void plantSeed(Seed seed);

    void removeSeed(Seed seed);

    float getHeight(int x, int y);
    
    int getIntHeight(int x, int y);
    
    boolean tick();

    void neutralise();
}