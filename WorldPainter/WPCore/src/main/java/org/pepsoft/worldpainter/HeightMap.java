/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.heightMaps.*;

import javax.swing.*;
import java.awt.*;

/**
 * A height map.
 *
 * @author pepijn
 */
public interface HeightMap {
    /**
     * Get the name of the height map.
     *
     * @return The name of the height map.
     */
    String getName();

    /**
     * Get the seed of the height map.
     *
     * @return The seed of the height map.
     */
    long getSeed();

    /**
     * Set the seed of the height map.
     *
     * @param seed The new seed of the height map.
     */
    void setSeed(long seed);

    /**
     * Get the height of the height map at a particular location.
     *
     * @param x The X coordinate of the location of which to get the height.
     * @param y The Y coordinate of the location of which to get the height.
     * @return The height at the specified location.
     */
    double getHeight(int x, int y);
    
    /**
     * Get the height of the height map at a particular location.
     *
     * @param x The X coordinate of the location of which to get the height.
     * @param y The Y coordinate of the location of which to get the height.
     * @return The height at the specified location.
     */
    double getHeight(float x, float y);

    /**
     * Get the base height of this height map, in other words the lowest value
     * it can have.
     *
     * @return The base height of this height map.
     */
    double getBaseHeight();

    /**
     * Create a deep copy of the height map.
     *
     * @return A deep copy of the height map.
     */
    HeightMap clone();

    /**
     * Get the extent of the height map, if any. The extent is the area it
     * covers and for which it can return meaningful values. A height map may
     * be unbounded in which case this should return {@code null}.
     *
     * @return The extent of the height map, or {@code null} if it is
     * unbounded.
     */
    Rectangle getExtent();

    /**
     * Get the colour of the height map at a particular location as a combined
     * RGB value consisting of the red component in bits 16-23, the green
     * component in bits 8-15, and the blue component in bits 0-7.
     *
     * @param x The X coordinate of the location of which to get the colour.
     * @param y The Y coordinate of the location of which to get the colour.
     * @return The colour at the specified location.
     */
    int getColour(int x, int y);

    /**
     * Indicate whether the height map is constant, i.e. it always returns the
     * same value regardless of the location.
     *
     * @return {@code true} if the height map is constant.
     */
    boolean isConstant();

    /**
     * If the height map is {@link #isConstant() constant}: returns the constant
     * value that would be returned for any location from
     * {@link #getHeight(int, int)} and {@link #getHeight(float, float)}. If the
     * height map is not constant the behaviour is undefined.
     *
     * @return The constant value of this height map, providing that it is
     * constant according to {@link #isConstant()}.
     */
    double getConstantValue();

    /**
     * Get a 16x16 icon representing this height map.
     *
     * @return A 16x16 icon representing this height map.
     */
    Icon getIcon();

    /**
     * Get the range of this height map, i.e. the lowest and highest values it can return, as a double array of size
     * two, with the lower bound in index 0 (which must be equal to {@link #getBaseHeight()}) and the upper bound in
     * index 1.
     *
     * @return The range of this height map.
     */
    double[] getRange();

    /**
     * Create a new height map which is the sum of this and another height map.
     */
    default HeightMap plus(HeightMap addend) {
        return new SumHeightMap(this, addend);
    }

    /**
     * Create a new height map which is the sum of this and a constant value.
     */
    default HeightMap plus(double addend) {
        return new SumHeightMap(this, new ConstantHeightMap(addend));
    }

    /**
     * Create a new height map which is the difference of this and another
     * height map.
     */
    default HeightMap minus(HeightMap subtrahend) {
        return new DifferenceHeightMap(this, subtrahend);
    }

    /**
     * Create a new height map which is the difference of this and a constant
     * value.
     */
    default HeightMap minus(double subtrahend) {
        return new DifferenceHeightMap(this, new ConstantHeightMap(subtrahend));
    }

    /**
     * Create a new height map which is the product of this and another
     * height map.
     */
    default HeightMap times(HeightMap factor) {
        return new ProductHeightMap(this, factor);
    }

    /**
     * Create a new height map which is the product of this and a constant
     * value.
     */
    default HeightMap times(double factor) {
        return new ProductHeightMap(this, new ConstantHeightMap(factor));
    }

    /**
     * Create a new height map which uses bicubic interpolation to interpolate values between the integer coordinates
     * of this height map.
     */
    default HeightMap smoothed() {
        return new BicubicHeightMap(this);
    }

    /**
     * Create a new height map that is this height map scaled up by a given factor.
     */
    default HeightMap scaled(float scale) {
        return TransformingHeightMap.build().withHeightMap(this).withScale(scale).now();
    }

    /**
     * Create a new height map that constrains the value of this height map to a given range.
     */
    default HeightMap clamped(double min, double max) {
        return new MaximisingHeightMap(new MinimisingHeightMap(this, new ConstantHeightMap(max)), new ConstantHeightMap(min));
    }
}