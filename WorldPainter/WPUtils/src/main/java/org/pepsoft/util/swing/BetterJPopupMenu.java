package org.pepsoft.util.swing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class BetterJPopupMenu extends JPopupMenu {
    public BetterJPopupMenu() {
        // Do nothing
    }

    public BetterJPopupMenu(String label) {
        super(label);
    }

    /**
     * Displays the popup menu at the position x,y in the coordinate space of the component invoker.
     *
     * <p>This version fails silently if {@code invoker} is not currently showing, rather than throwing an exception.
     *
     * @param invoker the component in whose space the popup menu is to appear
     * @param x the x coordinate in invoker's coordinate space at which the popup menu is to be displayed
     * @param y the y coordinate in invoker's coordinate space at which the popup menu is to be displayed
     */
    @Override
    public void show(Component invoker, int x, int y) {
        if ((invoker != null) && (! invoker.isShowing())) {
            logger.warn("Not showing popup menu because invoker is not showing: " + invoker);
        } else {
            super.show(invoker, x, y);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BetterJPopupMenu.class);
}