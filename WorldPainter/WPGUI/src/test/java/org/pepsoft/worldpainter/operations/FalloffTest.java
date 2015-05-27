/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.util.Random;
import org.junit.Test;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;

import static org.junit.Assert.*;

/**
 *
 * @author pepijn
 */
public class FalloffTest {
    @Test
    public void testNoisyStrength() {
        final float strengthRef[] = new float[1];
        strengthRef[0] = 0.0f;
        SymmetricBrush fallOff = new SymmetricBrush("Test Falloff", BrushShape.CIRCLE, true) {
            @Override
            public float calcStrength(int dx, int dy) {
                return strengthRef[0];
            }
        };
        fallOff.setRadius(1);
        assertEquals(0.0, fallOff.getNoisyStrength(0, 0, 0, 0, 0), 0.0);

        Random random = new Random(0);
        for (int i = 0; i < 100; i++) {
            strengthRef[0] = random.nextFloat();
            fallOff.setRadius(2); fallOff.setRadius(1); // Flush the caches
            double noisyStrength = fallOff.getNoisyStrength(0, 0, 0, 0, 0);
            assertTrue(noisyStrength >= 0.0);
            assertTrue(noisyStrength <= 1.0);
        }

        strengthRef[0] = 1.0f;
        fallOff.setRadius(2); fallOff.setRadius(1); // Flush the caches
        assertEquals(1.0, fallOff.getNoisyStrength(0, 0, 0, 0, 0), 0.0);

        fallOff = SymmetricBrush.LINEAR_CIRCLE;
        int radius = 100;
        fallOff.setRadius(radius);
        for (int i = 0; i < 100; i++) {
            long seed = random.nextLong();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    double distance = Math.sqrt(x * x + y * y);
                    double strength = fallOff.getNoisyStrength(seed, 0, 0, x, y);
                    if (distance >= radius) {
                        assertEquals("round " + i + "; seed " + seed, 0.0, strength, 0.0);
                    } else if ((x == 0) && (y == 0)) {
                        assertEquals("round " + i + "; seed " + seed, 1.0, strength, 0.0);
                    } else {
                        assertTrue("round " + i + "; seed " + seed, strength >= 0.0);
                        assertTrue("round " + i + "; seed " + seed, strength <= 1.0);
                    }
                }
            }
        }
    }
}