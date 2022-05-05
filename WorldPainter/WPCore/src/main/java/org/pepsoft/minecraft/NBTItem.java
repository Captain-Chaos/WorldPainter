/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.Tag;

import java.util.Map;

/**
 *
 * @author pepijn
 */
public interface NBTItem {
    Tag toNBT();
    default Map<DataType, ? extends Tag> toMultipleNBT() {
        throw new UnsupportedOperationException();
    }
}