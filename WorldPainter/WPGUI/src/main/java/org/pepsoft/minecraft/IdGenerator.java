/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

/**
 *
 * @author pepijn
 */
public final class IdGenerator {
    private IdGenerator() {
        // Prevent instantiation
    }

    public static synchronized String getNextId() {
        return Integer.toString(nextId++);
    }

    private static int nextId = 0;
}