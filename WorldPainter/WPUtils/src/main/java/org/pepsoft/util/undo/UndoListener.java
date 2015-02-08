/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util.undo;

/**
 *
 * @author pepijn
 */
public interface UndoListener {
    /**
     * A save point has been scheduled. The listener should re-get all its
     * buffers when it wants to edit them.
     */
    void savePointArmed();

    /**
     * A save point has been performed. The listener should re-get all its
     * buffers when it wants to edit them.
     */
    void savePointCreated();

    /**
     * An undo has been performed.
     */
    void undoPerformed();

    /**
     * A redo has been performed.
     */
    void redoPerformed();

    /**
     * A buffer has changed due to an undo or redo operation. The listener
     * should re-get it.
     *
     * @param key The key of the buffer that has changed.
     */
    void bufferChanged(BufferKey<?> key);
}