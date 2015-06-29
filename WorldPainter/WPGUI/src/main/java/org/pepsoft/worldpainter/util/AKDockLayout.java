////////////////////////////////////////////////////////////////////////
//AKDockLayout.java
//A layout manager to control toolbar docking.
//
package org.pepsoft.worldpainter.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;


public class AKDockLayout extends BorderLayout {
    @Override
    public void addLayoutComponent(Component c, Object con) {

        synchronized (c.getTreeLock()) {

            if (con != null) {

                String s = con.toString();

                switch (s) {
                    case NORTH:

                        north.add(c);

                        break;
                    case SOUTH:

                        south.add(c);

                        break;
                    case EAST:

                        east.add(c);

                        break;
                    case WEST:

                        west.add(c);

                        break;
                    case CENTER:

                        center = c;

                        break;
                }

                c.getParent().validate();

            }

        }

    }

    @Override
    public void removeLayoutComponent(Component c) {

        north.remove(c);

        south.remove(c);

        east.remove(c);

        west.remove(c);

        if (c == center) {
            center = null;
        }

        flipSeparators(c, SwingConstants.VERTICAL);

    }

    @Override
    public void layoutContainer(Container target) {

        synchronized (target.getTreeLock()) {

            Insets insets = target.getInsets();

            int top = insets.top;

            int bottom = target.getHeight() - insets.bottom;

            int left = insets.left;

            int right = target.getWidth() - insets.right;

            northHeight = getPreferredDimension(north).height;

            southHeight = getPreferredDimension(south).height;

            eastWidth = getPreferredDimension(east).width;

            westWidth = getPreferredDimension(west).width;

            placeComponents(target, north, left, top, right - left, northHeight,
                    TOP);

            top += (northHeight + getVgap());

            placeComponents(target, south, left, bottom - southHeight,
                    right - left, southHeight, BOTTOM);

            bottom -= (southHeight + getVgap());

            placeComponents(target, east, right - eastWidth, top, eastWidth,
                    bottom - top, RIGHT);

            right -= (eastWidth + getHgap());

            placeComponents(target, west, left, top, westWidth, bottom - top, LEFT);

            left += (westWidth + getHgap());

            if (center != null) {

                center.setBounds(left, top, right - left, bottom - top);

            }

        }

    }

// Returns the ideal width for a vertically oriented toolbar
// and the ideal height for a horizontally oriented toolbar:
    private Dimension getPreferredDimension(List<Component> comps) {

        int w = 0, h = 0;

        for (Component comp : comps) {

            Component c = (Component) comp;

            Dimension d = c.getPreferredSize();

            w = Math.max(w, d.width);

            h = Math.max(h, d.height);
        }

        return new Dimension(w, h);

    }

    private void placeComponents(Container target, List<Component> comps,
            int x, int y, int w, int h, int orientation) {

        int offset = 0;

        Component c = null;

        if (orientation == TOP || orientation == BOTTOM) {

            offset = x;

            int totalWidth = 0;

            for (int i = 0; i < comps.size(); i++) {

                c = (Component) (comps.get(i));

                flipSeparators(c, SwingConstants.VERTICAL);

                int cwidth = c.getPreferredSize().width;

                totalWidth += cwidth;

                if (w < totalWidth && i != 0) {

                    offset = x;

                    if (orientation == TOP) {

                        y += h;

                        northHeight += h;

                    } else if (orientation == BOTTOM) {

                        southHeight += h;

                        y -= h;

                    }

                    totalWidth = cwidth;

                }

                c.setBounds(x + offset, y, cwidth, h);

                offset += cwidth;

            }

            flipSeparators(c, SwingConstants.VERTICAL);

        } else {

            int totalHeight = 0;

            for (int i = 0; i < comps.size(); i++) {

                c = (Component) (comps.get(i));

                int cheight = c.getPreferredSize().height;

                totalHeight += cheight;

                if (h < totalHeight && i != 0) {

                    if (orientation == LEFT) {

                        x += w;

                        westWidth += w;

                    } else if (orientation == RIGHT) {

                        eastWidth += w;

                        x -= w;

                    }

                    totalHeight = cheight;

                    offset = 0;

                }

                if (totalHeight > h) {
                    cheight = h - 1;
                }

                c.setBounds(x, y + offset, w, cheight);

                offset += cheight;

            }

            flipSeparators(c, SwingConstants.HORIZONTAL);

        }

    }

    private void flipSeparators(Component c, int orientn) {

        if (c != null && c instanceof JToolBar
                && UIManager.getLookAndFeel().getName().toLowerCase().indexOf("windows")
                != -1) {

            JToolBar jtb = (JToolBar) c;

            Component comps[] = jtb.getComponents();

            if (comps != null && comps.length > 0) {

                for (int i = 0; i < comps.length; i++) {

                    try {

                        Component component = comps[i];

                        if (component != null) {

                            if (component instanceof JSeparator) {

                                jtb.remove(component);

                                JSeparator separ = new JSeparator();

                                if (orientn == SwingConstants.VERTICAL) {

                                    separ.setOrientation(SwingConstants.VERTICAL);

                                    separ.setMinimumSize(new Dimension(2, 6));

                                    separ.setPreferredSize(new Dimension(2, 6));

                                    separ.setMaximumSize(new Dimension(2, 100));

                                } else {

                                    separ.setOrientation(SwingConstants.HORIZONTAL);

                                    separ.setMinimumSize(new Dimension(6, 2));

                                    separ.setPreferredSize(new Dimension(6, 2));

                                    separ.setMaximumSize(new Dimension(100, 2));

                                }

                                jtb.add(separ, i);

                            }

                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                }

            }

        }

    }
    
    private List<Component> north = new ArrayList<>(1);
    private List<Component> south = new ArrayList<>(1);
    private List<Component> east = new ArrayList<>(1);
    private List<Component> west = new ArrayList<>(1);
    private Component center = null;
    private int northHeight, southHeight, eastWidth, westWidth;
    
    public static final int TOP = SwingConstants.TOP;
    public static final int BOTTOM = SwingConstants.BOTTOM;
    public static final int LEFT = SwingConstants.LEFT;
    public static final int RIGHT = SwingConstants.RIGHT;
    
    private static final long serialVersionUID = 1L;
}