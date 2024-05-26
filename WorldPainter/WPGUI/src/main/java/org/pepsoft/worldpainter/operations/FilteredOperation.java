package org.pepsoft.worldpainter.operations;

/**
 * A WorldPainter {@link Operation} which uses a {@link Filter}. WorldPainter will invoke {@link #setFilter(Filter)}
 * automatically prior to activation to set the brush filter currently configured by the user.
 */
public interface FilteredOperation {
    /**
     * Get the currently configured filter.
     *
     * @return The currently configured filter.
     */
    Filter getFilter();

    /**
     * Set the filter to use for operations.
     *
     * @param filter The filter to use for operations.
     */
    void setFilter(Filter filter);
}