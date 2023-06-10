package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Material;

import static org.pepsoft.minecraft.Constants.MC_COBBLESTONE_WALL;

/**
 * Created by isc21004 on 03-06-15.
 */
public class WPObjectUtils {
    /**
     * Determine whether two blocks would connect to each other in some way (forming a fence, for instance).
     */
    @SuppressWarnings("StringEquality") // String is interned
    public static boolean wouldConnect(Material blockTypeOne, Material blockTypeTwo) {
        if (blockTypeOne.veryInsubstantial || blockTypeTwo.veryInsubstantial) {
            return false;
        } else if (blockTypeOne.connectingBlock || blockTypeOne.isNamed(MC_COBBLESTONE_WALL) || blockTypeTwo.connectingBlock || blockTypeTwo.isNamed(MC_COBBLESTONE_WALL)) {
            // TODO encode this into a "connects" property on the material and just check the name
            return (blockTypeOne.name == blockTypeTwo.name)
                    || (blockTypeOne.solid && blockTypeOne.opaque)
                    || (blockTypeTwo.solid && blockTypeTwo.opaque);
        } else {
            return false;
        }
    }
}