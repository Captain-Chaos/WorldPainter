package org.pepsoft.util;

import java.util.Observable;

/**
 * Created by Pepijn Schmitz on 21-12-16.
 */
public class ObservableBoolean extends Observable {
    public ObservableBoolean() {
        // Do nothing
    }

    public ObservableBoolean(boolean value) {
        this.value = value;
    }

    public synchronized void setValue(boolean value) {
        if (value != this.value) {
            this.value = value;
            setChanged();
            notifyObservers(value);
        }
    }

    public synchronized boolean getValue() {
        return value;
    }

    private boolean value;
}