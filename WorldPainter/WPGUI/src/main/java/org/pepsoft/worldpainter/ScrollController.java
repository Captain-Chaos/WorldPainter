package org.pepsoft.worldpainter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class ScrollController extends MouseAdapter implements KeyEventDispatcher {
    ScrollController(App app) {
        this.app = app;
        timer.setRepeats(false);
    }

    void install() {
        app.view.addMouseListener(this);
        app.view.addMouseMotionListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    void uninstall() {
        if (keyDragging || mouseDragging) {
            app.glassPane.setCursor(previousCursor);
        }
        mouseDragging = false;
        keyDragging = false;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
        app.view.removeMouseMotionListener(this);
        app.view.removeMouseListener(this);
    }

    // MouseListener / MouseMotionListener

    @Override
    public void mousePressed(MouseEvent e) {
        if ((e.getButton() == MouseEvent.BUTTON2) && (!mouseDragging)) {
            if (!keyDragging) {
                Point viewLocOnScreen = app.view.getLocationOnScreen();
                e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
                previousLocation = e.getPoint();

                previousCursor = app.glassPane.getCursor();
                app.glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            mouseDragging = true;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (mouseDragging || keyDragging) {
            Point viewLocOnScreen = app.view.getLocationOnScreen();
            e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
            Point location = e.getPoint();
            if (previousLocation != null) {
                // No idea how previousLocation could be null (it
                // implies that the mouse pressed event was never
                // received or handled), but we have a report from the
                // wild that it happened, so check for it
                int dx = location.x - previousLocation.x;
                int dy = location.y - previousLocation.y;
                app.view.moveBy(-dx, -dy);
            }
            previousLocation = location;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseDragging || keyDragging) {
            Point viewLocOnScreen = app.view.getLocationOnScreen();
            e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
            Point location = e.getPoint();
            if (previousLocation != null) {
                // No idea how previousLocation could be null (it
                // implies that the mouse pressed event was never
                // received or handled), but we have a report from the
                // wild that it happened, so check for it
                int dx = location.x - previousLocation.x;
                int dy = location.y - previousLocation.y;
                app.view.moveBy(-dx, -dy);
            }
            previousLocation = location;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ((e.getButton() == MouseEvent.BUTTON2) && mouseDragging) {
            mouseDragging = false;
            if (!keyDragging) {
                app.glassPane.setCursor(previousCursor);
            }
        }
    }

    // KeyEventDispatcher

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_SPACE) && app.isFocused()) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if ((e.getWhen() - lastReleased) < KEY_REPEAT_GUARD_TIME) {
                    timer.stop();
                    return true;
                } else if (!keyDragging) {
                    Point mouseLocOnScreen = MouseInfo.getPointerInfo().getLocation();
                    Point scrollPaneLocOnScreen = app.view.getLocationOnScreen();
                    Rectangle viewBoundsOnScreen = app.view.getBounds();
                    viewBoundsOnScreen.translate(scrollPaneLocOnScreen.x, scrollPaneLocOnScreen.y);
                    if (!viewBoundsOnScreen.contains(mouseLocOnScreen)) {
                        // The mouse cursor is not over the view
                        return false;
                    }

                    if (!mouseDragging) {
                        previousLocation = mouseLocOnScreen;

                        previousCursor = app.glassPane.getCursor();
                        app.glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }

                    keyDragging = true;
                    return true;
                }
            } else if ((e.getID() == KeyEvent.KEY_RELEASED) && keyDragging) {
                lastReleased = e.getWhen();
                timer.start();
                return true;
            }
        }
        return false;
    }

    private final App app;
    private final Timer timer = new Timer(KEY_REPEAT_GUARD_TIME, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            keyDragging = false;
            if (!mouseDragging) {
                app.glassPane.setCursor(previousCursor);
            }
        }
    });

    private Point previousLocation;
    private boolean mouseDragging, keyDragging;
    private Cursor previousCursor;
    private long lastReleased;

    /**
     * The number of milliseconds between key press and release events below
     * which they will be considered automatic repeats
     */
    private static final int KEY_REPEAT_GUARD_TIME = 10;
}
