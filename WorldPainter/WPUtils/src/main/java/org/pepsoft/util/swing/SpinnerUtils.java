/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import javax.swing.*;

/**
 *
 * @author Pepijn
 */
public final class SpinnerUtils {
    private SpinnerUtils() {
        // Prevent instantiation
    }

    /**
     * Set the minimum of a {@link JSpinner} with a number spinner model to a new value, adjusting the current value
     * first if necessary.
     *
     * @param spinner The spinner of which to set the minimum. Must have a {@link SpinnerNumberModel}.
     * @param newMinimum The value to set the minimum to. Must be a {@link Number}.
     */
    public static void setMinimum(JSpinner spinner, Comparable<?> newMinimum) {
        setMinimum((SpinnerNumberModel) spinner.getModel(), newMinimum);
    }

    /**
     * Set the minimum of a number spinner model to a new value, adjusting the current value first if necessary.
     *
     * @param model The model of which to set the minimum.
     * @param newMinimum The value to set the minimum to. Must be a {@link Number}.
     */
    public static void setMinimum(SpinnerNumberModel model, Comparable<?> newMinimum) {
        if (((Number) model.getValue()).doubleValue() < ((Number) newMinimum).doubleValue()) {
            model.setValue(newMinimum);
        }
        model.setMinimum(newMinimum);
    }

    /**
     * Set the maximum of a {@link JSpinner} with a number spinner model to a new value, adjusting the current value
     * first if necessary.
     * 
     * @param spinner The spinner of which to set the maximum. Must have a {@link SpinnerNumberModel}.
     * @param newMaximum The value to set the maximum to. Must be a {@link Number}.
     */
    public static void setMaximum(JSpinner spinner, Comparable<?> newMaximum) {
        setMaximum((SpinnerNumberModel) spinner.getModel(), newMaximum);
    }
    
    /**
     * Set the maximum of a number spinner model to a new value, adjusting the current value first if necessary.
     * 
     * @param model The model of which to set the maximum.
     * @param newMaximum The value to set the maximum to. Must be a {@link Number}.
     */
    public static void setMaximum(SpinnerNumberModel model, Comparable<?> newMaximum) {
        if (((Number) model.getValue()).doubleValue() > ((Number) newMaximum).doubleValue()) {
            model.setValue(newMaximum);
        }
        model.setMaximum(newMaximum);
    }
}