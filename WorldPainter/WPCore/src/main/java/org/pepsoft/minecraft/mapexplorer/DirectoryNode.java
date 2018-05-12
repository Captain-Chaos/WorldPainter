package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pepijn on 13-3-16.
 */
public class DirectoryNode extends FileSystemNode {
    public DirectoryNode(File dir) {
        this(dir, false);
    }

    public DirectoryNode(File dir, boolean showFiles) {
        super(dir);
        this.showFiles = showFiles;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void doubleClicked() {
        // Do nothing
    }

    @Override
    protected Node[] loadChildren() {
        File[] contents = file.listFiles(showFiles ? null : File::isDirectory);
        if (contents != null) {
            Node[] children = new Node[contents.length];
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isDirectory()) {
                    for (MapRecognizer mapRecognizer: MAP_RECOGNIZERS) {
                        if (mapRecognizer.isMap(contents[i])) {
                            children[i] = mapRecognizer.getMapNode(contents[i]);
                            break;
                        }
                    }
                    if (children[i] == null) {
                        children[i] = new DirectoryNode(contents[i], showFiles);
                    }
                } else {
                    children[i] = new FileSystemNode(contents[i]);
                }
            }
            Arrays.sort(children, (node1, node2) -> COLLATOR.compare(node1.getName(), node2.getName()));
            return children;
        } else {
            return new Node[0];
        }
    }

    protected static final Collator COLLATOR = Collator.getInstance();

    private final boolean showFiles;

    private static final List<MapRecognizer> MAP_RECOGNIZERS = new ArrayList<>();

    static {
        for (Platform platform: PlatformManager.getInstance().getAllPlatforms()) {
            PlatformProvider platformProvider = PlatformManager.getInstance().getPlatformProvider(platform);
            MapRecognizer mapRecognizer = platformProvider.getMapRecognizer();
            if (mapRecognizer != null) {
                MAP_RECOGNIZERS.add(mapRecognizer);
            }
        }
    }
}
