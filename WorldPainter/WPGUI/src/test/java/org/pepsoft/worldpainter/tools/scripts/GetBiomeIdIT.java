package org.pepsoft.worldpainter.tools.scripts;

import org.junit.Test;

import java.io.IOException;

public class GetBiomeIdIT extends AbstractScriptTest {
    @Test
    public void testGetBiomeId() throws IOException, ClassNotFoundException {
        testScript("getbiomeidtest.js");
    }
}