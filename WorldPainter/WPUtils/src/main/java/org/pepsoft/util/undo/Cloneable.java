/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.undo;

/**
 * A cloneable class which promises that its clones are deep copies.
 *
 * @author pepijn
 */
public interface Cloneable<T> extends java.lang.Cloneable {
    /**
     * Make a deep copy of the object. All mutable state, recursively, must be copied.
     *
     * @return A deep copy of the object.
     */
    T clone();
}