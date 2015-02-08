/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKind;
import jpen.PKindEvent;
import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PScrollEvent;
import jpen.event.PenListener;
import jpen.owner.multiAwt.AwtPenToolkit;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.EventLogger;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.vo.EventVO;

/**
 *
 * @author pepijn
 */
public abstract class MouseOrTabletOperation extends AbstractOperation implements PenListener, MouseListener, MouseMotionListener {
    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, String statisticsKey) {
        this(name, description, view, -1, true, statisticsKey);
    }

    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey) {
        this(name, description, view, delay, false, statisticsKey);
    }

    protected MouseOrTabletOperation(String name, String description, WorldPainterView view, int delay, boolean oneshot, String statisticsKey) {
        super(name, description);
        setView(view);
        this.delay = delay;
        this.oneShot = oneshot;
        this.statisticsKey = statisticsKey;
        statisticsKeyUndo = statisticsKey + ".undo";
        legacy = (System.getProperty("os.name").startsWith("Mac OS X") && System.getProperty("os.version").startsWith("10.4.")) || "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.disableTabletSupport"));
        if (legacy) {
            logger.warning("Tablet support disabled for operation " + name);
        }
    }
    
    protected MouseOrTabletOperation(String name, String description, String statisticsKey) {
        this(name, description, null, statisticsKey);
    }

    protected MouseOrTabletOperation(String name, String description, int delay, String statisticsKey) {
        this(name, description, null, delay, statisticsKey);
    }
    
    public Dimension getDimension() {
        return view.getDimension();
    }
    
    public final WorldPainterView getView() {
        return view;
    }
    
    @Override
    public final void setView(WorldPainterView view) {
        if (this.view != null) {
            deactivate();
        }
        this.view = view;
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

    // PenListener
    
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
        final boolean stylus = penKindType == PKind.Type.STYLUS;
        final boolean eraser = penKindType == PKind.Type.ERASER;
        if ((! stylus) && (! eraser) && (penKindType != PKind.Type.CURSOR)) {
            // We don't want events from keyboards, etc.
            return;
        }
        final PButton.Type buttonType = pbe.button.getType();
        if (buttonType == PButton.Type.ALT) {
            altDown = pbe.button.value;
        } else if (buttonType == PButton.Type.CONTROL) {
            ctrlDown = pbe.button.value;
        } else if (buttonType == PButton.Type.SHIFT) {
            shiftDown = pbe.button.value;
        } else if ((buttonType == PButton.Type.LEFT) || (buttonType == PButton.Type.RIGHT)) {
            if (pbe.button.value) {
                // Button pressed
                first = true;
                if (! oneShot) {
                    undo = eraser || (buttonType == PButton.Type.RIGHT);
                    if (timer == null) {
                        timer = new Timer(delay, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Point worldCoords = view.viewToWorld((int) x, (int) y);
                                tick(worldCoords.x, worldCoords.y, undo, first, (stylus || eraser) ? dynamicLevel : 1.0f);
                                view.updateStatusBar(worldCoords.x, worldCoords.y);
                                first = false;
                            }
                        });
                        timer.setInitialDelay(0);
                        timer.start();
    //                    start = System.currentTimeMillis();
                    }
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Point worldCoords = view.viewToWorld((int) x, (int) y);
                            tick(worldCoords.x, worldCoords.y, eraser || (buttonType == PButton.Type.RIGHT), true, 1.0f);
                            view.updateStatusBar(worldCoords.x, worldCoords.y);
                            getDimension().armSavePoint();
                            logOperation(undo ? statisticsKeyUndo : statisticsKey);
                        }
                    });
                }
            } else {
                // Button released
                if (! oneShot) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (timer != null) {
                                logOperation(undo ? statisticsKeyUndo : statisticsKey);
                                timer.stop();
                                timer = null;
                            }
                            getDimension().armSavePoint();
                        }
                    });
                }
            }
        }
    }
    
    @Override public void penKindEvent(PKindEvent pke) {}
    @Override public void penScrollEvent(PScrollEvent pse) {}
    @Override public void penTock(long l) {}

    // MouseListener
    
    @Override
    public void mousePressed(MouseEvent me) {
        x = me.getX();
        y = me.getY();
        undo = me.getButton() == MouseEvent.BUTTON3;
        altDown = me.isAltDown() || me.isAltGraphDown();
        ctrlDown = me.isControlDown() || me.isMetaDown();
        shiftDown = me.isShiftDown();
        first = true;
        if (! oneShot) {
            if (timer == null) {
                timer = new Timer(delay, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Point worldCoords = view.viewToWorld((int) x, (int) y);
                        tick(worldCoords.x, worldCoords.y, undo, first, 1.0f);
                        view.updateStatusBar(worldCoords.x, worldCoords.y);
                        first = false;
                    }
                });
                timer.setInitialDelay(0);
                timer.start();
//                start = System.currentTimeMillis();
            }
        } else {
            Point worldCoords = view.viewToWorld((int) x, (int) y);
            tick(worldCoords.x, worldCoords.y, undo, true, 1.0f);
            view.updateStatusBar(worldCoords.x, worldCoords.y);
            getDimension().armSavePoint();
            logOperation(undo ? statisticsKeyUndo : statisticsKey);
        }
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        if (! oneShot) {
            if (timer != null) {
                logOperation(undo ? statisticsKeyUndo : statisticsKey);
                timer.stop();
                timer = null;
            }
            getDimension().armSavePoint();
        }
    }

    @Override public void mouseClicked(MouseEvent me) {}
    @Override public void mouseEntered(MouseEvent me) {}
    @Override public void mouseExited(MouseEvent me) {}
    
    // MouseMotionListener
    
    @Override
    public void mouseDragged(MouseEvent me) {
        x = me.getX();
        y = me.getY();
    }

    @Override public void mouseMoved(MouseEvent me) {
        altDown = me.isAltDown() || me.isAltGraphDown();
        ctrlDown = me.isControlDown() || me.isMetaDown();
        shiftDown = me.isShiftDown();
    }
    
    @Override
    protected void activate() {
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
     * @param x The x coordinate of the center of the brush, in world
     *     coordinates.
     * @param y The y coordinate of the center of the brush, in world
     *     coordinates.
     * @param inverse Whether to perform the "inverse" operation instead of the
     *     regular operation, if applicable.
     * @param first Whether this is the first tick of a continuous operation.
     *     For a one shot operation this will always be <code>true</code>.
     * @param dynamicLevel The dynamic level (from 0.0f to 1.0f inclusive) to
     *     apply in addition to the <code>level</code> property, for instance
     *     due to a pressure sensitive stylus being used. In other words,
     *     <strong>not</strong> the total level at which to apply the operation!
     */
    protected abstract void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel);
    
    protected final boolean isAltDown() {
        return altDown;
    }
    
    protected final boolean isCtrlDown() {
        return ctrlDown;
    }
    
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
    private WorldPainterView view;
    private volatile Timer timer;
    private boolean altDown, ctrlDown, shiftDown, first = true;
    private volatile float dynamicLevel = 1.0f;
    private volatile float x, y;
    private float level = 1.0f;
//    private long start;
    private boolean undo;
    
    private static final Map<String, Long> operationCounts = new HashMap<String, Long>();
    private static final Logger logger = Logger.getLogger(MouseOrTabletOperation.class.getName());
}