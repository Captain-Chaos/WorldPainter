/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public interface RadiusControl {
    void increaseRadius(int amount);
    void increaseRadiusByOne();
    void decreaseRadius(int amount);
    void decreaseRadiusByOne();
}