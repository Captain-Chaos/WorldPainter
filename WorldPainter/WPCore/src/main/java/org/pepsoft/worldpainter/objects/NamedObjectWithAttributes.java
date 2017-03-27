package org.pepsoft.worldpainter.objects;

import org.pepsoft.util.AttributeKey;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pepijn on 9-3-2017.
 */
public abstract class NamedObjectWithAttributes extends AbstractObject {
    protected NamedObjectWithAttributes() {
        // Do nothing
    }

    protected NamedObjectWithAttributes(String name) {
        this.name = name;
    }

    protected NamedObjectWithAttributes(String name, Map<String, Serializable> attributes) {
        this.name = name;
        this.attributes = ((attributes != null) && (! attributes.isEmpty())) ? new HashMap<>(attributes) : null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = ((attributes != null) && (! attributes.isEmpty())) ? new HashMap<>(attributes) : null;
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        if ((value == null) ? (key.defaultValue == null) : value.equals(key.defaultValue)) {
            // Default value being set
            if (attributes != null) {
                attributes.remove(key.key);
                if (attributes.isEmpty()) {
                    attributes = null;
                }
            }
        } else {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key.key, value);
        }
    }

    private String name;
    private Map<String, Serializable> attributes;

    private static final long serialVersionUID = 1L;
}