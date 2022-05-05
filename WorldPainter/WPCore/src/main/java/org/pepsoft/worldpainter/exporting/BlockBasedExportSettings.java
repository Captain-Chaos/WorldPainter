package org.pepsoft.worldpainter.exporting;

public abstract class BlockBasedExportSettings extends ExportSettings {
    public abstract boolean isCalculateSkyLight();

    public abstract boolean isCalculateBlockLight();

    public abstract boolean isCalculateLeafDistance();

    public abstract boolean isRemoveFloatingLeaves();
}