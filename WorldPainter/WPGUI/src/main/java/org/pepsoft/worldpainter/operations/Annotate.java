/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.Annotations;
import static org.pepsoft.worldpainter.layers.exporters.AnnotationsExporter.AnnotationsSettings;

/**
 *
 * @author SchmitzP
 */
public class Annotate extends LayerPaint {
    public Annotate(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, ColourScheme colourScheme) {
        super(view, radiusControl, mapDragControl, Annotations.INSTANCE);
        painter.setLayer(Annotations.INSTANCE);
        painter.setNumValue(currentColour);
        optionsPanel = createOptionsPanel(colourScheme);
    }
    
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void activate() {
        super.activate();
        painter.setDimension(getDimension());
        painter.setBrush(getBrush());
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        currentTool.tick(centreX, centreY, inverse, first, dynamicLevel, (isAltDown() ? ALT : 0) | (isCtrlDown() ? CTRL : 0) | (isShiftDown() ? SHIFT : 0));
    }

    @Override
    protected void brushChanged() {
        super.brushChanged();
        painter.setBrush(getBrush());
    }
    
    private JPanel createOptionsPanel(ColourScheme colourScheme) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        panel.add(toolGrid);
        JToggleButton button = new JToggleButton(IconUtils.loadIcon("org/pepsoft/worldpainter/icons/pencil.png"));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setSelected(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = PENCIL;
                currentTool.activate();
            }
        });
        toolButtonGroup.add(button);
        toolGrid.add(button);
        button = new JToggleButton(IconUtils.loadIcon("org/pepsoft/worldpainter/icons/text.png"));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = TEXT;
                currentTool.activate();
            }
        });
        toolButtonGroup.add(button);
        toolGrid.add(button);
        
        panel.add(colourGrid);
        for (int i = 1; i < 16; i++) {
            final int selectedColour = i;
            button = new JToggleButton(createColourIcon(colourScheme.getColour(Constants.BLK_WOOL, i - ((i < 8) ? 1 : 0))));
            button.setMargin(new Insets(2, 2, 2, 2));
            if (i == 1) {
                button.setSelected(true);
            }
            colourButtonGroup.add(button);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentColour = selectedColour;
                }
            });
            colourGrid.add(button);
        }
        return panel;
    }

    private Icon createColourIcon(int colour) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return new ImageIcon(image);
    }
    
    private final JPanel optionsPanel, colourGrid = new JPanel(new GridLayout(0, 4)), toolGrid = new JPanel(new GridLayout(0, 4));
    private final ButtonGroup colourButtonGroup = new ButtonGroup(), toolButtonGroup = new ButtonGroup();
    private final DimensionPainter painter = new DimensionPainter();
    private int currentColour = 1;
    
    abstract class Tool {
        abstract void tick(int centerX, int centerY, boolean undo, boolean first, float dynamicLevel, int modifiers);
        
        void activate() {
            // Do nothing
        }
        
        void deactivate() {
            // Do nothing
        }
    }
    
    final Tool PENCIL = new Tool() {
        @Override
        void tick(int centreX, int centreY, boolean undo, boolean first, float dynamicLevel, int modifiers) {
            Dimension dimension = getDimension();
            dimension.setEventsInhibited(true);
            try {
                painter.setNumValue(undo ? 0 : currentColour);
                if (first) {
                    // Either a single click, or the start of a drag
                    lockedX = centreX;
                    lockedY = centreY;
                    lockedAxis = null;
                    if ((modifiers & SHIFT) != 0) {
                        // Shift was pressed: draw a line from the last location
                        if ((previousX != Integer.MIN_VALUE) && (previousY != Integer.MIN_VALUE)) {
                            if ((modifiers & CTRL) != 0) {
                                // Ctrl was also pressed: snap the line to 45 degree
                                // angles
                                int[] snappedCoords = snapCoords(previousX, previousY, centreX, centreY, getAxis(previousX, previousY, centreX, centreY));
                                centreX = snappedCoords[0];
                                centreY = snappedCoords[1];
                            }
                            painter.drawLine(previousX, previousY, centreX, centreY);
                        }
                        inhibitDrag = true;
                    } else {
                        // Shift was not pressed: just draw a single dot
                        painter.drawPoint(centreX, centreY);
                        inhibitDrag = false;
                    }
                    previousX = centreX;
                    previousY = centreY;
                } else if (! inhibitDrag) {
                    // Continuation of a drag
                    if ((modifiers & CTRL) != 0) {
                        // Ctrl was pressed: snap the line to 45 degree angles
                        // relative to the point where the drag was started
                        if (lockedAxis == null) {
                            lockedAxis = getAxis(lockedX, lockedY, centreX, centreY);
                        }
                        int[] snappedCoords = snapCoords(lockedX, lockedY, centreX, centreY, lockedAxis);
                        centreX = snappedCoords[0];
                        centreY = snappedCoords[1];
                    }
                    if ((centreX != previousX) || (centreY != previousY)) {
                        if ((Math.abs(centreX - previousX) <= 1) && (Math.abs(centreY - previousY) <= 1)) {
                            painter.drawPoint(centreX, centreY);
                        } else {
                            painter.drawLine(previousX, previousY, centreX, centreY);
                        }
                        previousX = centreX;
                        previousY = centreY;
                    }
                }
            } finally {
                dimension.setEventsInhibited(false);
            }
        }

        @Override
        void activate() {
            getView().setDrawRadius(true);
        }

        private Axis getAxis(int x1, int y1, int x2, int y2) {
            if ((x1 == x2) && (y1 == y2)) {
                return null;
            }
            double angle = Math.atan((double) (y2 - y1) / (x2 - x1));
            if (x2 < x1) {
                angle += Math.PI;
            } else if (angle < 0) {
                angle += Math.PI * 2;
            }
            switch ((int) (angle * 4 / Math.PI + 0.5)) {
                case 0:
                case 4:
                case 8:
                    return Axis.W_E;
                case 1:
                case 5:
                    return Axis.NW_SE;
                case 2:
                case 6:
                    return Axis.N_S;
                case 3:
                case 7:
                    return Axis.NE_SW;
                default:
                    throw new InternalError();
            }
        }
        
        private int[] snapCoords(int x1, int y1, int x2, int y2, Axis axis) {
            if (axis == null) {
                return new int[] {x2, y2};
            }
            switch (axis) {
                case W_E:
                    return new int[] {x2, y1};
                case NW_SE:
                    Point closestPoint = closestPoint(new Point(x1, y1), new Point(x1 + 1000, y1 + 1000), new Point(x2, y2));
                    return new int[] {closestPoint.x, closestPoint.y};
                case N_S:
                    return new int[] {x1, y2};
                case NE_SW:
                    closestPoint = closestPoint(new Point(x1, y1), new Point(x1 + 1000, y1 - 1000), new Point(x2, y2));
                    return new int[] {closestPoint.x, closestPoint.y};
                default:
                    throw new IllegalArgumentException();
            }
        }
        
        /**
         * Returns the closest point on the infinite line through p1 and p2 to
         * p3.
         * 
         * @param p1 First point of the line
         * @param p2 Second point of the line
         * @param p3 Point to which we want to find the closest point on
         *     the line defined by p1,p2
         * @return The closest point on the line through p1 and p2 to p3
         */
        private Point closestPoint(Point p1, Point p2, Point p3) {
            final double xDelta = p2.getX() - p1.getX();
            final double yDelta = p2.getY() - p1.getY();

            if ((xDelta == 0) && (yDelta == 0)) {
                throw new IllegalArgumentException("p1 and p2 cannot be the same point");
            }

            final double u = ((p3.getX() - p1.getX()) * xDelta + (p3.getY() - p1.getY()) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

            return new Point((int) (p1.getX() + u * xDelta + 0.5), (int) (p1.getY() + u * yDelta + 0.5));
        }
    
        private int previousX = Integer.MIN_VALUE, previousY = Integer.MIN_VALUE, lockedX = Integer.MIN_VALUE, lockedY = Integer.MIN_VALUE;
        private Axis lockedAxis;
        private boolean inhibitDrag;
    };
    
    private final Tool TEXT = new Tool() {
        @Override
        void tick(int centerX, int centerY, boolean undo, boolean first, float dynamicLevel, int modifiers) {
            if (first) {
                AnnotationsSettings settings = (AnnotationsSettings) getDimension().getLayerSettings(Annotations.INSTANCE);
                if (settings == null) {
                    settings = new AnnotationsSettings();
                }
                TextDialog dialog = new TextDialog(SwingUtilities.getWindowAncestor(getView()), settings.getDefaultFont(), settings.getDefaultSize(), savedText);
                dialog.setVisible(true);
                if (! dialog.isCancelled()) {
                    Font font = dialog.getSelectedFont();
                    settings.setDefaultFont(font.getFamily());
                    settings.setDefaultSize(font.getSize());
                    Dimension dimension = getDimension();
                    dimension.setLayerSettings(Annotations.INSTANCE, settings);
                    painter.setFont(font);
                    painter.setTextAngle(dialog.getSelectedAngle());
                    painter.setNumValue(undo ? 0 : currentColour);
                    savedText = dialog.getText();
                    dimension.setEventsInhibited(true);
                    try {
                        painter.drawText(centerX, centerY, savedText);
                    } finally {
                        dimension.setEventsInhibited(false);
                    }
                    dimension.armSavePoint();
                }
            }
        }

        @Override
        void activate() {
            getView().setDrawRadius(false);
        }
        
        private String savedText;
    };
    
    private Tool currentTool = PENCIL;

    static final int ALT   = 1;
    static final int CTRL  = 2;
    static final int SHIFT = 4;

    enum Axis {W_E, NW_SE, N_S, NE_SW}
}