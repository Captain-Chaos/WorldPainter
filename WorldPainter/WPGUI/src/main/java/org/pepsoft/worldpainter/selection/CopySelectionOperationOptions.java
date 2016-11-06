/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.selection;

/**
 *
 * @author Pepijn
 */
public class CopySelectionOperationOptions {
    public boolean isCopyHeights() {
        return copyHeights;
    }

    public void setCopyHeights(boolean copyHeights) {
        this.copyHeights = copyHeights;
    }

    public boolean isCopyTerrain() {
        return copyTerrain;
    }

    public void setCopyTerrain(boolean copyTerrain) {
        this.copyTerrain = copyTerrain;
    }

    public boolean isCopyFluids() {
        return copyFluids;
    }

    public void setCopyFluids(boolean copyFluids) {
        this.copyFluids = copyFluids;
    }

    public boolean isCopyLayers() {
        return copyLayers;
    }

    public void setCopyLayers(boolean copyLayers) {
        this.copyLayers = copyLayers;
    }

    public boolean isCopyBiomes() {
        return copyBiomes;
    }

    public void setCopyBiomes(boolean copyBiomes) {
        this.copyBiomes = copyBiomes;
    }

    public boolean isCopyAnnotations() {
        return copyAnnotations;
    }

    public void setCopyAnnotations(boolean copyAnnotations) {
        this.copyAnnotations = copyAnnotations;
    }
    
    private boolean copyHeights = true, copyTerrain = true, copyFluids = true,
            copyLayers = true, copyBiomes = true, copyAnnotations;
}