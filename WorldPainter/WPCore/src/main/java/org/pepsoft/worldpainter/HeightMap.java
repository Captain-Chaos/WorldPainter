/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public interface HeightMap extends Serializable {
    String getName();
    
    long getSeed();
    
    void setSeed(long seed);
    
    float getHeight(int x, int y);
    
    float getHeight(float x, float y);

    float getBaseHeight();
    
    HeightMap clone();
    
    Rectangle getExtent();
    
    int getColour(int x, int y);

    boolean isConstant();

    float getConstantValue();

    Icon getIcon();
}