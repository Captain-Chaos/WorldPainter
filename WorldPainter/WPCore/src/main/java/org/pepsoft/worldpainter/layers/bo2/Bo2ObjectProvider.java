/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.io.Serializable;
import java.util.List;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 * A provider of one or more {@link WPObject}(s).
 *
 * @author pepijn
 */
public interface Bo2ObjectProvider extends Serializable, Cloneable {
    /**
     * Get the name of this object provider (if it represents one object this
     * may be the name of the object).
     *
     * @return The name of this object provider.
     */
    String getName();

    /**
     * Obtain one {@link WPObject}, which may be the same one every time, or a
     * different one from a random collection or sequence every time.
     *
     * @return One <code>WPObject</code>.
     */
    WPObject getObject();

    /**
     * Obtain a list of <em>all</em> different {@link WPObject}s which this
     * object provider can return. This is an optional operation which may
     * throw an {@link UnsupportedOperationException} if this object provider
     * does not support it.
     *
     * @return A list of <em>all</em> objects which this provider can return.
     * @throws UnsupportedOperationException If this object provider does not
     *     support this operation (for example if the objects are dynamically
     *     generated).
     */
    List<WPObject> getAllObjects();

    /**
     * Sets the seed of the PRNG, for object providers which use one. Does
     * nothing for other object providers.
     *
     * @param seed The seed to set on the PRNG, if any.
     */
    void setSeed(long seed);

    /**
     * Create a deep copy of the object provider.
     *
     * @return A deep copy of the object provider.
     */
    Bo2ObjectProvider clone();
}