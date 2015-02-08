/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.undo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pepijn
 */
public final class Snapshot {
    Snapshot(UndoManager undoManager, int frame) {
        this.undoManager = undoManager;
        this.frame = frame;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getBuffer(BufferKey<T> key) {
        if (frame == -1) {
            throw new IllegalStateException("Undo history frame no longer available");
        }
        if (bufferCache.containsKey(key)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Getting buffer " + key + " for reading from buffer cache");
            }
            return (T) bufferCache.get(key);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Getting buffer " + key + " for reading from history");
            }
            T buffer = undoManager.findMostRecentCopy(key, frame);
            bufferCache.put(key, buffer);
            return buffer;
        }
    }
    
    private final UndoManager undoManager;
    int frame;
    private final Map<BufferKey<?>, Object> bufferCache = new HashMap<BufferKey<?>, Object>();
    
    private static final Logger logger = Logger.getLogger(Snapshot.class.getName());
}