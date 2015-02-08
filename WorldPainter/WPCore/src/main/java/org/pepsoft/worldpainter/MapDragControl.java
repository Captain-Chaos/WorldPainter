/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public interface MapDragControl {
    boolean isMapDraggingInhibited();
    void setMapDraggingInhibited(boolean mapDraggingInhibited);
}