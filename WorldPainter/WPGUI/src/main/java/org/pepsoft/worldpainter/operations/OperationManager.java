/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pepsoft.worldpainter.plugins.OperationProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author pepijn
 */
public class OperationManager {
    private OperationManager() {
        operations = new ArrayList<>();
        List<OperationProvider> operationProviders = WPPluginManager.getInstance().getPlugins(OperationProvider.class);
        for (OperationProvider operationProvider: operationProviders) {
            operations.addAll(operationProvider.getOperations());
        }
        for (Operation operation: operations) {
            operationsByName.put(operation.getName(), operation);
        }
    }
    
    public List<Operation> getOperations() {
        return Collections.unmodifiableList(operations);
    }
    
    public Operation getOperation(String name) {
        return operationsByName.get(name);
    }
    
    public static OperationManager getInstance() {
        return INSTANCE;
    }
    
    private final List<Operation> operations;
    private final Map<String, Operation> operationsByName = new HashMap<>();
    
    private static final OperationManager INSTANCE = new OperationManager();
}