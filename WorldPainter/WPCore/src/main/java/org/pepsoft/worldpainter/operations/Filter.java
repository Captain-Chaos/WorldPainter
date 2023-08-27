/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.panels.CombinedFilter;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter for applying operations to a WorldPainter dimensions.
 *
 * @author pepijn
 */
public interface Filter {
    /**
     * Filter the input strength.
     *
     * @param x        The X coordinate in WorldPainter dimension coordinates for which to determine the filter
     *                 strength.
     * @param y        The Y coordinate in WorldPainter dimension coordinates for which to determine the filter
     *                 strength.
     * @param strength The input filter strength as a number between 0.0 and 1.0.
     * @return The modified strength as a number between 0.0 and 1.0.
     */
    float modifyStrength(int x, int y, float strength);

    /**
     * Returns a filter that combines this one and the specified one, returning the lowest strength of them both.
     */
    default Filter and(Filter filter) {
        final List<Filter> filters;
        if (filter instanceof CombinedFilter) {
            filters = new ArrayList<>(((CombinedFilter) filter).getFilters());
        } else {
            filters = new ArrayList<>();
            filters.add(filter);
        }
        filters.add(this);
        return new CombinedFilter(filters);
    }

    /**
     * Create a filter builder for a default filter implementation.
     */
    static DefaultFilter.Builder build(Dimension dimension) {
        return new DefaultFilter.Builder(dimension);
    }
}