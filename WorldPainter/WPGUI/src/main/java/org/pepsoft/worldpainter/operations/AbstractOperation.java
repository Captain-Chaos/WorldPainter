/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

/**
 * An abstract base class for WorldPainter {@link Operation}s which provides
 * name and description getters and separate {@link #activate()} and
 * {@link #deactivate()} methods for convenience, and implements
 * {@link #toString()}.
 *
 * @author pepijn
 */
public abstract class AbstractOperation implements Operation {
    protected AbstractOperation(String name, String description) {
        this(name, description, name.toLowerCase().replaceAll("\\s", ""));
    }

    protected AbstractOperation(String name, String description, String iconName) {
        if ((name == null) || (description == null)) {
            throw new NullPointerException();
        }
        this.name = name;
        this.description = description;
        icon = IconUtils.loadScaledImage(getClass().getClassLoader(), "org/pepsoft/worldpainter/icons/" + iconName + ".png");
    }

    @Override
    public void setView(WorldPainterView view) {
        this.view = view;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final void setActive(boolean active) throws PropertyVetoException {
        if (active != this.active) {
            this.active = active;
            if (active) {
                try {
                    activate();
                } catch (PropertyVetoException e) {
                    this.active = false;
                    throw e;
                }
            } else {
                deactivate();
            }
        }
    }

    @Override
    public final BufferedImage getIcon() {
        return icon;
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    protected final WorldPainterView getView() {
        return view;
    }

    /**
     * Utility method for obtaining the current dimension from the configured view.
     *
     * @return The current dimension from the configured view. May be {@code null} if the view is not configured, or it
     * does not return a dimension.
     */
    protected final Dimension getDimension() {
        return (view != null) ? view.getDimension() : null;
    }

    protected abstract void activate() throws PropertyVetoException;
    protected abstract void deactivate();

    private final String name, description;
    private final BufferedImage icon;
    private boolean active;
    private WorldPainterView view;
}