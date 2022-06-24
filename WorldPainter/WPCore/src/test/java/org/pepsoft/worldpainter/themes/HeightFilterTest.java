/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import org.junit.Assert;
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
        HeightFilter filter = new HeightFilter(0, 32, 10, 20, true);
        for (int z = 0; z < 32; z++) {
            if (z < 8) {
                Assert.assertEquals(0, filter.getLevel(0, 0, z, 15));
            } else if (z < 12) {
                Assert.assertTrue(filter.getLevel(0, 0, z, 15) > 0 && filter.getLevel(0, 0, z, 15) < 15);
            } else if (z < 19) {
                Assert.assertEquals(15, filter.getLevel(0, 0, z, 15));
            } else if (z < 23) {
                Assert.assertTrue(filter.getLevel(0, 0, z, 15) > 0 && filter.getLevel(0, 0, z, 15) < 15);
            } else {
                Assert.assertEquals(0, filter.getLevel(0, 0, z, 15));
            }
        }
        filter = new HeightFilter(0, 32, 0, 31, false);
        for (int z = 0; z < 32; z++) {
            Assert.assertEquals(15, filter.getLevel(0, 0, z, 15));
        }
        filter = new HeightFilter(0, 32, 10, 20, false);
        for (int z = 0; z < 32; z++) {
            if (z < 10) {
                Assert.assertEquals(0, filter.getLevel(0, 0, z, 15));
            } else if (z <= 20) {
                Assert.assertEquals(15, filter.getLevel(0, 0, z, 15));
            } else {
                Assert.assertEquals(0, filter.getLevel(0, 0, z, 15));
            }
        }
    }
}