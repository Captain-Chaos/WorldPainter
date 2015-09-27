/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import javax.swing.JTable;

/**
 *
 * @author pepijn
 */
public interface ButtonPressListener {
    void buttonPressed(JTable source, int row, int column);
}