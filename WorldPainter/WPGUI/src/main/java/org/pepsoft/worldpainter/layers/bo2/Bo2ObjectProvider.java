/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.Serializable;
import java.util.List;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 *
 * @author pepijn
 */
public interface Bo2ObjectProvider extends Serializable {
    String getName();
    WPObject getObject();
    List<WPObject> getAllObjects();
    void setSeed(long seed);
}