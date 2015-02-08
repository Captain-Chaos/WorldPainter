/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.BitSet;

/**
 *
 * @author pepijn
 */
public final class CollectionUtils {
    private CollectionUtils() {
        // Prevent instantiation
    }
    
    public static BitSet bitSetOf(int... values) {
        BitSet bitSet = new BitSet(values[values.length - 1] + 1);
        for (int value: values) {
            bitSet.set(value);
        }
        return bitSet;
    }
}