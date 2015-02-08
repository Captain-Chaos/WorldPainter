/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.WorldPainterView;

/**
 *
 * @author pepijn
 */
public interface Operation {
    void setView(WorldPainterView view);
    String getName();
    String getDescription();
    boolean isActive();
    void setActive(boolean active);
}