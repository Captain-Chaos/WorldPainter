/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author pepijn
 */
@SuppressWarnings("unused") // Used at compile time by EventVO.get- and setAttribute
public final class AttributeKeyVO<T> implements Serializable {
    public AttributeKeyVO(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.key != null ? this.key.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AttributeKeyVO<?> other = (AttributeKeyVO<?>) obj;
        return Objects.equals(this.key, other.key);
    }

    @Override
    public String toString() {
        return key;
    }
    
    private final String key;
    
    private static final long serialVersionUID = 1L;
}