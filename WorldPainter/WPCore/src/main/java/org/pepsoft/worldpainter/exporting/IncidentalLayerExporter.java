/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;

import javax.vecmath.Point3i;
import java.awt.*;

/**
 * An exporter which can export a layer block by block at a particular, directed
 * height and intensity.
 * 
 * @author SchmitzP
 */
public interface IncidentalLayerExporter extends LayerExporter {
    /**
     * Apply the layer at a particular single set of coordinates in the world.
     * This method should be deterministic; i.e. when applied to the same layer
     * at the same location in a dimension with the same seed the result should
     * be the same.
     * 
     * @param dimension The dimension that is being exported.
     * @param location The 3D location at which to apply the layer. Points to
     *     the block <em>above</em> the surface.
     * @param intensity The intensity at which to apply the layer, as a
     *     percentage.
     * @param exportedArea The area which will actually be exported. May be used
     *     to for instance avoid objects getting cut off at area boundaries.
     * @param minecraftWorld The Minecraft world in which to apply the layer.
     * @return An optional fixup which should be executed after all regions have
     *     been exported.
     */
    Fixup apply(Dimension dimension, Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld);
}