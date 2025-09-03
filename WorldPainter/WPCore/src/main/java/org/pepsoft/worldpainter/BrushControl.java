/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public interface BrushControl extends RadiusControl {
    /**
     * Get the current radius of the brush in blocks.
     */
    int getRadius();

    /**
     * Increase the radius of the brush by the specified steps of 10 percent.
     */
    void increaseRadius(int steps);

    /**
     * Increase the radius of the brush by one block.
     */
    void increaseRadiusByOne();

    /**
     * Decrease the radius of the brush by the specified steps of 10 percent.
     */
    void decreaseRadius(int steps);

    /**
     * Decrease the radius of the brush by one block.
     */
    void decreaseRadiusByOne();

    /**
     * Set the radius of the brush in blocks.
     */
    void setRadius(int radius);

    /**
     * Get the current rotation of the brush in degrees.
     */
    int getRotation();

    /**
     * Set the rotation of the brush in degrees.
     */
    void setRotation(int rotation);
}