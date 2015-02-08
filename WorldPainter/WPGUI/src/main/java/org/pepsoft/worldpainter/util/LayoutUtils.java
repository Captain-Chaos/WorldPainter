/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author SchmitzP
 */
public final class LayoutUtils {
    private LayoutUtils() {
        // Prevent instantiation
    }
    
    public static void addRowOfComponents(Container container, GridBagConstraints constraints, Collection<? extends Component> components) {
        for (Iterator<? extends Component> i = components.iterator(); i.hasNext(); ) {
            Component component = i.next();
            if (i.hasNext()) {
                // Not the last component
                constraints.gridwidth = 1;
                constraints.weightx = 0.0;
            } else {
                // Last component
                constraints.gridwidth = GridBagConstraints.REMAINDER;
                constraints.weightx = 1.0;
            }
            container.add(component, constraints);
        }
    }    

    public static void insertRowOfComponents(Container container, GridBagConstraints constraints, int index, Collection<? extends Component> components) {
        for (Iterator<? extends Component> i = components.iterator(); i.hasNext(); ) {
            Component component = i.next();
            if (i.hasNext()) {
                // Not the last component
                constraints.gridwidth = 1;
                constraints.weightx = 0.0;
            } else {
                // Last component
                constraints.gridwidth = GridBagConstraints.REMAINDER;
                constraints.weightx = 1.0;
            }
            container.add(component, constraints, index++);
        }
    }    
}