package org.pepsoft.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionTest {
    @Test
    public void testParse() {
        assertEquals(new Version(1), Version.parse("1"));
        assertEquals(new Version(1, 2, 3), Version.parse("1.2.3"));
        assertEquals(new Version(1, "SNAPSHOT"), Version.parse("1-SNAPSHOT"));
        assertEquals(new Version(1, 2, 3, "SNAPSHOT"), Version.parse("1.2.3-SNAPSHOT"));
    }

    @Test
    public void testCompareTo() {
        assertEquals(0, new Version(1).compareTo(new Version(1)));
        assertEquals(0, new Version(1, 0, 0).compareTo(new Version(1, 0, 0)));
        assertEquals(0, new Version(1).compareTo(new Version(1, 0, 0)));
        assertEquals(0, new Version(1, 0, 0).compareTo(new Version(1)));

        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(1, 0, 0, "SNAPSHOT")));
        assertEquals(1, new Version(1, 0, 0, "SNAPSHOT").compareTo(new Version(1, 0, 0)));
        assertEquals(0, new Version(1, 0, 0, "SNAPSHOT").compareTo(new Version(1, 0, 0, "SNAPSHOT")));
        assertEquals(-1, new Version(1).compareTo(new Version(1, 0, 0, "SNAPSHOT")));
        assertEquals(1, new Version(1, 0, 0, "SNAPSHOT").compareTo(new Version(1)));
        assertEquals(0, new Version(1, 0, 0, "SNAPSHOT").compareTo(new Version(1, "SNAPSHOT")));

        assertEquals(-1, new Version(0, 9, 9).compareTo(new Version(1)));
        assertEquals(-1, new Version(0, 9, 9).compareTo(new Version(1, 0, 0)));
        assertEquals(1, new Version(1, 0, 1).compareTo(new Version(1)));
        assertEquals(1, new Version(1, 0, 1).compareTo(new Version(1, 0, 0)));
        assertEquals(1, new Version(2).compareTo(new Version(1)));
        assertEquals(1, new Version(2).compareTo(new Version(1, 0, 0)));
        assertEquals(1, new Version(2, 0, 0).compareTo(new Version(1, 0, 0)));
        assertEquals(-1, new Version(1).compareTo(new Version(2)));
        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(2)));
        assertEquals(-1, new Version(1, 0, 0).compareTo(new Version(2, 0, 0)));
    }
}