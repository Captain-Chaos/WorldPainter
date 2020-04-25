/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.util;

import java.awt.*;
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

    /**
     * Increases the size of the specified window to be the specified percentage
     * of the screen (in width and height, not in area) and places it centered
     * relative to its parent.
     *
     * @param window The window to size and place.
     * @param percentageOfScreen The percentage of the screen width and height
     *                           it should take up.
     */
    public static void setDefaultSizeAndLocation(Window window, int percentageOfScreen) {
        Window parent = window.getOwner();
        if (parent != null) {
            DisplayMode displayMode = parent.getGraphicsConfiguration().getDevice().getDisplayMode();
            window.setSize(
                    Math.max(window.getWidth(), displayMode.getWidth() * percentageOfScreen / 100),
                    Math.max(window.getHeight(), displayMode.getHeight() * percentageOfScreen / 100));
            window.setLocationRelativeTo(parent);
        } else {
            DisplayMode displayMode = window.getGraphicsConfiguration().getDevice().getDisplayMode();
            window.setSize(
                    Math.max(window.getWidth(), displayMode.getWidth() * percentageOfScreen / 100),
                    Math.max(window.getHeight(), displayMode.getHeight() * percentageOfScreen / 100));
            window.setLocationRelativeTo(null);
        }
    }
}