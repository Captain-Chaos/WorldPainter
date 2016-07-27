package org.pepsoft.util.swing;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 20-07-16.
 */
public class ScrollablePanel extends JPanel implements Scrollable {
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return unitIncrement;
    }

    public void setUnitIncrement(int unitIncrement) {
        this.unitIncrement = unitIncrement;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return blockIncrement;
    }

    public void setBlockIncrement(int blockIncrement) {
        this.blockIncrement = blockIncrement;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return trackViewportWidth;
    }

    public void setTrackViewportWidth(boolean trackViewportWidth) {
        this.trackViewportWidth = trackViewportWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return trackViewportHeight;
    }

    public void setTrackViewportHeight(boolean trackViewportHeight) {
        this.trackViewportHeight = trackViewportHeight;
    }

    private int unitIncrement = 10, blockIncrement = 100;
    private boolean trackViewportWidth = false, trackViewportHeight = false;
}