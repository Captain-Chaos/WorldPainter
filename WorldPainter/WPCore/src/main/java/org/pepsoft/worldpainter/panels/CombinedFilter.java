package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.operations.Filter;

import java.util.Collection;

public class CombinedFilter implements Filter {

    public CombinedFilter(Collection<Filter> filters) {
        this.filters = filters.toArray(new Filter[filters.size()]);
    }

    @Override
    public float modifyStrength(int x, int y, float strength) {
        for (Filter filter: filters) {
            strength = filter.modifyStrength(x, y, strength);
            if (strength < 0.0f) {
                return 0.0f;
            }
        }
        return strength;
    }

    private final Filter[] filters;
}