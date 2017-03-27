package org.pepsoft.worldpainter.plugins;

import java.util.Collection;

/**
 * A provider of some entity or service.
 *
 * <p>Created by Pepijn on 9-3-2017.
 *
 * @param <K> The type by which this provider is keyed or discriminated.
 */
public interface Provider<K> extends Plugin {
    /**
     * Get the keys supported by this provider.
     *
     * @return The keys supported by this provider.
     */
    Collection<K> getKeys();
}