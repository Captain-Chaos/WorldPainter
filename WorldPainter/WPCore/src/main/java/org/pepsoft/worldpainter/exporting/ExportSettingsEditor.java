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
    public abstract void setExportSettings(ExportSettings exportSettings);
    public abstract ExportSettings getExportSettings();
}