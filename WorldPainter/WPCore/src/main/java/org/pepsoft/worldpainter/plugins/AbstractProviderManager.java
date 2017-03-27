package org.pepsoft.worldpainter.plugins;

import java.util.*;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.copyOf;

/**
 * Created by Pepijn on 9-3-2017.
 *
 * @param <K> The type by which the managed provider is keyed.
 * @param <P> The type of provider managed by this class.
 */
public abstract class AbstractProviderManager<K, P extends Provider<K>> {
    protected AbstractProviderManager(Class<P> providerClass) {
        List<P> allImplementations = new ArrayList<>();
        List<K> allKeys = new ArrayList<>();
        Map<K, P> implementationsByKey = new HashMap<>();
        for (P implementation: WPPluginManager.getInstance().getPlugins(providerClass)) {
            Collection<K> keys = implementation.getKeys();
            allImplementations.add(implementation);
            for (K key: keys) {
                if (allKeys.contains(key)) {
                    throw new RuntimeException("Duplicate key encountered: " + key);
                }
                allKeys.add(key);
                implementationsByKey.put(key, implementation);
            }
        }
        this.allImplementations = copyOf(allImplementations);
        this.allKeys = copyOf(allKeys);
        this.implementationsByKey = copyOf(implementationsByKey);
    }

    protected final List<K> getKeys() {
        return allKeys;
    }

    protected final List<P> getImplementations() {
        return allImplementations;
    }

    protected final P getImplementation(K key) {
        return implementationsByKey.get(key);
    }

    private final List<P> allImplementations;
    private final List<K> allKeys;
    private final Map<K, P> implementationsByKey;
}
