package org.pepsoft.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.pepsoft.util.TextUtils.getLine;

public class TextUtilsTest {
    @Test
    public void testGetLine() {
        assertEquals("een", getLine("een twee drie vier vijf", 1, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 1, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 2, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 3, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 4, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 5, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 6, 1));
        assertEquals("twee", getLine("een twee drie vier vijf", 7, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 8, 1));
        assertEquals("drie vier", getLine("een twee drie vier vijf", 9, 1));
        assertEquals("drie vier", getLine("een twee drie vier vijf", 10, 1));
        assertEquals("vijf", getLine("een twee drie vier vijf", 10, 2));
        assertEquals("", getLine("een twee drie vier vijf", 10, 3));

        assertEquals("een", getLine("een twee drie vier vijf", 1, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 1, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 1, 2));
        assertEquals("vier", getLine("een twee drie vier vijf", 1, 3));
        assertEquals("vijf", getLine("een twee drie vier vijf", 1, 4));
        assertEquals("", getLine("een twee drie vier vijf", 1, 5));

        assertEquals("een", getLine("een twee drie vier vijf", 3, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 3, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 3, 2));
        assertEquals("vier", getLine("een twee drie vier vijf", 3, 3));
        assertEquals("vijf", getLine("een twee drie vier vijf", 3, 4));
        assertEquals("", getLine("een twee drie vier vijf", 3, 5));

        assertEquals("een", getLine("een twee drie vier vijf", 4, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 4, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 4, 2));
        assertEquals("vier", getLine("een twee drie vier vijf", 4, 3));
        assertEquals("vijf", getLine("een twee drie vier vijf", 4, 4));
        assertEquals("", getLine("een twee drie vier vijf", 4, 5));

        assertEquals("een", getLine("een twee drie vier vijf", 5, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 5, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 5, 2));
        assertEquals("vier", getLine("een twee drie vier vijf", 5, 3));
        assertEquals("vijf", getLine("een twee drie vier vijf", 5, 4));
        assertEquals("", getLine("een twee drie vier vijf", 5, 5));

        assertEquals("een", getLine("een twee drie vier vijf", 6, 0));
        assertEquals("twee", getLine("een twee drie vier vijf", 6, 1));
        assertEquals("drie", getLine("een twee drie vier vijf", 6, 2));
        assertEquals("vier", getLine("een twee drie vier vijf", 6, 3));
        assertEquals("vijf", getLine("een twee drie vier vijf", 6, 4));
        assertEquals("", getLine("een twee drie vier vijf", 6, 5));

        assertEquals("een twee", getLine("een twee drie vier vijf", 9, 0));
        assertEquals("drie vier", getLine("een twee drie vier vijf", 9, 1));
        assertEquals("vijf", getLine("een twee drie vier vijf", 9, 2));
        assertEquals("", getLine("een twee drie vier vijf", 9, 3));

        assertEquals("een", getLine("een twee drie vier vijf", 1, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 2, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 3, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 4, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 5, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 6, 0));
        assertEquals("een", getLine("een twee drie vier vijf", 7, 0));
        assertEquals("een twee", getLine("een twee drie vier vijf", 8, 0));
        assertEquals("een twee", getLine("een twee drie vier vijf", 9, 0));
        assertEquals("een twee", getLine("een twee drie vier vijf", 10, 0));
        assertEquals("een twee", getLine("een twee drie vier vijf", 11, 0));
        assertEquals("een twee", getLine("een twee drie vier vijf", 12, 0));
        assertEquals("een twee drie", getLine("een twee drie vier vijf", 13, 0));
        assertEquals("een twee drie", getLine("een twee drie vier vijf", 14, 0));
        assertEquals("een twee drie", getLine("een twee drie vier vijf", 15, 0));
        assertEquals("een twee drie", getLine("een twee drie vier vijf", 16, 0));
        assertEquals("een twee drie", getLine("een twee drie vier vijf", 17, 0));
        assertEquals("een twee drie vier", getLine("een twee drie vier vijf", 18, 0));
        assertEquals("een twee drie vier", getLine("een twee drie vier vijf", 19, 0));
        assertEquals("een twee drie vier", getLine("een twee drie vier vijf", 20, 0));
        assertEquals("een twee drie vier", getLine("een twee drie vier vijf", 21, 0));
        assertEquals("een twee drie vier", getLine("een twee drie vier vijf", 22, 0));
        assertEquals("een twee drie vier vijf", getLine("een twee drie vier vijf", 23, 0));
        assertEquals("een twee drie vier vijf", getLine("een twee drie vier vijf", 24, 0));

        assertEquals("een twee drie", getLine(" een\ntwee      drie         vier\tvijf ", 13, 0));
    }
}