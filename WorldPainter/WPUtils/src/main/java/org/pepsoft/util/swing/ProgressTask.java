/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import org.pepsoft.util.ProgressReceiver;

/**
 *
 * @author pepijn
 */
public interface ProgressTask<T> {
    String getName();
    
    T execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled;
}