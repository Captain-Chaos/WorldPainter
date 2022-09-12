/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.DefaultFilter;

/**
 *
 * @author pepijn
 */
public interface Filter {
    float modifyStrength(int x, int y, float strength);

    static DefaultFilter.Builder build(Dimension dimension) {
        return new DefaultFilter.Builder(dimension);
    }
}