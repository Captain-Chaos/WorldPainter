/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

/**
 *
 * @author SchmitzP
 */
public class HeightFilter implements Filter {
    public HeightFilter(int maxHeight, int startHeight, int stopHeight, boolean feather) {
        this.startHeight = startHeight;
        this.stopHeight = stopHeight;
        this.feather = feather;
        if (feather) {
            if (startHeight > 0) {
                start = Math.max(startHeight - 2, 0);
                fullStart = startHeight + 2;
            } else {
                start = 0;
                fullStart = 0;
            }
            if (stopHeight < (maxHeight - 1)) {
                fullEnd = stopHeight - 2;
                end = Math.min(stopHeight + 2, maxHeight - 1);
            } else {
                fullEnd = maxHeight - 1;
                end = maxHeight - 1;
            }
        } else {
            start = fullStart = startHeight;
            end = fullEnd = stopHeight;
        }
    }

    @Override
    public int getLevel(int x, int y, int z, int inputLevel) {
        if ((z < start) || (z > end)) {
            return 0;
        } else if (z < fullStart) {
            // Lower feathering
            return inputLevel - (fullStart - z) * inputLevel / (fullStart - start + 1);
        } else if (z <= fullEnd) {
            // Full strength
            return inputLevel;
        } else {
            // Upper feathering
            return inputLevel - (z - fullEnd) * inputLevel / (end - fullEnd + 1);
        }
    }

    public int getStartHeight() {
        return startHeight;
    }

    public int getStopHeight() {
        return stopHeight;
    }

    public boolean isFeather() {
        return feather;
    }
    
    private final int start, fullStart, end, fullEnd, startHeight, stopHeight;
    private final boolean feather;
    
    private static final long serialVersionUID = 1L;
}