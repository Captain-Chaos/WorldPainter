/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.HashMap;
import java.util.Map;

/**
 * A superclass that invokes a listener whenever an instance is created.
 * 
 * @author pepijn
 */
public class InstanceKeeper {
    public InstanceKeeper() {
        @SuppressWarnings("unchecked")
        InstantiationListener<Object> listener = (InstantiationListener<Object>) listeners.get(getClass());
        if (listener != null) {
            listener.objectInstaniated(this);
        }
    }
    
    public static <T> void setInstantiationListener(Class<T> type, InstantiationListener<T> listener) {
        listeners.put(type, listener);
    }
    
    private static Map<Class<?>, InstantiationListener<?>> listeners = new HashMap<>();
    
    public static interface InstantiationListener<T> {
        void objectInstaniated(T object);
    }
}