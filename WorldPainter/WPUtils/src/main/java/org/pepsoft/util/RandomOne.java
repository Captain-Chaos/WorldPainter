/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.util.Random;

/**
 *
 * @author pepijn
 */
public final class RandomOne {
    private RandomOne() {
        // Prevent instantiation
    }
    
    @SafeVarargs
    public static <T> T of(T... objects) {
        return objects[RANDOM.nextInt(objects.length)];
    }
    
    @SafeVarargs
    public static <T> T of(Random random, T... objects) {
        return objects[random.nextInt(objects.length)];
    }
    
    private static final Random RANDOM = new Random();
}