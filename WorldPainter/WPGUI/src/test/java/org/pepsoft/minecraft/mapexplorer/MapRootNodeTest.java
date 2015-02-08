/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author pepijn
 */
public class MapRootNodeTest {
    @Test
    @Ignore // TODO: make it portable
    public void testGetRegions() {
        MapRootNode mapRootNode = new MapRootNode(new File("/home/pepijn/.minecraft/saves"), "Generated World");
        mapRootNode.getRegionNodes();
    }
}