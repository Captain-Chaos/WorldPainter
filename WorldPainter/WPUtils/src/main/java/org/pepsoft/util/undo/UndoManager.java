/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util.undo;

import org.pepsoft.util.MemoryUtils;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

import static org.pepsoft.util.ObjectUtils.copyObject;

/**
 * A data buffer oriented (rather than edit list or operations oriented) manager
 * of undo and redo data. Uses a copy on write mechanism to maintain a list of
 * historical versions of a set of data buffers, copying them automatically when
 * needed after a save point has been executed, and notifying clients when
 * buffers need to be updated because an undo or redo has been performed.
 *
 * @author pepijn
 */
public class UndoManager {
    public UndoManager() {
        this(null, null, DEFAULT_MAX_FRAMES);
    }

    public UndoManager(int maxFrames) {
        this(null, null, maxFrames);
    }
    
    public UndoManager(Action undoAction, Action redoAction) {
        this(undoAction, redoAction, DEFAULT_MAX_FRAMES);
    }

    public UndoManager(Action undoAction, Action redoAction, int maxFrames) {
        this.maxFrames = maxFrames;
        history.add(new WeakHashMap<>());
        registerActions(undoAction, redoAction);
    }

    public void registerActions(Action undoAction, Action redoAction) {
        this.undoAction = undoAction;
        this.redoAction = redoAction;
        updateActions();
    }

    public void unregisterActions() {
        disableActions();
        undoAction = null;
        redoAction = null;
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    /**
     * Arm a save point. It will be executed the next time a buffer is requested
     * for editing. Arming a save point instead of executing it immediately
     * allows a redo to be performed instead.
     * 
     * <p>Will do nothing if a save point is already armed, or if the current
     * frame is the last one and it is not dirty.
     */
    public void armSavePoint() {
        if ((! savePointArmed) /*&& ((currentFrame < (history.size() - 1)) || isDirty())*/) {
            savePointArmed = true;
            listeners.forEach(UndoListener::savePointArmed);
            if (logger.isDebugEnabled()) {
                logger.debug("Save point armed");
            }
        }
    }

    /**
     * Save the current state of all buffers as an undo point.
     */
    public void savePoint() {
        clearRedo();

        // Add a new frame
        history.add(new WeakHashMap<>());
        
        // Update the current frame pointer
        currentFrame++;

        // If the max undos has been reached, throw away the oldest
        pruneHistory();

        // Clear cache
        writeableBufferCache.clear();

        savePointArmed = false;

        listeners.forEach(UndoListener::savePointCreated);
        
        updateActions();

        if (logger.isDebugEnabled()) {
            logger.debug("Save point set; new current frame: " + currentFrame);
            if (logger.isTraceEnabled()) {
                dumpBuffer();
            }
        }
    }
    
    /**
     * Get a read-only snapshot of the current state of the buffers. If you want
     * the state to be a static snapshot that will not reflect later changes,
     * you should execute a save point after getting the snapshot. The snapshot
     * will remain valid until the corresponding undo history frame disappears,
     * after which it will throw an exception if you try to use it.
     * 
     * @return A snapshot of the current undo history frame.
     */
    public Snapshot getSnapshot() {
        Snapshot snapshot = new Snapshot(this, currentFrame);
        snapshots.add(new WeakReference<>(snapshot));
        return snapshot;
    }

    /**
     * Indicates whether the current history frame is dirty (meaning that
     * buffers have been checked out for editing from it).
     * 
     * @return <code>true</code> if the current history frame is dirty.
     */
    public boolean isDirty() {
        return ! writeableBufferCache.isEmpty();
    }

    /**
     * Rolls back all buffers to the previous save point, if there is one still
     * available.
     *
     * @return <code>true</code> if the undo was succesful.
     */
    public boolean undo() {
        if (currentFrame > 0) {
            currentFrame--;
            readOnlyBufferCache.clear();
            writeableBufferCache.clear();
            listeners.forEach(UndoListener::undoPerformed);
            Map<BufferKey<?>, Object> previousHistoryFrame = history.get(currentFrame + 1);
            for (BufferKey<?> key: previousHistoryFrame.keySet()) {
                UndoListener listener = keyListeners.get(key);
                if (listener != null) {
                    listener.bufferChanged(key);
                }
            }
            updateActions();
            if (logger.isDebugEnabled()) {
                logger.debug("Undo requested; now at frame " + currentFrame + " (total: " + history.size() + ")");
                if (logger.isTraceEnabled()) {
                    dumpBuffer();
                }
            }
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Undo requested, but no more frames available");
            }
            return false;
        }
    }

