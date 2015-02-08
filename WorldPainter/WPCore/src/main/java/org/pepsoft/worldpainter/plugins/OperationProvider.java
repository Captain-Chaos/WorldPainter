/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import java.util.List;
import org.pepsoft.worldpainter.operations.Operation;

/**
 *
 * @author pepijn
 */
public interface OperationProvider extends Plugin {
    List<Operation> getOperations();
}