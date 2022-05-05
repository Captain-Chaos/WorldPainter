/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import javax.swing.*;

/**
 *
 * @author Pepijn
 */
public abstract class ExportSettingsEditor extends JPanel {
    /**
     * Load the settings from the specified {@code ExportSettings} object.
     *
     * @param exportSettings The settings to load.
     * @throws ClassCastException If the settings object does not belong to this editor's platform.
     */
    public abstract void setExportSettings(ExportSettings exportSettings);

    /**
     * Save the export settings to an {@code ExportSettings} object.
     */
    public abstract ExportSettings getExportSettings();
}