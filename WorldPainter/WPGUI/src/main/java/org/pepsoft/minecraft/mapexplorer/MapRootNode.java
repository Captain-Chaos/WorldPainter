/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.util.IconUtils;

import javax.swing.*;
import java.io.File;

/**
 *
 * @author pepijn
 */
public class MapRootNode extends MinecraftDirectoryNode {
    public MapRootNode(File dir) {
        super(dir);
    }
    
    @Override
    public Icon getIcon() {
        return ICON;
    }

    private static final Icon ICON = IconUtils.scaleIcon(IconUtils.loadIcon("org/pepsoft/worldpainter/icons/grass.png"), 16);
}