/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;

import javax.swing.*;
import java.awt.*;

import static javax.swing.BoxLayout.X_AXIS;

/**
 *
 * @author pepijn
 */
public class Flatten extends AbstractBrushOperation {
    public Flatten(WorldPainter view) {
        super("Flatten", "Flatten an area", view, 100, "operation.flatten");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void tick(final int centreX, final int centreY, final boolean inverse, final boolean first, final float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        if (first) {
            targetHeight = dimension.getHeightAt(centreX, centreY);
            if (targetHeight == -Float.MAX_VALUE) {
                DesktopUtils.beep();
            }
        }
        if (targetHeight == -Float.MAX_VALUE) {
            return;
        }
        dimension.setEventsInhibited(true);
        try {
            int radius = getEffectiveRadius();
            boolean applyTheme = options.isApplyTheme();
            switch (mode) {
                case FLATTEN -> {
                    for (int x = centreX - radius; x <= centreX + radius; x++) {
                        for (int y = centreY - radius; y <= centreY + radius; y++) {
                            float currentHeight = dimension.getHeightAt(x, y);
                            float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                            if (strength > 0.0f) {
                                float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                                dimension.setHeightAt(x, y, newHeight);
                                if (applyTheme) {
                                    dimension.applyTheme(x, y);
                                }
                            }
                        }
                    }
                }
                case RAISE -> {
                    for (int x = centreX - radius; x <= centreX + radius; x++) {
                        for (int y = centreY - radius; y <= centreY + radius; y++) {
                            float currentHeight = dimension.getHeightAt(x, y);
                            float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                            if (strength > 0.0f) {
                                float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                                if (newHeight > currentHeight) {
                                    dimension.setHeightAt(x, y, newHeight);
                                    if (applyTheme) {
                                        dimension.applyTheme(x, y);
                                    }
                                }
                            }
                        }
                    }
                }
                case LOWER -> {
                    for (int x = centreX - radius; x <= centreX + radius; x++) {
                        for (int y = centreY - radius; y <= centreY + radius; y++) {
                            float currentHeight = dimension.getHeightAt(x, y);
                            float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                            if (strength > 0.0f) {
                                float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                                if (newHeight < currentHeight) {
                                    dimension.setHeightAt(x, y, newHeight);
                                    if (applyTheme) {
                                        dimension.applyTheme(x, y);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
    
    private final TerrainShapingOptions<Flatten> options = new TerrainShapingOptions<>();
    private final TerrainShapingOptionsPanel optionsPanel = new TerrainShapingOptionsPanel("Flatten", "<p>Click to flatten the surrounding terrain to the level at that location", options) {
        @Override
        protected void addAdditionalComponents(GridBagConstraints constraints) {
            final ButtonGroup buttonGroup = new ButtonGroup();

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, X_AXIS));
            buttonPanel.add(new JLabel(FLATTEN_ICON));
            final JRadioButton flattenButton = new JRadioButton("Flatten in both directions", true);
            flattenButton.addActionListener(e -> mode = Mode.FLATTEN);
            buttonGroup.add(flattenButton);
            buttonPanel.add(flattenButton);
            add(buttonPanel, constraints);

            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, X_AXIS));
            buttonPanel.add(new JLabel(RAISE_ICON));
            final JRadioButton raiseButton = new JRadioButton("Only raise terrain", true);
            raiseButton.addActionListener(e -> mode = Mode.RAISE);
            buttonGroup.add(raiseButton);
            buttonPanel.add(raiseButton);
            add(buttonPanel, constraints);

            buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, X_AXIS));
            buttonPanel.add(new JLabel(LOWER_ICON));
            final JRadioButton lowerButton = new JRadioButton("Only lower terrain", true);
            lowerButton.addActionListener(e -> mode = Mode.LOWER);
            buttonGroup.add(lowerButton);
            buttonPanel.add(lowerButton);
            add(buttonPanel, constraints);

            super.addAdditionalComponents(constraints);
        }
    };
    private float targetHeight = -Float.MAX_VALUE;
    private Mode mode = Mode.FLATTEN;

    private static final Icon FLATTEN_ICON = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/flatten.png");
    private static final Icon RAISE_ICON = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/raise.png");
    private static final Icon LOWER_ICON = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/lower.png");

    enum Mode { FLATTEN, RAISE, LOWER}
}