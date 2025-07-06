/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import jpen.*;
import jpen.event.PenListener;
import jpen.owner.multiAwt.AwtPenToolkit;
import org.pepsoft.util.SystemUtils;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.EventLogger;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.vo.EventVO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.event.MouseEvent.BUTTON3;
import static jpen.PButton.Type.RIGHT;
import static jpen.PKind.Type.*;
import static org.pepsoft.util.AwtUtils.doOnEventThreadAndWait;

/**
 * A localised operation which uses the mouse or tablet to indicate where and
 * how it should be applied.
 *
 * @author pepijn
 */
public abstract class MouseOrTabletOperation extends AbstractOperation implements PenListener, MouseListener, MouseMotionListener {
    /**
     * Creates a new one-shot operation (an operation which performs a single action when clicked).
     * {@link #tick(int, int, boolean, boolean, float)} will only be invoked once per activation for these operations.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     * @param iconName The base name of the icon for the operation.
     */
    protected MouseOrTabletOperation(String name, String description, String statisticsKey, String iconName) {
        this(name, description, -1, true, statisticsKey, iconName);
    }

    /**
     * Creates a new continuous operation (an operation which is continually performed while e.g. the mouse button is
     * held down). {@link #tick(int, int, boolean, boolean, float)} will be invoked every {@code delay}
     * milliseconds during each activation of these operations, with the {@code first} parameter set to
     * {@code true} for the first invocation per activation, and set to {@code false} for all subsequent
     * invocations per activation.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param delay The delay in ms between each invocation of {@link #tick(int, int, boolean, boolean, float)} while
     *              this operation is being applied by the user.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     * @param iconName The base name of the icon for the operation.
     */
    protected MouseOrTabletOperation(String name, String description, int delay, String statisticsKey, String iconName) {
        this(name, description, delay, false, statisticsKey, iconName);
    }

    /**
     * Creates a new one-shot operation (an operation which performs a single action when clicked).
     * {@link #tick(int, int, boolean, boolean, float)} will only be invoked once per activation for these operations.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     */
    protected MouseOrTabletOperation(String name, String description, String statisticsKey) {
        this(name, description, -1, true, statisticsKey, null);
    }

    /**
     * Creates a new continuous operation (an operation which is continually performed while e.g. the mouse button is
     * held down). {@link #tick(int, int, boolean, boolean, float)} will be invoked every {@code delay}
     * milliseconds during each activation of these operations, with the {@code first} parameter set to
     * {@code true} for the first invocation per activation, and set to {@code false} for all subsequent
     * invocations per activation.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param delay The delay in ms between each invocation of {@link #tick(int, int, boolean, boolean, float)} while
     *              this operation is being applied by the user.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     */
    protected MouseOrTabletOperation(String name, String description, int delay, String statisticsKey) {
        this(name, description, delay, false, statisticsKey, null);
    }

