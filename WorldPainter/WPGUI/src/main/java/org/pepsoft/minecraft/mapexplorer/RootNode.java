package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.mapexplorer.AbstractNode;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pepijn on 13-3-16.
 */
public class RootNode extends AbstractNode {
    public RootNode() {
    }

    @Override
    public String getName() {
        return "Root";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    protected Node[] loadChildren() {
        List<Node> children = new ArrayList<>();
        File javaDir = MinecraftUtil.findMinecraftDir();
        if (javaDir != null) {
            children.add(new DirectoryNode(new File(javaDir, "saves"), false, "Java Maps"));
        }
        File windowsDir = new File(System.getProperty("user.home"), "AppData/Local/Packages/Microsoft.MinecraftUWP_8wekyb3d8bbwe/LocalState/games/com.mojang/minecraftWorlds");
        if (windowsDir.isDirectory()) {
            children.add(new DirectoryNode(windowsDir, true, "Bedrock Edition Maps"));
        }
        Arrays.stream(File.listRoots())
                .map(DirectoryNode::new)
                .forEach(children::add);
        return children.toArray(new Node[children.size()]);
    }
}
