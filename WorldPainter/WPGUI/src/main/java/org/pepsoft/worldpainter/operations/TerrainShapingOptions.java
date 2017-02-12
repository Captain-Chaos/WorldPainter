package org.pepsoft.worldpainter.operations;

/**
 * Options for operations which shape the terrain.
 *
 * <p>Created by Pepijn Schmitz on 18-01-17.
 */
public class TerrainShapingOptions<O extends Operation> implements OperationOptions<O> {
    public boolean isApplyTheme() {
        return applyTheme;
    }

    public void setApplyTheme(boolean applyTheme) {
        this.applyTheme = applyTheme;
    }

    private boolean applyTheme;
}