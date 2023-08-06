package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.exporters.CliffCarverExporter;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import static org.pepsoft.worldpainter.layers.Layer.DataSize.BIT;

public class CliffCarver extends CustomLayer {
    public CliffCarver(String name, String description, Object paint) {
        super(name, description, BIT, 24, paint);
    }

    @Override
    public LayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        return new CliffCarverExporter(dimension, platform, settings, this);
    }

    private static final long serialVersionUID = 1L;
}