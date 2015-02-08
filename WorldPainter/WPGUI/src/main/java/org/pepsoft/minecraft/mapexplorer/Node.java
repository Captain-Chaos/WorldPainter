/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

/**
 *
 * @author pepijn
 */
public interface Node {
    boolean isLeaf();
    Node[] getChildren();
}