/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.WorldPainterView;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

/**
 * A WorldPainter operation or tool which affects the world in some way,
 * directed by the user.
 *
 * @author pepijn
 */
public interface Operation {
    /**
     * Set the {@link WorldPainterView} with which the operation should
     * associate. Will be invoked once by WorldPainter when the operation is
     * initialised.
     *
     * @param view The <code>WorldPainterView</code> with which the operation
     *             should associate.
     */
    void setView(WorldPainterView view);

    /**
     * Get the short name of the operation. Displayed to the user as part of the
     * tooltip for the operation.
     *
     * @return The short name of the operation.
     */
    String getName();

    /**
     * Get the short description of the operation. May be used in a tooltip.
     *
     * @return The short description of the operation.
     */
    String getDescription();

    /**
     * Determine whether the operation is currently active.
     *
     * @return <code>true</code> if the operation is currently active.
     */
    boolean isActive();

    /**
     * Activate or deactivate the operation. Will be invoked by WorldPainter
     * when the user activates or deactivates the operation, for example by
     * pressing a toggle button.
     *
     * <p><strong>Please note:</strong> it is the operation's own responsibility
     * to install listeners, etc. on the view if necessary to detect user
     * activity while the operation is active, and uninstall them when
     * deactivated.
     *
     * @param active Whether the operation should activatate itself (when
     *               <code>true</code>) or deactivate itself (when
     *               <code>false</code>).
     * @throws PropertyVetoException If the operation failed to activate for
     * some reason, for instance because some dependency is not met, or some
     * state is inappropriate.
     */
    void setActive(boolean active) throws PropertyVetoException;

    /**
     * Get the icon of the operation. Should be 16x16 and have a transparent
     * background.
     *
     * @return The icon of the operation.
     */
    BufferedImage getIcon();

    /**
     * Get the options panel of the operation, if any. Should be a live and
     * modifiable view of the current options of the operation. May be
     * <code>null</code> if the operation has no options.
     */
    JPanel getOptionsPanel();
}