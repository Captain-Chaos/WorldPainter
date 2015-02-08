/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import org.junit.Test;

/**
 *
 * @author SchmitzP
 */
public class HeightFilterTest {
    /**
     * Test of getLevel method, of class HeightFilter.
     */
    @Test
    public void testGetLevel() {
        HeightFilter filter = new HeightFilter(32, 10, 20, true);
        for (int z = 0; z < 32; z++) {
            System.out.printf("Level %3d: %2d%n", z, filter.getLevel(0, 0, z, 15));
        }
        filter = new HeightFilter(32, 0, 31, false);
        for (int z = 0; z < 32; z++) {
            System.out.printf("Level %3d: %2d%n", z, filter.getLevel(0, 0, z, 15));
        }
        filter = new HeightFilter(32, 10, 20, false);
        for (int z = 0; z < 32; z++) {
            System.out.printf("Level %3d: %2d%n", z, filter.getLevel(0, 0, z, 15));
        }
    }
}