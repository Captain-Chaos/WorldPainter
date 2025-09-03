/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 * @deprecated Use {@link BrushControl}.
 * @author pepijn
 */
@Deprecated
public interface RadiusControl {
    void increaseRadius(int amount);
    void increaseRadiusByOne();
    void decreaseRadius(int amount);
    void decreaseRadiusByOne();
}