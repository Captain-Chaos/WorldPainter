package org.pepsoft.worldpainter.exporting;

import java.awt.*;
import java.io.Serializable;
import java.util.Set;

public class WorldExportSettings implements Serializable {
    public WorldExportSettings() {
        // Do nothing
    }

    public WorldExportSettings(Set<Integer> dimensionsToExport, Set<Point> tilesToExport, Set<Step> stepsToSkip) {
        this.dimensionsToExport = dimensionsToExport;
        this.tilesToExport = tilesToExport;
        this.stepsToSkip = stepsToSkip;
    }

    public boolean isExportEverything() {
        return ((dimensionsToExport == null) || dimensionsToExport.isEmpty())
                && ((tilesToExport == null) || tilesToExport.isEmpty())
                && ((stepsToSkip == null) || stepsToSkip.isEmpty());
    }

    public Set<Integer> getDimensionsToExport() {
        return dimensionsToExport;
    }

    public void setDimensionsToExport(Set<Integer> dimensionsToExport) {
        this.dimensionsToExport = dimensionsToExport;
    }

    public Set<Point> getTilesToExport() {
        return tilesToExport;
    }

    public void setTilesToExport(Set<Point> tilesToExport) {
        this.tilesToExport = tilesToExport;
    }

    public Set<Step> getStepsToSkip() {
        return stepsToSkip;
    }

    public void setStepsToSkip(Set<Step> stepsToSkip) {
        this.stepsToSkip = stepsToSkip;
    }

    /**
     * The dimension(s) to export. If this is {@code null} then <em>all</em> dimensions must be exported.
     */
    private Set<Integer> dimensionsToExport;

    /**
     * The tiles to export. This may only be set if {@code dimensionsToExport} is set and contains one dimension.
     */
    private Set<Point> tilesToExport;

    /**
     * Export steps to skip. If this is {@code null} than <em>all</em> steps must be performed.
     */
    private Set<Step> stepsToSkip;

    public static final WorldExportSettings EXPORT_EVERYTHING = new WorldExportSettings() {
        @Override
        public void setDimensionsToExport(Set<Integer> dimensionsToExport) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTilesToExport(Set<Point> tilesToExport) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setStepsToSkip(Set<Step> stepsToSkip) {
            throw new UnsupportedOperationException();
        }
    };

    private static final long serialVersionUID = 1L;

    public enum Step { CAVES, RESOURCES, LIGHTING, LEAVES }
}