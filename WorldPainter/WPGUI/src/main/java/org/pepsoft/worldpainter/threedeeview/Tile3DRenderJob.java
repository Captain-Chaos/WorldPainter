/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import org.pepsoft.util.jobqueue.Job;
import org.pepsoft.worldpainter.Tile;

/**
 *
 * @author pepijn
 */
public class Tile3DRenderJob implements Job{
    public Tile3DRenderJob(Tile tile) {
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tile3DRenderJob other = (Tile3DRenderJob) obj;
        if (this.tile != other.tile && (this.tile == null || !this.tile.equals(other.tile))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + (this.tile != null ? this.tile.hashCode() : 0);
        return hash;
    }

    private final Tile tile;
}