package org.pepsoft.util;

import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.pepsoft.util.CollectionUtils.listOf;

public class CollectionUtilsTest {
    @Test
    public void testListOf() {
        List<String> result = listOf(singletonList("een"), asList("twee", "drie"), asList("vier", "vijf", "zes"), emptyList());
        assertEquals(6, result.size());
        assertEquals("een", result.get(0));
        assertEquals("twee", result.get(1));
        assertEquals("drie", result.get(2));
        assertEquals("vier", result.get(3));
        assertEquals("vijf", result.get(4));
        assertEquals("zes", result.get(5));
        Iterator<String> iterator = result.iterator();
        assertEquals("een", iterator.next());
        assertEquals("twee", iterator.next());
        assertEquals("drie", iterator.next());
        assertEquals("vier", iterator.next());
        assertEquals("vijf", iterator.next());
        assertEquals("zes", iterator.next());
        assertFalse(iterator.hasNext());
        try {
            result.get(6);
            throw new AssertionError("Should never reach here");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }
}