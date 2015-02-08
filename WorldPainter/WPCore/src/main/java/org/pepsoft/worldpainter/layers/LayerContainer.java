/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.util.List;

/**
 * Some container of layers.
 *
 * @author pepijn
 */
public interface LayerContainer {
    List<Layer> getLayers();
}