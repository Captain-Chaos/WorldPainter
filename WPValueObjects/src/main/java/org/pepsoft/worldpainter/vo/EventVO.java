/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toMap;

/**
 *
 * @author pepijn
 */
public final class EventVO implements Serializable {
    public EventVO(String key) {
        this.key = key;
    }
    
    public EventVO count(long count) {
        setAttribute(ATTRIBUTE_COUNT, count);
        return this;
    }
    
    public EventVO duration(long duration) {
        setAttribute(ATTRIBUTE_DURATION, duration);
        return this;
    }

    public EventVO addTimestamp() {
        setAttribute(ATTRIBUTE_TIMESTAMP, new Date());
        return this;
    }

    public String getKey() {
        return key;
    }

    public Map<String, Serializable> getAttributes() {
        return (attributes != null) ? attributes.entrySet().stream().collect(toMap(entry -> entry.getKey().getKey(), Entry::getValue)) : null;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = (attributes != null) ? attributes.entrySet().stream().collect(toMap(entry -> new AttributeKeyVO<>(entry.getKey()), Entry::getValue)) : null;
    }

    public <T extends Serializable> EventVO setAttribute(AttributeKeyVO<T> key, T value) {
        if (value != null) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key, value);
        } else if ((attributes != null) && attributes.containsKey(key)) {
            attributes.remove(key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        }
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(AttributeKeyVO<T> key) {
        if (attributes != null) {
            return (T) attributes.get(key);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "EventVO{" + "key=" + key + ", attributes=" + attributes + '}';
    }
    
    private final String key;
    private Map<AttributeKeyVO<? extends Serializable>, Serializable> attributes;
    
    public static final AttributeKeyVO<Long> ATTRIBUTE_COUNT = new AttributeKeyVO<>("count");
    public static final AttributeKeyVO<Long> ATTRIBUTE_DURATION = new AttributeKeyVO<>("duration");
    public static final AttributeKeyVO<Date> ATTRIBUTE_TIMESTAMP = new AttributeKeyVO<>("timestamp");

    private static final long serialVersionUID = 1L;
}