    /**
     * @deprecated Use {@link #MouseOrTabletOperation(String, String, String)}.
     */
    @Deprecated
    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, String statisticsKey) {
        this(name, description, -1, true, statisticsKey, null);
        setView(view);
    }

    /**
     * @deprecated Use {@link #MouseOrTabletOperation(String, String, String, String)}.
     */
    @Deprecated
    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, String statisticsKey, String iconName) {
        this(name, description, -1, true, statisticsKey, iconName);
        setView(view);
    }

    /**
     * @deprecated Use {@link #MouseOrTabletOperation(String, String, int, String)}.
     */
    @Deprecated
    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey) {
        this(name, description, delay, false, statisticsKey, null);
        setView(view);
    }

    /**
     * @deprecated Use {@link #MouseOrTabletOperation(String, String, int, String, String)}.
     */
    @Deprecated
    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey, String iconName) {
        this(name, description, delay, false, statisticsKey, iconName);
        setView(view);
    }

    private MouseOrTabletOperation(String name, String description, int delay, boolean oneshot, String statisticsKey, String iconName) {
        super(name, description, (iconName != null) ? iconName : name.toLowerCase().replaceAll("\\s", ""));
        this.delay = delay;
        this.oneShot = oneshot;
        this.statisticsKey = statisticsKey;
        statisticsKeyUndo = statisticsKey + ".undo";
        legacy = (SystemUtils.isMac() && System.getProperty("os.version").startsWith("10.4."))
                || "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.disableTabletSupport"))
                // TODO this is based on one incident. Check whether it is always necessary. See: https://github.com/Captain-Chaos/WorldPainter/issues/263
                || (SystemUtils.isLinux() && (System.getenv("XDG_SESSION_TYPE") != null) && System.getenv("XDG_SESSION_TYPE").equalsIgnoreCase("wayland"));
        if (legacy) {
            logger.warn("Tablet support disabled for operation " + name);
        }
    }
    
    @Override
    public void setView(WorldPainterView view) {
        if (getView() != null) {
            deactivate();
        }
        super.setView(view);
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        if ((level < 0.0f) || (level > 1.0f)) {
            throw new IllegalArgumentException();
        }
        this.level = level;
    }

    @Override
    public void interrupt() {
        if (timer != null) {
            doOnEventThreadAndWait(() -> {
                if (timer != null) {
                    logOperation(undo ? statisticsKeyUndo : statisticsKey);
                    timer.stop();
                    timer = null;
                    finished();
                    Dimension dimension = getDimension();
                    if (dimension != null) {
                        dimension.armSavePoint();
                    }
                    App.getInstance().resumeAutosave();
                }
            });
        }
    }

    // PenListener (these methods are invoked in non-legacy mode, even for mouse events)
    
    @Override
    public void penLevelEvent(PLevelEvent ple) {
        for (PLevel pLevel: ple.levels) {
            switch (pLevel.getType()) {
                case PRESSURE:
                    dynamicLevel = pLevel.value;
                    break;
                case X:
                    x = pLevel.value;
                    break;
                case Y:
                    y = pLevel.value;
                    break;
                default:
                    // Do nothing
            }
        }
    }

    @Override
    public void penButtonEvent(PButtonEvent pbe) {
        PKind.Type penKindType = pbe.pen.getKind().getType();
        final boolean stylus = penKindType == STYLUS;
        final boolean eraser = penKindType == ERASER;
        if ((! stylus) && (! eraser) && (penKindType != CURSOR)) {
            // We don't want events from keyboards, etc.
            return;
        }
        final PButton.Type buttonType = pbe.button.getType();
        switch (buttonType) {
            case ALT:
                altDown = pbe.button.value;
                break;
            case CONTROL:
                ctrlDown = pbe.button.value;
                break;
            case SHIFT:
                shiftDown = pbe.button.value;
                break;
            case LEFT:
            case RIGHT:
                if (pbe.button.value) {
                    // Button pressed
                    first = true;
                    undo = eraser || (buttonType == RIGHT) || altDown;
                    final WorldPainterView view = getView();
                    if (! oneShot) {
                        interrupt(); // Make sure any operation in progress (due to timing issues perhaps) is interrupted
                        timer = new Timer(delay, e -> {
                            Point worldCoords = view.viewToWorld((int) x, (int) y);
                            tick(worldCoords.x, worldCoords.y, undo, first, (stylus || eraser) ? dynamicLevel : 1.0f);
                            view.updateStatusBar(worldCoords.x, worldCoords.y);
                            first = false;
                        });
                        timer.setInitialDelay(0);
                        timer.start();
                        operationStartedWithButton = buttonType.ordinal();
                        App.getInstance().pauseAutosave();
//                        start = System.currentTimeMillis();
                    } else {
                        Point worldCoords = view.viewToWorld((int) x, (int) y);
                        SwingUtilities.invokeLater(() -> {
                            App.getInstance().pauseAutosave();
                            try {
                                tick(worldCoords.x, worldCoords.y, undo, true, 1.0f);
                                view.updateStatusBar(worldCoords.x, worldCoords.y);
                                Dimension dimension = getDimension();
                                if (dimension != null) {
                                    dimension.armSavePoint();
                                }
                                logOperation(undo ? statisticsKeyUndo : statisticsKey);
                            } finally {
                                App.getInstance().resumeAutosave();
                            }
                        });
                    }
                } else {
                    // Button released
                    // Finish the operation, but only if the button being
                    // released is the one that actually started it
                    if (buttonType.ordinal() == operationStartedWithButton) {
                        interrupt();
                    }
                }
                break;
        }
    }
    
    @Override public void penKindEvent(PKindEvent pke) {}
    @Override public void penScrollEvent(PScrollEvent pse) {}
    @Override public void penTock(long l) {}

    // MouseListener (these methods are only invoked in legacy mode)
    
    @Override
    public void mousePressed(MouseEvent me) {
        if ((me.getButton() != BUTTON1) && (me.getButton() != BUTTON3)) {
            // Only interested in left and right mouse buttons
            // TODO: the right mouse button is not button three on two-button
            //  mice, is it?
            return;
        }
        x = me.getX();
        y = me.getY();
        altDown = me.isAltDown() || me.isAltGraphDown();
        undo = (me.getButton() == BUTTON3) || altDown;
        ctrlDown = me.isControlDown() || me.isMetaDown();
        shiftDown = me.isShiftDown();
        first = true;
        final WorldPainterView view = getView();
        if (! oneShot) {
            interrupt(); // Make sure any operation in progress (due to timing issues perhaps) is interrupted
            timer = new Timer(delay, e -> {
                Point worldCoords = view.viewToWorld((int) x, (int) y);
                tick(worldCoords.x, worldCoords.y, undo, first, 1.0f);
                view.updateStatusBar(worldCoords.x, worldCoords.y);
                first = false;
            });
            timer.setInitialDelay(0);
            timer.start();
            operationStartedWithButton = me.getButton();
            App.getInstance().pauseAutosave();
//            start = System.currentTimeMillis();
        } else {
            Point worldCoords = view.viewToWorld((int) x, (int) y);
            App.getInstance().pauseAutosave();
            try {
                tick(worldCoords.x, worldCoords.y, undo, true, 1.0f);
                view.updateStatusBar(worldCoords.x, worldCoords.y);
                Dimension dimension = getDimension();
                if (dimension != null) {
                    dimension.armSavePoint();
                }
                logOperation(undo ? statisticsKeyUndo : statisticsKey);
            } finally {
                App.getInstance().resumeAutosave();
            }
        }
        me.consume();
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        if ((me.getButton() != BUTTON1) && (me.getButton() != BUTTON3)) {
            // Only interested in left and right mouse buttons
            // TODO: the right mouse button is not button three on two-button
            //  mice, is it?
            return;
        }
        // Finish the operation, but only if the button being
        // released is the one that actually started it
        if (me.getButton() == operationStartedWithButton) {
            interrupt();
            me.consume();
        }
    }

    @Override public void mouseClicked(MouseEvent me) {}
    @Override public void mouseEntered(MouseEvent me) {}
    @Override public void mouseExited(MouseEvent me) {}
    
    // MouseMotionListener (these methods are only invoked in legacy mode)
    
    @Override
    public void mouseDragged(MouseEvent me) {
        x = me.getX();
        y = me.getY();
    }

    @Override
    public void mouseMoved(MouseEvent me) {
        altDown = me.isAltDown() || me.isAltGraphDown();
        ctrlDown = me.isControlDown() || me.isMetaDown();
        shiftDown = me.isShiftDown();
    }

    @Override
    protected void activate() throws PropertyVetoException {
        final WorldPainterView view = getView();
        if (legacy) {
            view.addMouseListener(this);
            view.addMouseMotionListener(this);
        } else {
            AwtPenToolkit.addPenListener(view, this);
        }
        // Prevent hanging modifiers
        altDown = ctrlDown = shiftDown = false;
    }

    @Override
    protected void deactivate() {
        interrupt();
        final WorldPainterView view = getView();
        if (legacy) {
            view.removeMouseMotionListener(this);
            view.removeMouseListener(this);
        } else {
            AwtPenToolkit.removePenListener(view, this);
        }
    }

    /**
     * Apply the operation.
     * 
     * @param centreX The x coordinate of the center of the brush, in world
     *     coordinates.
     * @param centreY The y coordinate of the center of the brush, in world
     *     coordinates.
     * @param inverse Whether to perform the "inverse" operation instead of the
     *     regular operation, if applicable. If the operation has no inverse it
     *     should just apply the normal operation.
     * @param first Whether this is the first tick of a continuous operation.
     *     For a one shot operation this will always be {@code true}.
     * @param dynamicLevel The dynamic level (from 0.0f to 1.0f inclusive) to
     *     apply in addition to the {@code level} property, for instance
     *     due to a pressure sensitive stylus being used. In other words,
     *     <strong>not</strong> the total level at which to apply the operation!
     *     Operations are free to ignore this if it is not applicable. If the
     *     operation is being applied through a means which doesn't provide a
     *     dynamic level (for instance the mouse), this will be <em>exactly</em>
     *     {@code 1.0f}.
     */
    protected abstract void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel);

    /**
     * Invoked after the last {@link #tick(int, int, boolean, boolean, float)}
     * when the user ceases to apply the operation (except for one shot
     * operations).
     */
    protected void finished() {
        // Do nothing
    }

    /**
     * Determine whether the Alt (PC/Mac), AltGr (PC) or Option (Mac) key is
     * currently depressed. <strong>Warning:</strong> this key is also used to
     * invert operations! It is probably a bad idea to overload it with anything
     * else.
     *
     * @return {@code true} if the Alt, AltGr or Option key is currently
     * depressed.
     */
    protected final boolean isAltDown() {
        return altDown;
    }

    /**
     * Determine whether the Ctrl (PC/Mac), Windows (PC) or Command (Mac) key is
     * currently depressed.
     *
     * @return {@code true} if the Ctrl (PC/Mac), Windows (PC) or
     * Command (Mac) key is currently depressed.
     */
    protected final boolean isCtrlDown() {
        return ctrlDown;
    }

    /**
     * Determine whether the Shift key is currently depressed.
     *
     * @return {@code true} if the Shift key is currently depressed.
     */
    protected final boolean isShiftDown() {
        return shiftDown;
    }
    
    public static void flushEvents(EventLogger eventLogger) {
        synchronized (operationCounts) {
            for (Map.Entry<String, Long> entry: operationCounts.entrySet()) {
                eventLogger.logEvent(new EventVO(entry.getKey()).count(entry.getValue()));
            }
            operationCounts.clear();
        }
    }
    
    private static void logOperation(String key) {
        synchronized (operationCounts) {
            if (operationCounts.containsKey(key)) {
                operationCounts.put(key, operationCounts.get(key) + 1);
            } else {
                operationCounts.put(key, 1L);
            }
        }
    }
    
    protected final boolean legacy;
    
    private final int delay;
    private final boolean oneShot;
    private final String statisticsKey, statisticsKeyUndo;
    private volatile Timer timer;
    private volatile boolean altDown, ctrlDown, shiftDown, first = true, undo;
    private volatile float dynamicLevel = 1.0f;
    private volatile float x, y;
    private float level = 1.0f;
//    private long start;
    private volatile int operationStartedWithButton;

    private static final Map<String, Long> operationCounts = new HashMap<>();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MouseOrTabletOperation.class);
}