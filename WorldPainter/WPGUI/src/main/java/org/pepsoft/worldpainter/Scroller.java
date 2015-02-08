/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JScrollBar;
import javax.swing.Timer;

/**
 *
 * @author pepijn
 */
public class Scroller implements ActionListener {
    public void attach(JScrollBar scrollBar) {
        this.scrollBar = scrollBar;
        state = State.PAUSING_AT_START;
        pauseStart = System.currentTimeMillis();
        timer = new Timer(100, this);
        timer.start();
    }
    
    public void detach() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        scrollBar = null;
    }

    public long getPauseAtEnd() {
        return pauseAtEnd;
    }

    public void setPauseAtEnd(long pauseAtEnd) {
        this.pauseAtEnd = pauseAtEnd;
    }

    public long getPauseAtStart() {
        return pauseAtStart;
    }

    public void setPauseAtStart(long pauseAtStart) {
        this.pauseAtStart = pauseAtStart;
    }
    
    // ActionListener
    
    @Override
    public void actionPerformed(ActionEvent e) {
        boolean valueIsAdjusting = scrollBar.getValueIsAdjusting();
        int current = scrollBar.getValue(), max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
        long now = System.currentTimeMillis();
        switch (state) {
            case PAUSING_AT_START:
                if (valueIsAdjusting) {
                    state = State.VALUE_IS_ADJUSTING;
                } else if ((now - pauseStart) > pauseAtStart) {
                    state = State.SCROLLING;
                }
                break;
            case SCROLLING:
                if (valueIsAdjusting) {
                    state = State.VALUE_IS_ADJUSTING;
                } else if (current < max) {
                    scrollBar.setValue(current + 1);
                } else {
                    state = State.PAUSING_AT_END;
                    pauseStart = now;
                }
                break;
            case VALUE_IS_ADJUSTING:
                if (! valueIsAdjusting) {
                    state = State.PAUSING_AT_START;
                    pauseStart = now;
                }
                break;
            case PAUSING_AT_END:
                if (valueIsAdjusting) {
                    state = State.VALUE_IS_ADJUSTING;
                } else if ((now - pauseStart) > pauseAtEnd) {
                    scrollBar.setValue(0);
                    state = State.PAUSING_AT_START;
                    pauseStart = now;
                }
                break;
            default:
                throw new InternalError();
        }
    }
    
    private JScrollBar scrollBar;
    private Timer timer;
    private long pauseStart, pauseAtStart = DEFAULT_PAUSE_AT_START, pauseAtEnd = DEFAULT_PAUSE_AT_END;
    private State state;
    
    public static final int DEFAULT_PAUSE_AT_START = 5000, DEFAULT_PAUSE_AT_END = 3000;
    
    private enum State {PAUSING_AT_START, SCROLLING, VALUE_IS_ADJUSTING, PAUSING_AT_END}
}