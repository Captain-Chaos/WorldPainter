/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

/**
 * A container for a {@link TiledImageViewer}, which adds scrollbars around it.
 * The scrollbars take the combined {@link TileProvider#getExtent() extents} of
 * the tile providers into account, in that their sizes by default only allow
 * scrolling inside the combined extent of the tile providers.
 *
 * <p>The image can be scrolled outside the extent by mouse dragging, or
 * programmatically, and when done so the scrollbars will adjust dynamically.
 *
 * <p>If none of the tile providers have an extent, or there are no tile
 * providers configured, the scrollbars will be disabled.
 *
 * @author pepijn
 */
public class TiledImageViewerContainer extends JPanel implements TiledImageViewer.ViewListener, AdjustmentListener {
    public TiledImageViewerContainer(TiledImageViewer view) {
        this.view = view;

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0f;
        constraints.weighty = 1.0f;
        constraints.fill = GridBagConstraints.BOTH;
        add(view, constraints);
        constraints.weightx = 0.0f;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(verticalScrollbar, constraints);
        constraints.weightx = 1.0f;
        constraints.weighty = 0.0f;
        constraints.gridwidth = 1;
        add(horizontalScrollbar, constraints);
        
        horizontalScrollbar.addAdjustmentListener(this);
        verticalScrollbar.addAdjustmentListener(this);
    
        view.setViewListener(this);
    }

    /**
     * When set to true, prevents the scrollbars from updating on view changed
     * events. When set to false, updates the scrollbars immediately.
     *
     * @param inhibitUpdates Whether scrollbar updates should be inhibited.
     */
    public void setInhibitUpdates(boolean inhibitUpdates) {
        if (inhibitUpdates != this.inhibitUpdates) {
            this.inhibitUpdates = inhibitUpdates;
            if (! inhibitUpdates) {
                programmaticChange = true;
                try {
                    updateScrollBars();
                } finally {
                    programmaticChange = false;
                }
            }
        }
    }

    // ViewListener
    
    @Override
    public void viewChanged(TiledImageViewer source) {
        if ((! programmaticChange) && (! inhibitUpdates)) {
            programmaticChange = true;
            try {
                updateScrollBars();
            } finally {
                programmaticChange = false;
            }
        }
    }

    // AdjustmentListener
    
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (programmaticChange) {
            return;
        }
        programmaticChange = true;
        try {
            if (e.getSource() == horizontalScrollbar) {
                int dx = e.getValue() - previousHorizontalValue;
                if (dx != 0) {
                    previousHorizontalValue = e.getValue();
                    view.moveBy(dx, 0);
                    if (! e.getValueIsAdjusting()) {
                        updateScrollBars();
                    }
                }
            } else if (e.getSource() == verticalScrollbar) {
                int dy = e.getValue() - previousVerticalValue;
                if (dy != 0) {
                    previousVerticalValue = e.getValue();
                    view.moveBy(0, dy);
                    if (! e.getValueIsAdjusting()) {
                        updateScrollBars();
                    }
                }
            }
        } finally {
            programmaticChange = false;
        }
    }

    private void updateScrollBars() {
        Rectangle viewExtent = view.getExtent();
        if (viewExtent == null) {
            // If there is no extent we can't meaningfully scroll, so disable
            // the scrollbars
            if (scrollingEnabled) {
                horizontalScrollbar.setEnabled(false);
                verticalScrollbar.setEnabled(false);
                scrollingEnabled = false;
            }
        } else {
            if (! scrollingEnabled) {
                horizontalScrollbar.setEnabled(true);
                verticalScrollbar.setEnabled(true);
                scrollingEnabled = true;
            }
            // If the extent is smaller than the viewport, add a border to allow
            // scrolling the extent to any edge of the viewport
            int viewWidth = view.getWidth(), viewHeight = view.getHeight();
            if (viewExtent.width < viewWidth) {
                viewExtent.x -= (viewWidth - viewExtent.width);
                viewExtent.width += (viewWidth - viewExtent.width) * 2;
            }
            if (viewExtent.height < viewHeight) {
                viewExtent.y -= (viewHeight - viewExtent.height);
                viewExtent.height += (viewHeight - viewExtent.height) * 2;
            }
            Rectangle viewBounds = new Rectangle(0, 0, viewWidth, viewHeight);
            // The combined area is the total area which the scrollbars should
            // cover:
            Rectangle combinedArea = viewExtent.union(viewBounds);
            horizontalScrollbar.setMinimum(combinedArea.x);
            horizontalScrollbar.setMaximum(combinedArea.x + combinedArea.width);
            horizontalScrollbar.setVisibleAmount(viewWidth);
            horizontalScrollbar.setValue(0);
            horizontalScrollbar.setBlockIncrement(viewWidth * 9 / 10);
            previousHorizontalValue = 0;
            verticalScrollbar.setMinimum(combinedArea.y);
            verticalScrollbar.setMaximum(combinedArea.y + combinedArea.height);
            verticalScrollbar.setVisibleAmount(viewHeight);
            verticalScrollbar.setValue(0);
            verticalScrollbar.setBlockIncrement(viewHeight * 9 / 10);
            previousVerticalValue = 0;
        }
    }
    
    private final TiledImageViewer view;
    private final JScrollBar horizontalScrollbar = new JScrollBar(JScrollBar.HORIZONTAL), verticalScrollbar = new JScrollBar();
    private int previousHorizontalValue, previousVerticalValue;
    private boolean scrollingEnabled = true, programmaticChange, inhibitUpdates;
}