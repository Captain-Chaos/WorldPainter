/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import com.khorn.terraincontrol.util.minecraftTypes.DefaultMaterial;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.objects.AbstractObject;
import org.pepsoft.worldpainter.objects.WPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public final class Bo3Object extends AbstractObject implements Bo2ObjectProvider {
    private Bo3Object(String name, Map<String, String> properties, Map<Point3i, Bo3BlockSpec> blocks, Point3i origin, Point3i dimensions, Map<String, Serializable> attributes) {
        this.name = name;
        this.properties = properties;
        this.blocks = blocks;
        this.origin = origin;
        this.dimensions = dimensions;
        if ((origin.x != 0) || (origin.y != 0) || (origin.z != 0)) {
            if (attributes == null) attributes = new HashMap<>();
            attributes.put(ATTRIBUTE_OFFSET.key, new Point3i(-origin.x, -origin.y, -origin.z));
        }
        if ((! properties.containsKey(KEY_RANDOM_ROTATION)) || (! Boolean.valueOf(properties.get(KEY_RANDOM_ROTATION)))) {
            if (attributes == null) attributes = new HashMap<>();
            attributes.put(ATTRIBUTE_RANDOM_ROTATION.key, false);
        }
        this.attributes = attributes;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return blocks.get(new Point3i(x - origin.x, y - origin.y, z - origin.z)).getMaterial();
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return blocks.containsKey(new Point3i(x - origin.x, y - origin.y, z - origin.z));
    }

    @Override
    public Bo3Object getObject() {
        return this;
    }

    @Override
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        if (tileEntities == null) {
            tileEntities = blocks.values().stream()
                .flatMap(block -> block.getTileEntities().stream())
                .map(tileEntity -> {
                    TileEntity clone = (TileEntity) tileEntity.clone();
                    clone.setX(clone.getX() + origin.x);
                    clone.setY(clone.getY() + origin.z);
                    clone.setZ(clone.getZ() + origin.y);
                    return clone;
                })
                .collect(Collectors.toList());
        }
        if (tileEntities.isEmpty()) {
            return null;
        } else {
            return tileEntities;
        }
    }

    @Override
    public List<WPObject> getAllObjects() {
        return Collections.singletonList(this);
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        if (value != null) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key.key, value);
        } else if (attributes != null) {
            attributes.remove(key.key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        }
    }

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public boolean visitBlocks(BlockVisitor visitor) {
        for (Map.Entry<Point3i, Bo3BlockSpec> entry: blocks.entrySet()) {
            Point3i coords = entry.getKey();
            if (! visitor.visitBlock(this, coords.x + origin.x, coords.y + origin.y, coords.z + origin.z, entry.getValue().getMaterial())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Bo3Object clone() {
        Bo3Object clone = (Bo3Object) super.clone();
        clone.origin = (Point3i) origin.clone();
        clone.dimensions = (Point3i) dimensions.clone();
        if (attributes != null) {
            clone.attributes = new HashMap<>(attributes);
        }
        return clone;
    }
    
    /**
     * Load a custom object in bo3 format from a file. The name of the object
     * will be the name of the file, minus the extension.
     *
     * @param file The file from which to load the object.
     * @return A new <code>Bo3Object</code> containing the contents of the
     *     specified file.
     * @throws IOException If an I/O error occurred while reading the file.
     */
    public static Bo3Object load(File file) throws IOException {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        return load(name, file);
    }

    /**
     * Load a custom object in bo3 format from a file.
     *
     * @param objectName The name of the object.
     * @param file The file from which to load the object.
     * @return A new <code>Bo3Object</code> containing the contents of the
     *     specified file.
     * @throws IOException If an I/O error occurred while reading the file.
     */
    public static Bo3Object load(String objectName, File file) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("US-ASCII")))) {
            Map<String, String> properties = new HashMap<>();
            Map<Point3i, Bo3BlockSpec> blocks = new HashMap<>();
            String line;
            int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE;
            int lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
            int lowestZ = Integer.MAX_VALUE, highestZ = Integer.MIN_VALUE;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                } else if (line.startsWith("Block")) {
                    // Block spec
                    Deque<String> args = getArgs(line);
                    int x = Integer.parseInt(args.pop());
                    int z = Integer.parseInt(args.pop());
                    int y = Integer.parseInt(args.pop());
                    if (x < lowestX)  {lowestX = x;}
                    if (x > highestX) {highestX = x;}
                    if (y < lowestY)  {lowestY = y;}
                    if (y > highestY) {highestY = y;}
                    if (z < lowestZ)  {lowestZ = z;}
                    if (z > highestZ) {highestZ = z;}
                    Material material = decodeMaterial(args.pop());
                    TileEntity tileEntity = null;
                    if (! args.isEmpty()) {
                        tileEntity = loadTileEntity(file, args.pop());
                    }
                    Point3i coords = new Point3i(x, y, z);
                    blocks.put(coords, new Bo3BlockSpec(coords, material, tileEntity));
                } else if (line.startsWith("RandomBlock")) {
                    // Random block spec
                    Deque<String> args = getArgs(line);
                    int x = Integer.parseInt(args.pop());
                    int z = Integer.parseInt(args.pop());
                    int y = Integer.parseInt(args.pop());
                    if (x < lowestX)  {lowestX = x;}
                    if (x > highestX) {highestX = x;}
                    if (y < lowestY)  {lowestY = y;}
                    if (y > highestY) {highestY = y;}
                    if (z < lowestZ)  {lowestZ = z;}
                    if (z > highestZ) {highestZ = z;}
                    List<Bo3BlockSpec.RandomBlock> randomBlocks = new ArrayList<>();
                    do {
                        Material material = decodeMaterial(args.pop());
                        String nbtOrChance = args.pop();
                        TileEntity tileEntity = null;
                        int chance;
                        try {
                            chance = Integer.parseInt(nbtOrChance);
                        } catch (NumberFormatException e) {
                            tileEntity = loadTileEntity(file, nbtOrChance);
                            chance = Integer.parseInt(args.pop());
                        }
                        randomBlocks.add(new Bo3BlockSpec.RandomBlock(material, tileEntity, chance));
                    } while (! args.isEmpty());
                    Point3i coords = new Point3i(x, y, z);
                    blocks.put(coords, new Bo3BlockSpec(coords, randomBlocks.toArray(new Bo3BlockSpec.RandomBlock[randomBlocks.size()])));
                } else if (line.startsWith("BlockCheck") || line.startsWith("LightCheck") || line.startsWith("Branch") || line.startsWith("WeightedBranch")) {
                    logger.warn("Ignoring unsupported bo3 feature " + line);
                } else {
                    int p = line.indexOf(':');
                    if (p != -1) {
                        properties.put(line.substring(0, p).trim(), line.substring(p + 1).trim());
                    } else {
                        logger.warn("Ignoring unrecognised line: " + line);
                    }
                }
            }
            if (blocks.isEmpty()) {
                throw new IOException("No blocks found in the file; is this a bo3 object?");
            }
            Map<String, Serializable> attributes = new HashMap<>(Collections.singletonMap(ATTRIBUTE_FILE.key, file));
            return new Bo3Object(objectName, properties, blocks, new Point3i(-lowestX, -lowestY, -lowestZ), new Point3i(highestX - lowestX + 1, highestY - lowestY + 1, highestZ - lowestZ + 1), attributes);
        }
    }

    private static Deque<String> getArgs(String line) {
        int start = line.indexOf('(');
        if (start != -1) {
            int end = line.lastIndexOf(')');
            if (end != -1) {
                Deque<String> args = new ArrayDeque<>();
                args.addAll(Arrays.asList(line.substring(start + 1, end).split(",")));
                return args;
            }
        }
        throw new IllegalArgumentException("Could not parse line \"" + line + "\"");
    }

    private static Material decodeMaterial(String materialSpec) {
        int p = materialSpec.indexOf(':');
        if (p == -1) {
            if (Character.isDigit(materialSpec.charAt(0))) {
                return Material.get(Integer.parseInt(materialSpec));
            } else {
                return Material.get(DefaultMaterial.valueOf(materialSpec).id);
            }
        } else {
            String idSpec = materialSpec.substring(0, p).trim();
            String dataSpec = materialSpec.substring(p + 1).trim();
            if (Character.isDigit(idSpec.charAt(0))) {
                return Material.get(Integer.parseInt(idSpec), Integer.parseInt(dataSpec));
            } else {
                return Material.get(DefaultMaterial.valueOf(idSpec).id, Integer.parseInt(dataSpec));
            }
        }
    }

    private static TileEntity loadTileEntity(File bo3File, String nbtFileName) throws IOException {
        File nbtFile = new File(bo3File.getParentFile(), nbtFileName);
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(nbtFile)))) {
            CompoundTag tag = (CompoundTag) in.readTag();
            Map<String, Tag> map = tag.getValue();
            if ((map.size() == 1) && (map.values().iterator().next() instanceof CompoundTag)) {
                // If the root tag is a CompoundTag which only contains another
                // CompoundTag, assume that the nested CompoundTag actually
                // contains the data
                return TileEntity.fromNBT((CompoundTag) tag.getValue().values().iterator().next());
            } else {
                return TileEntity.fromNBT(tag);
            }
        }
    }
 
    private String name;
    private final Map<String, String> properties;
    private final Map<Point3i, Bo3BlockSpec> blocks;
    private Point3i origin, dimensions;
    private Map<String, Serializable> attributes;
    private int version = 1;
    private transient List<TileEntity> tileEntities;
    
    public static final String KEY_RANDOM_ROTATION = "RotateRandomly";

    private static final Logger logger = LoggerFactory.getLogger(Bo3Object.class);

    private static final long serialVersionUID = 1L;
}