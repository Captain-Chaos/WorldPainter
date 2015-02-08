/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author pepijn
 */
public class Test {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            int n = 1 << i;
            int exp = (int) (Math.log(n) / Math.log(2));
            System.out.println(n + " = 2^" + exp);
        }
    }
}
