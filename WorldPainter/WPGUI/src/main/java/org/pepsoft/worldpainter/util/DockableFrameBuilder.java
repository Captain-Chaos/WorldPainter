package org.pepsoft.worldpainter.util;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.App;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import static com.jidesoft.docking.DockableFrame.*;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.worldpainter.App.KEY_ICON;

public class DockableFrameBuilder {
    public DockableFrameBuilder(Component component, String title, int side, int index) {
        this.component = component;
        this.title = title;
        this.side = side;
        this.index = index;
        id = (Character.toLowerCase(title.charAt(0)) + title.substring(1)).replaceAll("\\s", "");
    }

    public DockableFrameBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public DockableFrameBuilder expand() {
        expand = true;
        return this;
    }

    public DockableFrameBuilder withIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public DockableFrameBuilder withMargin(int margin) {
        this.margin = margin;
        return this;
    }

    public DockableFrame build() {
        DockableFrame dockableFrame = new DockableFrame(id);

        JPanel panel = new JPanel(new GridBagLayout());
        if (margin > 0) {
            panel.setBorder(new EmptyBorder(margin, margin, margin, margin));
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        if (expand) {
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weighty = 1.0;
            panel.add(component, constraints);
        } else {
            constraints.fill = HORIZONTAL;
            panel.add(component, constraints);
            constraints.weighty = 1.0;
            panel.add(new JPanel(), constraints);
        }
        dockableFrame.add(panel, BorderLayout.CENTER);

        // Use title everywhere
        dockableFrame.setTitle(title);
        dockableFrame.setSideTitle(title);
        dockableFrame.setTabTitle(title);
        dockableFrame.setToolTipText(title);

        // Try to find an icon to use for the tab
        if ((icon == null) && (component instanceof Container)) {
            icon = findIcon((Container) component);
            if (icon != null) {
                final int desiredSize = Math.round(16 * getUIScale());
                if (((icon.getIconHeight() > desiredSize) || (icon.getIconWidth() > desiredSize))
                        && (icon instanceof ImageIcon)
                        && (((ImageIcon) icon).getImage() instanceof BufferedImage)) {
                    float s;
                    if (icon.getIconWidth() > icon.getIconHeight()) {
                        // Wide icon
                        s = (float) desiredSize / icon.getIconWidth();
                    } else {
                        // Tall (or square) icon
                        s = (float) desiredSize / icon.getIconHeight();
                    }
                    BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(s, s), AffineTransformOp.TYPE_BICUBIC);
                    BufferedImage iconImage = op.filter((BufferedImage) ((ImageIcon) icon).getImage(), null);
                    icon = new ImageIcon(iconImage);
                }
            }
        }
        dockableFrame.setFrameIcon((icon != null) ? icon : ICON_UNKNOWN_PATTERN);

        // Use preferred size of component as much as possible
        final Dimension preferredSize = component.getPreferredSize();
        dockableFrame.setAutohideWidth(preferredSize.width);
        dockableFrame.setDockedWidth(preferredSize.width);
        dockableFrame.setDockedHeight(preferredSize.height);
        dockableFrame.setUndockedBounds(new Rectangle(-1, -1, preferredSize.width, preferredSize.height));

        // Make hidable, but don't display hide button, so incidental panels can
        // be hidden on the fly
        dockableFrame.setHidable(true);
        dockableFrame.setAvailableButtons(BUTTON_FLOATING | BUTTON_AUTOHIDE | BUTTON_HIDE_AUTOHIDE);
        dockableFrame.setShowContextMenu(false); // Disable the context menu because it contains the Close option with no way to hide it

        // Initial location of panel
        dockableFrame.setInitMode(DockContext.STATE_FRAMEDOCKED);
        dockableFrame.setInitSide(side);
        dockableFrame.setInitIndex(index);

        // Other flags
        dockableFrame.setAutohideWhenActive(true);
        dockableFrame.setMaximizable(false);

        //Help key
        dockableFrame.putClientProperty(App.KEY_HELP_KEY, "Panel/" + id);
        return dockableFrame;
    }

    private final String title;
    private final int side, index;
    private final Component component;

    private String id;
    private boolean expand;
    private Icon icon;
    private int margin = 2;

    private static Icon findIcon(Container container) {
        if (container instanceof JComponent) {
            Icon icon = (Icon) ((JComponent) container).getClientProperty(KEY_ICON);
            if (icon != null) {
                return icon;
            }
        }
        for (Component component: container.getComponents()) {
            if ((component instanceof AbstractButton) && (((AbstractButton) component).getIcon() != null)) {
                return ((AbstractButton) component).getIcon();
            } else if (component instanceof Container) {
                Icon icon = findIcon((Container) component);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
    }

    private static final Icon ICON_UNKNOWN_PATTERN = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/unknown_pattern.png");
}