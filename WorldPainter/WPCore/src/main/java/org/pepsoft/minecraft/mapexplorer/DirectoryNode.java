package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.mapexplorer.MapExplorerSupport;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pepijn on 13-3-16.
 */
public class DirectoryNode extends FileSystemNode {
    public DirectoryNode(File dir) {
        this(dir, true);
    }

    public DirectoryNode(File dir, boolean showFiles) {
        this(dir, showFiles, null);
    }

    public DirectoryNode(File dir, boolean showFiles, String displayName) {
        super(dir, displayName);
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
                    for (MapExplorerSupport mapRecognizer: MAP_RECOGNIZERS) {
                        try {
                            if (mapRecognizer.identifyMap(contents[i]) != null) {
                                children[i] = mapRecognizer.getMapNode(contents[i]);
                                break;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            // Continue the loop
                        }
                    }
                    if (children[i] == null) {
                        children[i] = new DirectoryNode(contents[i], true);
                    }
                } else if (contents[i].isFile()) {
                    String lowercaseName = contents[i].getName().toLowerCase();
                    if (lowercaseName.endsWith(".nbt") || lowercaseName.endsWith(".dat") || lowercaseName.endsWith(".dat_old")) {
                        children[i] = new NBTFileNode(contents[i]);
                    } else if (isGzip(contents[i])) {
                        // Gamble that any gzipped file is an NBT file
                        children[i] = new NBTFileNode(contents[i]);
//                    } else if (lowercaseName.endsWith(".json")) {
                        // TODO
                    } else {
                        children[i] = new FileSystemNode(contents[i]);
                    }
                } else {
                    children[i] = new FileSystemNode(contents[i]);
                }
            }
            Arrays.sort(children, (node1, node2) -> node1.isLeaf()
                    ? (node2.isLeaf() ? COLLATOR.compare(node1.getName(), node2.getName()) : 1)
                    : (node2.isLeaf() ? -1 : COLLATOR.compare(node1.getName(), node2.getName())));
            return children;
        } else {
            return new Node[0];
        }
    }

    protected boolean isGzip(File file) {
        if (file.length() >= 18) {
            try (FileInputStream in = new FileInputStream(file)) {
                return (in.read() == 0x1f) && (in.read() == 0x8b);
            } catch (IOException e) {
                logger.warn("I/O error while trying to determine whether " + file + " is a gzip file", e);
            }
        }
        return false;
    }

    protected static final Collator COLLATOR = Collator.getInstance();

    private final boolean showFiles;

    private static final List<MapExplorerSupport> MAP_RECOGNIZERS = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(DirectoryNode.class);

    static {
        for (Platform platform: PlatformManager.getInstance().getAllPlatforms()) {
            PlatformProvider platformProvider = PlatformManager.getInstance().getPlatformProvider(platform);
            if (platformProvider instanceof MapExplorerSupport) {
                MAP_RECOGNIZERS.add((MapExplorerSupport) platformProvider);
            }
        }
    }
}