    /**
     * Rolls forward all buffers to the next save point, if there is one
     * available, and no edits have been performed since the last undo.
     *
     * @return <code>true</code> if the redo was succesful.
     */
    public boolean redo() {
        if (currentFrame < (history.size() - 1)) {
            currentFrame++;
            readOnlyBufferCache.clear();
            writeableBufferCache.clear();
            listeners.forEach(UndoListener::redoPerformed);
            Map<BufferKey<?>, Object> currentHistoryFrame = history.get(currentFrame);
            for (BufferKey<?> key: currentHistoryFrame.keySet()) {
                UndoListener listener = keyListeners.get(key);
                if (listener != null) {
                    listener.bufferChanged(key);
                }
            }
            updateActions();
            if (logger.isDebugEnabled()) {
                logger.debug("Redo requested; now at frame " + currentFrame + " (total: " + history.size() + ")");
                if (logger.isTraceEnabled()) {
                    dumpBuffer();
                }
            }
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Redo requested, but no more frames available");
            }
            return false;
        }
    }

    /**
     * Throw away all undo and redo information.
     */
    public void clear() {
        clearRedo();
        
        int deletedFrames = 0;
        while (history.size() > 1) {
            shrinkHistory();
            deletedFrames++;
        }
        updateSnapshots(-deletedFrames);
        updateActions();
        savePointArmed = false;
        if (logger.isTraceEnabled()) {
            dumpBuffer();
        }
    }

    /**
     * Throw away all redo information
     */
    public void clearRedo() {
        // Make sure there is no history after the current frame (which there
        // might be if an undo has been performed)
        if (currentFrame < (history.size() - 1)) {
            do {
                history.removeLast();
            } while (currentFrame < (history.size() - 1));
            updateSnapshots(0);
            updateActions();
        }
    }

    public <T> void addBuffer(BufferKey<T> key, T buffer) {
        addBuffer(key, buffer, null);
    }

    public <T> void addBuffer(BufferKey<T> key, T buffer, UndoListener listener) {
        clearRedo();
        
        history.getLast().put(key, buffer);
        writeableBufferCache.put(key, buffer);
        if (listener != null) {
            keyListeners.put(key, listener);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Buffer added: " + key);
        }
    }

    public void removeBuffer(BufferKey<?> key) {
        writeableBufferCache.remove(key);
        readOnlyBufferCache.remove(key);
        for (Map<BufferKey<?>, Object> historyFrame: history) {
            historyFrame.remove(key);
        }
        keyListeners.remove(key);
        if (logger.isTraceEnabled()) {
            logger.trace("Buffer removed: " + key);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBuffer(BufferKey<T> key) {
        if (writeableBufferCache.containsKey(key)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting buffer " + key + " for reading from writeable buffer cache");
            }
            return (T) writeableBufferCache.get(key);
        } else if (readOnlyBufferCache.containsKey(key)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting buffer " + key + " for reading from read-only buffer cache");
            }
            return (T) readOnlyBufferCache.get(key);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting buffer " + key + " for reading from history");
            }
            T buffer = findMostRecentCopy(key);
            readOnlyBufferCache.put(key, buffer);
            return buffer;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBufferForEditing(BufferKey<T> key) {
        if (savePointArmed) {
            savePoint();
        }
        if (writeableBufferCache.containsKey(key)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting buffer " + key + " for writing from writeable buffer cache");
            }
            return (T) writeableBufferCache.get(key);
        } else {
            clearRedo();
            if (readOnlyBufferCache.containsKey(key)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Copying buffer " + key + " for writing from read-only buffer cache");
                }
                T buffer = (T) readOnlyBufferCache.remove(key);
                T copy = copyObject(buffer);
                history.getLast().put(key, copy);
                writeableBufferCache.put(key, copy);
                return copy;
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Copying buffer " + key + " for writing from history");
                }
                Map<BufferKey<?>, Object> currentHistoryFrame = history.getLast();
                if (currentHistoryFrame.containsKey(key)) {
                    // TODO: this should never happen. Remove?
                    T buffer = (T) currentHistoryFrame.get(key);
                    writeableBufferCache.put(key, buffer);
                    return buffer;
                } else {
                    // The buffer does not exist in the current history frame yet. Copy
                    // it.
                    T buffer = findMostRecentCopy(key);
                    T copy = copyObject(buffer);
                    currentHistoryFrame.put(key, copy);
                    writeableBufferCache.put(key, copy);
                    return copy;
                }
            }
        }
    }

    public void addListener(UndoListener listener) {
        if (logger.isTraceEnabled()) {
            logger.trace("Adding listener " + listener);
        }
        listeners.add(listener);
    }

    public void removeListener(UndoListener listener) {
        if (logger.isTraceEnabled()) {
            logger.trace("Removing listener " + listener);
        }
        listeners.remove(listener);
    }

    public Class<?>[] getStopAtClasses() {
        return stopAt.toArray(new Class<?>[stopAt.size()]);
    }

    public void setStopAtClasses(Class<?>... stopAt) {
        this.stopAt = new HashSet<>(Arrays.asList(stopAt));
    }
    
    public int getDataSize() {
        return MemoryUtils.getSize(history, stopAt);
    }
    
    private void updateSnapshots(int delta) {
        if (logger.isDebugEnabled()) {
            logger.debug("Updating snapshots");
        }
        int frameCount = history.size();
        for (Iterator<Reference<Snapshot>> i = snapshots.iterator(); i.hasNext(); ) {
            Snapshot snapshot = i.next().get();
            if (snapshot == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing garbage collected snapshot");
                }
                i.remove();
            } else {
                snapshot.frame += delta;
                if ((snapshot.frame < 0) || (snapshot.frame >= frameCount)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Disabling and removing snapshot with invalid frame reference");
                    }
                    snapshot.frame = -1;
                    i.remove();
                }
            }
        }
    }
    
    private void pruneHistory() {
        int deletedFrames = 0;
        while (history.size() > maxFrames) {
            shrinkHistory();
            deletedFrames++;
        }
        if (deletedFrames > 0) {
            updateSnapshots(-deletedFrames);
        }
        if (logger.isTraceEnabled()) {
            dumpBuffer();
        }
    }
    
    private void shrinkHistory() {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing oldest history frame; moving contents to next oldest frame");
        }
        
        // Remove oldest frame
        Map<BufferKey<?>, Object> oldestFrame = history.removeFirst();

        // Move all buffers from the previous oldest frame to the new
        // oldest frame, except the ones that already exist
        Map<BufferKey<?>, Object> nextOldestFrame = history.getFirst();
        oldestFrame.entrySet().stream()
            .filter(entry -> !nextOldestFrame.containsKey(entry.getKey()))
            .forEach(entry -> nextOldestFrame.put(entry.getKey(), entry.getValue()));
        
        if (currentFrame > 0) {
            currentFrame--;
        }
    }

    private <T> T findMostRecentCopy(BufferKey<T> key) {
        return findMostRecentCopy(key, currentFrame);
    }
    
    @SuppressWarnings("unchecked")
    <T> T findMostRecentCopy(BufferKey<T> key, int frame) {
        for (ListIterator<Map<BufferKey<?>, Object>> i = history.listIterator(frame + 1); i.hasPrevious(); ) {
            Map<BufferKey<?>, Object> historyFrame = i.previous();
            if (historyFrame.containsKey(key)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Most recent copy of buffer " + key + " found in frame " + frame + " of history");
                }
                return (T) historyFrame.get(key);
            }
            frame--;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Buffer " + key + " not present in undo history");
        }
        return null;
    }

    private void dumpBuffer() {
        int index = 0;
        long totalDataSize = 0;
        for (Map<BufferKey<?>, Object> frame: history) {
            int frameSize = MemoryUtils.getSize(frame, stopAt);
            totalDataSize += frameSize;
            logger.debug(((index == currentFrame) ? "* " : "  ") + " " + ((index < 10) ? "0" : "") + index + ": " + frame.size() + " buffers (size: " + (frameSize / 1024) + " KB)");
            index++;
        }
        logger.debug("   Total data size: " + (totalDataSize / 1024) + " KB");
    }
    
    private void updateActions() {
        if (undoAction != null) {
            undoAction.setEnabled(currentFrame > 0);
        }
        if (redoAction != null) {
            redoAction.setEnabled(currentFrame < (history.size() - 1));
        }
    }

    private void disableActions() {
        if (undoAction != null) {
            undoAction.setEnabled(false);
        }
        if (redoAction != null) {
            redoAction.setEnabled(false);
        }
    }

    private Action undoAction, redoAction;
    private final int maxFrames;
    private final LinkedList<Map<BufferKey<?>, Object>> history = new LinkedList<>();
    private int currentFrame;
    private final Map<BufferKey<?>, Object> readOnlyBufferCache = new WeakHashMap<>();
    private final Map<BufferKey<?>, Object> writeableBufferCache = new WeakHashMap<>();
    private final List<UndoListener> listeners = new ArrayList<>();
    private final Map<BufferKey<?>, UndoListener> keyListeners = new WeakHashMap<>();
    private boolean savePointArmed;
    private final Set<Reference<Snapshot>> snapshots = new HashSet<>();
    private Set<Class<?>> stopAt;

    private static final int DEFAULT_MAX_FRAMES = 25;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UndoManager.class);
}