/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.undo;

/**
 *
 * @author pepijn
 */
public interface Cloneable<T> extends java.lang.Cloneable {
    T clone();
}