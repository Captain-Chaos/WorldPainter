/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

/**
 *
 * @author pepijn
 */
public interface Brush {
    /**
     * Get the name of the brush.
     * 
     * @return The name of the brush.
     */
    String getName();
    
    /**
     * Get the brush strength at a specific point on the brush.
     * 
     * @param dx The X coordinate relative to the center of the brush.
     * @param dy The Y coordinate relative to the center of the brush.
     * @return The brush strength at the specified location, from 0.0f (no
     *     effect) to 1.0f (maximum effect) (inclusive).
     */
    float getStrength(int dx, int dy);
    
    /**
     * Get the maximum brush strength at a specific point on the brush, in other
     * words the strength if the level was set to 1.0f. Synonymous with invoking
     * <code>setLevel(1.0f); getStrength(dx, dy)</code>, but possibly more
     * efficient and without actually changing the value of the
     * <code>level</code> property.
     * 
     * @param dx The X coordinate relative to the center of the brush.
     * @param dy The Y coordinate relative to the center of the brush.
     * @return The maximum brush strength at the specified location, from 0.0f
     *     (no effect) to 1.0f (maximum effect) (inclusive).
     */
    float getFullStrength(int dx, int dy);
    
    /**
     * Get the current radius of the brush.
     * 
     * @return The current radius of the brush in pixels.
     */
    int getRadius();

    /**
     * Set the radius of the brush.
     * 
     * @param radius The new radius of the brush in pixels.
     */
    void setRadius(int radius);

    /**
     * Get the "radius" of the bounding box of the brush. Usually this will be
     * the same as {@link #getRadius()}, but it could be larger for, for
     * instance, a rotated square brush.
     *
     * @return The radius of the bounding box of the brush.
     */
    int getEffectiveRadius();

    /**
     * Get the current level of the brush.
     * 
     * @return The current level of the brush, from 0.0f (no effect) to 1.0f
     *     (maximum effect) (inclusive).
     */
    float getLevel();

    /**
     * Set the level of the brush.
     * 
     * @param level The new level of the brush, from 0.0f (no effect) to 1.0f
     *     (maximum effect) (inclusive).
     */
    void setLevel(float level);
    
    /**
     * Get the shape of the brush.
     * 
     * @return The shape of the brush.
     */
    BrushShape getBrushShape();
    
    /**
     * Create a deep copy of the brush. The copy will not be affected by
     * subsequent modifications to the original.
     * 
     * @return A deep copy of the brush.
     */
    Brush clone();
}