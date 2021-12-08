/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.platforms;

import org.pepsoft.worldpainter.exporting.ExportSettings;

import java.util.Objects;

import static org.pepsoft.worldpainter.platforms.JavaExportSettings.FloatMode.DROP;
import static org.pepsoft.worldpainter.platforms.JavaExportSettings.FloatMode.LEAVE_FLOATING;

/**
 *
 * @author Pepijn
 */
public class JavaExportSettings extends ExportSettings {
    public JavaExportSettings() {
        waterMode = DROP;
        lavaMode = DROP;
        sandMode = LEAVE_FLOATING;
        gravelMode = LEAVE_FLOATING;
        cementMode = LEAVE_FLOATING;
        flowWater = true;
        flowLava = true;
    }

    public JavaExportSettings(FloatMode waterMode, FloatMode lavaMode, FloatMode sandMode, FloatMode gravelMode, FloatMode cementNode, boolean flowWater, boolean flowLava) {
        this.waterMode = waterMode;
        this.lavaMode = lavaMode;
        this.sandMode = sandMode;
        this.gravelMode = gravelMode;
        this.cementMode = cementNode;
        this.flowWater = flowWater;
        this.flowLava = flowLava;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaExportSettings that = (JavaExportSettings) o;
        return flowWater == that.flowWater && flowLava == that.flowLava && waterMode == that.waterMode && lavaMode == that.lavaMode && sandMode == that.sandMode && gravelMode == that.gravelMode && cementMode == that.cementMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(waterMode, lavaMode, sandMode, gravelMode, cementMode, flowWater, flowLava);
    }

    final FloatMode waterMode, lavaMode, sandMode, gravelMode, cementMode;
    final boolean flowWater, flowLava;
    
    private static final long serialVersionUID = 1L;

    public enum FloatMode {DROP, SUPPORT, LEAVE_FLOATING}
}