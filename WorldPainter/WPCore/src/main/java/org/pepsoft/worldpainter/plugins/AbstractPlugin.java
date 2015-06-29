/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author pepijn
 */
public abstract class AbstractPlugin implements Plugin {
    public AbstractPlugin(String name, String version) {
        this(name, version, null);
    }
    
    public AbstractPlugin(String name, String version, UUID uuid) {
        properties.setProperty(PROPERTY_NAME, name);
        properties.setProperty(PROPERTY_VERSION, version);
        if (uuid != null) {
            properties.setProperty(PROPERTY_UUIDS, uuid.toString());
        }
    }
    
    @Override
    public final String getName() {
        return properties.getProperty(PROPERTY_NAME);
    }

    @Override
    public final String getVersion() {
        return properties.getProperty(PROPERTY_VERSION);
    }

    @Override
    public final Set<UUID> getUUIDs() {
        String uuidsStr = properties.getProperty(PROPERTY_UUIDS);
        if (uuidsStr != null) {
            Set<UUID> uuids = new HashSet<>();
            String[] uuidsStrs = uuidsStr.split(",");
            for (String uuidStr: uuidsStrs) {
                uuids.add(UUID.fromString(uuidStr.trim()));
            }
            return uuids;
        } else {
            return null;
        }
    }

    @Override
    public final String getMinimumWorldPainterVersion() {
        return properties.getProperty(PROPERTY_MINIMUM_WORLDPAINTER_VERSION);
    }

    @Override
    public final Properties getProperties() {
        return (Properties) properties.clone();
    }
    
    private final Properties properties = new Properties();
}