/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

/**
 *
 * @author pepijn
 */
public interface Filter {
    float modifyStrength(int x, int y, float strength);
}