/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;

import java.awt.*;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * An exporter which will be invoked in a second pass, after all chunks have been generated. This is for exporters which
 * need information from, or make changes to, neighbouring chunks.
 *
 * <p>There are multiple stages in which this exporter may be invoked. Each stage is finished for all layers before the
 * next stage is started. The exporter should indicate with {@link #getStages()}} for which stages it would like to be
 * invoked.
 *
 * <p>Implementations should implement {@link #carve(Rectangle, Rectangle, MinecraftWorld)} and/or
 * {@link #addFeatures(Rectangle, Rectangle, MinecraftWorld)} and implement {@link #getStages()}
 * accordingly.
 *
 * @author pepijn
 */
public interface SecondPassLayerExporter extends LayerExporter {
    default Set<Stage> getStages() {
        return singleton(Stage.CARVE);
    }

    /**
     * Carve an area of the map. Will be invoked if {@link #getStages()} contains {@link Stage#CARVE}.
     *
     * @param area           The area to process.
     * @param exportedArea   The area which will actually be exported. May be smaller than {@code area}. May be used to
     *                       for instance avoid objects getting cut off at area boundaries.
     * @param minecraftWorld The {@link MinecraftWorld} to which to export the layer.
     * @return An optional list of fixups which should be executed after all regions have been exported.
     */
    default List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        return render(null, area, exportedArea, minecraftWorld, null);
    }

    /**
     * Add features to an area of the map. Will be invoked if {@link #getStages()} contains {@link Stage#ADD_FEATURES},
     * after the {@link Stage#CARVE} stage is finished for all layers.
     *
     * @param area           The area to process.
     * @param exportedArea   The area which will actually be exported. May be smaller than {@code area}. May be used to
     *                       for instance avoid objects getting cut off at area boundaries.
     * @param minecraftWorld The {@link MinecraftWorld} to which to export the layer.
     * @return An optional list of fixups which should be executed after all regions have been exported.
     */
    default List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        return null;
    }

    /**
     * @deprecated Use {@link #carve(Rectangle, Rectangle, MinecraftWorld)} instead.
     */
    @Deprecated default List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        return null;
    }

    enum Stage { CARVE, ADD_FEATURES }
}