/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.List;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author pepijn
 */
public class WPContextProvider {
    public static synchronized  WPContext getWPContext() {
        if (wpContext == null) {
            List<ContextProvider> contextProviders = WPPluginManager.getInstance().getPlugins(ContextProvider.class);
            if (contextProviders.isEmpty()) {
                throw new RuntimeException("No context providers found!");
            } else if (contextProviders.size() > 1) {
                throw new RuntimeException("Multiple context providers found!");
            }
            wpContext = contextProviders.get(0).getWPContextInstance();
        }
        return wpContext;
    }
    
    private static WPContext wpContext;
}