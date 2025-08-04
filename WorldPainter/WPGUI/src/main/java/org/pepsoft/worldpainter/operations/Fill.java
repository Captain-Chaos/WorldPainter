package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

import static java.lang.Math.round;
import static javax.swing.BoxLayout.X_AXIS;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.util.swing.MessageUtils.showWarning;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.painting.DimensionPainter.AdditionalFillAction.APPLY_PAINT;
import static org.pepsoft.worldpainter.painting.DimensionPainter.AdditionalFillAction.APPLY_THEME;

/**
 * Created by pepijn on 14-5-15.
 */
public class Fill extends MouseOrTabletOperation implements PaintOperation {
    public Fill(WorldPainterView view) {
        super("Fill", "Flood fill an area of the world with any kind of layer or terrain", view, "operation.fill");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        // We have seen in the wild that this sometimes gets called recursively (perhaps someone clicks to fill more
        // than once and then it takes more than two seconds so it is continued in the background and event queue
        // processing is resumed?), which causes errors, so just ignore it if we are already filling.
        if (alreadyFilling) {
            logger.debug("Fill operation already in progress; ignoring repeated invocation");
            return;
        }
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        if (! dimension.isTilePresent(centreX >> TILE_SIZE_BITS, centreY >> TILE_SIZE_BITS)) {
            // Just silently fail if the user clicks outside the present area
            return;
        }
        alreadyFilling = true;
        try {
            painter.setUndo(inverse);
            synchronized (dimension) {
                dimension.setEventsInhibited(true);
            }
            try {
                final boolean fillComplete;
                if (radioButtonRaiseTerrain.isSelected()) {
                    if (checkboxApplyTheme.isSelected()) {
                        fillComplete = painter.fill(dimension, centreX, centreY, APPLY_THEME, SwingUtilities.getWindowAncestor(getView()));
                    } else if (checkboxApplyPaint.isSelected()) {
                        fillComplete = painter.fill(dimension, centreX, centreY, APPLY_PAINT, SwingUtilities.getWindowAncestor(getView()));
                    } else {
                        fillComplete = painter.fill(dimension, centreX, centreY, null, SwingUtilities.getWindowAncestor(getView()));
                    }
                } else {
                    fillComplete = painter.fill(dimension, centreX, centreY, SwingUtilities.getWindowAncestor(getView()));
                }
                if (! fillComplete) {
                    showWarning(getView(), "The area to be filled was too large and may not have been completely filled.", "Area Too Large");
                }
            } catch (IndexOutOfBoundsException e) {
                // This most likely indicates that the area being flooded was too large
                synchronized (dimension) {
                    if (dimension.undoChanges()) {
                        dimension.clearRedo();
                        dimension.armSavePoint();
                    }
                }
                JOptionPane.showMessageDialog(getView(), "The area to be filled is too large or complex; please retry with a smaller area", "Area Too Large", JOptionPane.ERROR_MESSAGE);
            } finally {
                synchronized (dimension) {
                    dimension.setEventsInhibited(false);
                }
            }
        } finally {
            alreadyFilling = false;
        }
    }

    @Override
    public Paint getPaint() {
        return painter.getPaint();
    }

    @Override
    public void setPaint(Paint paint) {
        painter.setPaint(paint);
        if (radioButtonRaiseTerrain.isSelected()) {
            // If the user selects a paint while having the Fill tool active and the Raise Terrain option selected,
            // assume that they also want to apply the selected paint
            checkboxApplyPaint.setSelected(true);
            checkboxApplyTheme.setSelected(false);
        }
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private final DimensionPainter painter = new DimensionPainter();
    private final JRadioButton radioButtonApplyPaint = new JRadioButton("Apply paint", true);
    private final JRadioButton radioButtonRaiseTerrain = new JRadioButton("Raise terrain");
    private final JCheckBox checkboxApplyPaint = new JCheckBox("Apply paint");
    private final JCheckBox checkboxApplyTheme = new JCheckBox("Apply theme");
    private boolean alreadyFilling;

    private final JPanel optionsPanel = new StandardOptionsPanel("Fill", null) {
        {
            final ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(radioButtonApplyPaint);
            buttonGroup.add(radioButtonRaiseTerrain);
            radioButtonRaiseTerrain.addActionListener(e -> setControlStates());
            radioButtonApplyPaint.addActionListener(e -> setControlStates());
            checkboxApplyPaint.setEnabled(false);
            checkboxApplyTheme.setEnabled(false);
            checkboxApplyPaint.addActionListener(e -> {
                if (checkboxApplyPaint.isSelected()) {
                    checkboxApplyTheme.setSelected(false);
                }
            });
            checkboxApplyTheme.addActionListener(e -> {
                if (checkboxApplyTheme.isSelected()) {
                    checkboxApplyPaint.setSelected(false);
                }
            });
        }

        @Override
        protected void addAdditionalComponents(GridBagConstraints constraints) {
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, X_AXIS));
            buttonPanel.add(new JLabel(ICON_PAINTBRUSH));
            buttonPanel.add(radioButtonApplyPaint);
            add(buttonPanel, constraints);

            addLabel("""
                    <ul>\
                    <li>Left-click to fill with the currently selected paint\
                    <li>Right-click with a Layer to remove the layer\
                    <li>Right-click with a Terrain to reset to current theme\
                    <li>Right-click with a Biome to reset to Auto Biome\
                    </ul>""", constraints);

            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, X_AXIS));
            buttonPanel.add(new JLabel(ICON_RAISE));
            buttonPanel.add(radioButtonRaiseTerrain);
            add(buttonPanel, constraints);

            constraints.insets.left = round(16 * getUIScale());
            add(checkboxApplyTheme, constraints);
            add(checkboxApplyPaint, constraints);
            constraints.insets.left = 0;
            addLabel("""
                    <ul>\
                    <li>Click to raise terrain of bounded area to the same level plus one\
                    </ul>""", constraints);
        }

        private void setControlStates() {
            checkboxApplyPaint.setEnabled(radioButtonRaiseTerrain.isSelected());
            checkboxApplyTheme.setEnabled(radioButtonRaiseTerrain.isSelected());
        }
    };

    private static final Icon ICON_RAISE = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/raise.png");
    private static final Icon ICON_PAINTBRUSH = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/paintbrush.png");

    private static final Logger logger = LoggerFactory.getLogger(Fill.class);
}