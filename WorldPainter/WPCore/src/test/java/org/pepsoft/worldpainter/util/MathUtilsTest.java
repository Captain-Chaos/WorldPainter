package org.pepsoft.worldpainter.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilsTest {
    @Test
    public void getAngle() {
        assertEquals(0.0,             MathUtils.getAngle( 100,    0), Double.MIN_VALUE);
        assertEquals(Math.PI / 4,     MathUtils.getAngle( 100,  100), Double.MIN_VALUE);
        assertEquals(Math.PI / 2,     MathUtils.getAngle(   0,  100), Double.MIN_VALUE);
        assertEquals(Math.PI * 3 / 4, MathUtils.getAngle(-100,  100), Double.MIN_VALUE);
        assertEquals(Math.PI,         MathUtils.getAngle(-100,    0), Double.MIN_VALUE);
        assertEquals(Math.PI * 5 / 4, MathUtils.getAngle(-100, -100), Double.MIN_VALUE);
        assertEquals(Math.PI * 3 / 2, MathUtils.getAngle(   0, -100), Double.MIN_VALUE);
        assertEquals(Math.PI * 7 / 4, MathUtils.getAngle( 100, -100), Double.MIN_VALUE);
    }
}