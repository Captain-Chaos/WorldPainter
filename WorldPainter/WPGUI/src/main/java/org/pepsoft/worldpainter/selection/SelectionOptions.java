package org.pepsoft.worldpainter.selection;

/**
 * Created by Pepijn Schmitz on 07-11-16.
 */
public class SelectionOptions {
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

    public boolean isDoBlending() {
        return doBlending;
    }

    public void setDoBlending(boolean doBlending) {
        this.doBlending = doBlending;
    }

    public boolean isCreateNewTiles() {
        return createNewTiles;
    }

    public void setCreateNewTiles(boolean createNewTiles) {
        this.createNewTiles = createNewTiles;
    }

    public boolean isRemoveExistingLayers() {
        return removeExistingLayers;
    }

    public void setRemoveExistingLayers(boolean removeExistingLayers) {
        this.removeExistingLayers = removeExistingLayers;
    }

    boolean copyHeights = true, copyTerrain = true, copyFluids = true, copyLayers = true, copyBiomes = true,
        copyAnnotations, doBlending, createNewTiles = false, removeExistingLayers = true;
}