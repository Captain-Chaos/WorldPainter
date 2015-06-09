package org.pepsoft.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Pepijn Schmitz on 09-06-15.
 */
public class MathUtilsTest {
    @Test
    public void testDoubleMod() {
        assertEquals(350.0, MathUtils.mod(-730.0, 360.0), 0.0);
        assertEquals(  0.0, MathUtils.mod(-720.0, 360.0), 0.0);
        assertEquals( 10.0, MathUtils.mod(-710.0, 360.0), 0.0);
        assertEquals(350.0, MathUtils.mod(-370.0, 360.0), 0.0);
        assertEquals(  0.0, MathUtils.mod(-360.0, 360.0), 0.0);
        assertEquals( 10.0, MathUtils.mod(-350.0, 360.0), 0.0);
        assertEquals(350.0, MathUtils.mod( -10.0, 360.0), 0.0);
        assertEquals(  0.0, MathUtils.mod(   0.0, 360.0), 0.0);
        assertEquals( 10.0, MathUtils.mod(  10.0, 360.0), 0.0);
        assertEquals(350.0, MathUtils.mod( 350.0, 360.0), 0.0);
        assertEquals(  0.0, MathUtils.mod( 360.0, 360.0), 0.0);
        assertEquals( 10.0, MathUtils.mod( 370.0, 360.0), 0.0);
        assertEquals(350.0, MathUtils.mod( 710.0, 360.0), 0.0);
        assertEquals(  0.0, MathUtils.mod( 720.0, 360.0), 0.0);
        assertEquals( 10.0, MathUtils.mod( 730.0, 360.0), 0.0);
    }
}