package org.pepsoft.worldpainter.panels;

import org.pepsoft.worldpainter.operations.Filter;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * A filter that combines a number of subordinate filters, returning the lowest strength among them.
 */
public final class CombinedFilter implements Filter {
    public CombinedFilter(Collection<Filter> filters) {
        this(filters.toArray(new Filter[filters.size()]));
    }

    private CombinedFilter(Filter[] filters) {
        this.filters = filters;
    }

    public List<Filter> getFilters() {
        return unmodifiableList(asList(filters));
    }

    @Override
    public Filter and(Filter filter) {
        final Filter[] filters = new Filter[this.filters.length + 1];
        System.arraycopy(this.filters, 0, filters, 0, this.filters.length);
        filters[filters.length - 1] = filter;
        return new CombinedFilter(filters);
    }

    @Override
    public float modifyStrength(int x, int y, float strength) {
        for (Filter filter: filters) {
            if (strength <= 0.0f) {
                return 0.0f;
            }
            strength = filter.modifyStrength(x, y, strength);
        }
        return strength;
    }

    private final Filter[] filters;
}