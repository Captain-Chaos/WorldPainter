/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.*;
import org.pepsoft.worldpainter.AccessDeniedException;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.Platform;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.MAX_HEIGHT;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 *
 * @author pepijn
 */
public final class Level extends AbstractNBTItem {
    public Level(int mapHeight, Platform platform) {
        super(new CompoundTag(TAG_DATA, new HashMap<>()));
        if ((platform != JAVA_ANVIL)
                && (platform != JAVA_MCREGION)
                && (platform != JAVA_ANVIL_1_13)) {
            throw new IllegalArgumentException("Not a supported platform: " + platform);
        }
        if ((mapHeight & (mapHeight - 1)) != 0) {
            throw new IllegalArgumentException("mapHeight " + mapHeight + " not a power of two");
        }
        if (mapHeight != ((platform == JAVA_MCREGION) ? DEFAULT_MAX_HEIGHT_MCREGION : DEFAULT_MAX_HEIGHT_ANVIL)) {
            setInt(TAG_MAP_HEIGHT, mapHeight);
        }
        this.maxHeight = mapHeight;
        extraTags = null;
        setInt(TAG_VERSION, (platform == JAVA_MCREGION) ? VERSION_MCREGION : VERSION_ANVIL);
        // TODO: make this dynamic?
        if (platform != JAVA_MCREGION) {
            int dataVersion = (platform == JAVA_ANVIL) ? DATA_VERSION_MC_1_12_2 : DATA_VERSION_MC_1_13_2;
            setInt(TAG_DATA_VERSION, dataVersion);
            Map<String, Tag> versionTag = new HashMap<>();
            versionTag.put(TAG_ID_, new IntTag(TAG_ID_, dataVersion));
            versionTag.put(TAG_NAME, new StringTag(TAG_NAME, "WorldPainter"));
            versionTag.put(TAG_SNAPSHOT, new ByteTag(TAG_SNAPSHOT, (byte) 0));
            setMap(TAG_VERSION_, versionTag);
        }
        addDimension(0);
    }

    public Level(CompoundTag tag, int mapHeight) {
        super((CompoundTag) tag.getTag(TAG_DATA));
        if ((mapHeight & (mapHeight - 1)) != 0) {
            throw new IllegalArgumentException("mapHeight " + mapHeight + " not a power of two");
        }
        int version = getInt(TAG_VERSION);
        if ((version != VERSION_MCREGION) && (version != VERSION_ANVIL)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        if (mapHeight != ((version == VERSION_MCREGION) ? DEFAULT_MAX_HEIGHT_MCREGION : DEFAULT_MAX_HEIGHT_ANVIL)) {
            setInt(TAG_MAP_HEIGHT, mapHeight);
        }
        this.maxHeight = mapHeight;
        if (tag.getValue().size() == 1) {
            // No extra tags
            extraTags = null;
        } else {
            // The root tag contains extra tags, most likely from mods. Preserve them (but filter out the data tag)
            extraTags = new HashSet<>();
            tag.getValue().values().stream()
                    .filter(extraTag -> ! extraTag.getName().equals(TAG_DATA))
                    .forEach(extraTags::add);
        }
        addDimension(0);
    }
    
    public void save(File worldDir) throws IOException {
        if (! worldDir.exists()) {
            if (! worldDir.mkdirs()) {
                throw new AccessDeniedException("Could not create directory " + worldDir);
            }
        }

        // Write session.lock file
        File sessionLockFile = new File(worldDir, "session.lock");
        try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
            sessionOut.writeLong(System.currentTimeMillis());
        }
        
        // Write level.dat file
        File levelDatFile = new File(worldDir, "level.dat");
        // Make it show at the top of the single player map list:
        setLong(TAG_LAST_PLAYED, System.currentTimeMillis());
        try (NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(new FileOutputStream(levelDatFile)))) {
            out.writeTag(toNBT());
        }
        
        // If height is non-standard, write DynamicHeight mod and Height Mod
        // files
        int mapHeight = getMapHeight();
        if (mapHeight != 0) {
            int exp = (int) (Math.log(mapHeight) / Math.log(2));
            PrintWriter writer = new PrintWriter(new File(worldDir, "maxheight.txt"), "US-ASCII");
            try {
                writer.println("#DynamicHeight Save Format 2");
                writer.println("#" + new Date());
                writer.println("height=" + exp);
            } finally {
                writer.close();
            }

            writer = new PrintWriter(new File(worldDir, "Height.txt"), "US-ASCII");
            try {
                writer.println("#HeightMod 1.5");
                writer.println("#" + new Date());
                writer.println("height=" + exp);
                writer.println("version=1");
                writer.println("midheight=" + mapHeight + ".0");
                writer.println("waterlevel=" + (mapHeight / 2 - 2) + ".0");
                writer.println("genheight=" + mapHeight + ".0");
            } finally {
                writer.close();
            }
        }
    }
    
    public Dimension getDimension(int dim) {
        return dimensions.get(dim);
    }
    
    public void addDimension(int dim) {
        if (dimensions.containsKey(dim)) {
            throw new IllegalStateException("Dimension " + dim + " already exists");
        } else {
            dimensions.put(dim, new Dimension(dim, maxHeight));
        }
    }
    
    public Dimension removeDimension(int dim) {
        return dimensions.remove(dim);
    }
    
    public String getName() {
        return getString(TAG_LEVEL_NAME);
    }

    public long getSeed() {
        return getLong(TAG_RANDOM_SEED);
    }

    public int getSpawnX() {
        return getInt(TAG_SPAWN_X);
    }

    public int getSpawnY() {
        return getInt(TAG_SPAWN_Y);
    }

    public int getSpawnZ() {
        return getInt(TAG_SPAWN_Z);
    }

    public long getTime() {
        return getLong(TAG_TIME);
    }
    
    public int getVersion() {
        return getInt(TAG_VERSION);
    }

    /**
     * 1.12.2: 1343
     * 18w11a: 1478
     */
    public int getDataVersion() {
        return getInt(TAG_DATA_VERSION);
    }

    public boolean isMapFeatures() {
        return getBoolean(TAG_MAP_FEATURES);
    }

    public int getMapHeight() {
        return getInt(TAG_MAP_HEIGHT);
    }
    
    public int getGameType() {
        return getInt(TAG_GAME_TYPE);
    }
    
    public boolean isHardcore() {
        return getBoolean(TAG_HARDCORE);
    }
    
    public String getGeneratorName() {
        return getString(TAG_GENERATOR_NAME);
    }

    public int getGeneratorVersion() {
        return getInt(TAG_GENERATOR_VERSION);
    }
    
    public Generator getGenerator() {
        if ("FLAT".equals(getGeneratorName()) || "flat".equals(getGeneratorName())) {
            return Generator.FLAT;
        } else if ("largeBiomes".equals(getGeneratorName())) {
            return Generator.LARGE_BIOMES;
        } else if ("DEFAULT".equals(getGeneratorName()) || "default".equals(getGeneratorName())) {
            return Generator.DEFAULT;
        } else {
            return Generator.CUSTOM;
        }
    }
    
    public Tag getGeneratorOptions() {
        return getTag(TAG_GENERATOR_OPTIONS);
    }
    
    public boolean isAllowCommands() {
        return getBoolean(TAG_ALLOW_COMMANDS);
    }

    public int getMaxHeight() {
        return maxHeight;
    }
    
    public int getDifficulty() {
        return getByte(TAG_DIFFICULTY);
    }
    
    public boolean isDifficultyLocked() {
        return getBoolean(TAG_DIFFICULTY_LOCKED);
    }
    
    public double getBorderCenterX() {
        return getDouble(TAG_BORDER_CENTER_X);
    }
    
    public double getBorderCenterZ() {
        return getDouble(TAG_BORDER_CENTER_Z);
    }
    
    public double getBorderSize() {
        return getDouble(TAG_BORDER_SIZE);
    }
    
    public double getBorderSafeZone() {
        return getDouble(TAG_BORDER_SAFE_ZONE);
    }
    
    public double getBorderWarningBlocks() {
        return getDouble(TAG_BORDER_WARNING_BLOCKS);
    }
    
    public double getBorderWarningTime() {
        return getDouble(TAG_BORDER_WARNING_TIME);
    }
    
    public double getBorderSizeLerpTarget() {
        return getDouble(TAG_BORDER_SIZE_LERP_TARGET);
    }
    
    public long getBorderSizeLerpTime() {
        return getLong(TAG_BORDER_SIZE_LERP_TIME);
    }
    
    public double getBorderDamagePerBlock() {
        return getDouble(TAG_BORDER_DAMAGE_PER_BLOCK);
    }
    
    public void setName(String name) {
        setString(TAG_LEVEL_NAME, name);
    }

    public void setSeed(long seed) {
        setLong(TAG_RANDOM_SEED, seed);
    }

    public void setSpawnX(int spawnX) {
        setInt(TAG_SPAWN_X, spawnX);
    }

    public void setSpawnY(int spawnY) {
        setInt(TAG_SPAWN_Y, spawnY);
    }

    public void setSpawnZ(int spawnZ) {
        setInt(TAG_SPAWN_Z, spawnZ);
    }

    public void setTime(long time) {
        setLong(TAG_TIME, time);
    }
    
    public void setMapFeatures(boolean mapFeatures) {
        setBoolean(TAG_MAP_FEATURES, mapFeatures);
    }
    
    public void setGameType(int gameType) {
        setInt(TAG_GAME_TYPE, gameType);
    }
    
    public void setHardcore(boolean hardcore) {
        setBoolean(TAG_HARDCORE, hardcore);
    }

    public void setGeneratorName(String generatorName) {
        setString(TAG_GENERATOR_NAME, generatorName);
    }

    public void setGenerator(Generator generator) {
        switch (generator) {
            case DEFAULT:
                if (getVersion() == VERSION_MCREGION) {
                    setString(TAG_GENERATOR_NAME, "DEFAULT");
                } else {
                    setString(TAG_GENERATOR_NAME, "default");
                    setInt(TAG_GENERATOR_VERSION, 1);
                }
                break;
            case FLAT:
                if (getVersion() == VERSION_MCREGION) {
                    setString(TAG_GENERATOR_NAME, "FLAT");
                } else {
                    setString(TAG_GENERATOR_NAME, "flat");
                }
                break;
            case LARGE_BIOMES:
                if (getVersion() == VERSION_MCREGION) {
                    throw new IllegalArgumentException("Large biomes not supported for Minecraft 1.1 maps");
                } else {
                    setString(TAG_GENERATOR_NAME, "largeBiomes");
                    setInt(TAG_GENERATOR_VERSION, 0);
                }
                break;
            default:
                throw new IllegalArgumentException("Use setGeneratorName(String) for generator " + generator);
        }
    }
    
    public void setGeneratorOptions(Tag generatorOptions) {
        setTag(TAG_GENERATOR_OPTIONS, generatorOptions);
    }
    
    public void setAllowCommands(boolean allowCommands) {
        setBoolean(TAG_ALLOW_COMMANDS, allowCommands);
    }
    
    public void setDifficulty(int difficulty) {
        setByte(TAG_DIFFICULTY, (byte) difficulty);
    }
    
    public void setDifficultyLocked(boolean difficultyLocked) {
        setBoolean(TAG_DIFFICULTY_LOCKED, difficultyLocked);
    }
    
    public void setBorderCenterX(double borderCenterX) {
        setDouble(TAG_BORDER_CENTER_X, borderCenterX);
    }
    
    public void setBorderCenterZ(double borderCenterZ) {
        setDouble(TAG_BORDER_CENTER_Z, borderCenterZ);
    }
    
    public void setBorderSize(double borderSize) {
        setDouble(TAG_BORDER_SIZE, borderSize);
    }
    
    public void setBorderSafeZone(double borderSafeZone) {
        setDouble(TAG_BORDER_SAFE_ZONE, borderSafeZone);
    }
    
    public void setBorderWarningBlocks(double borderWarningBlocks) {
        setDouble(TAG_BORDER_WARNING_BLOCKS, borderWarningBlocks);
    }
    
    public void setBorderWarningTime(double borderWarningTime) {
        setDouble(TAG_BORDER_WARNING_TIME, borderWarningTime);
    }
    
    public void setBorderSizeLerpTarget(double borderSizeLerpTarget) {
        setDouble(TAG_BORDER_SIZE_LERP_TARGET, borderSizeLerpTarget);
    }
    
    public void setBorderSizeLerpTime(long borderSizeLerpTime) {
        setLong(TAG_BORDER_SIZE_LERP_TIME, borderSizeLerpTime);
    }
    
    public void setBorderDamagePerBlock(double borderDamagePerBlock) {
        setDouble(TAG_BORDER_DAMAGE_PER_BLOCK, borderDamagePerBlock);
    }
    
    @Override
    public Tag toNBT() {
        Map<String, Tag> values = new HashMap<>();
        values.put(TAG_DATA, super.toNBT());
        if (extraTags != null) {
            for (Tag extraTag: extraTags) {
                values.put(extraTag.getName(), extraTag);
            }
        }
        return new CompoundTag("", values);
    }
    
    public static Level load(File levelDatFile) throws IOException {
        Tag tag;
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(levelDatFile)))) {
            tag = in.readTag();
        }
        
        int version = ((IntTag) ((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_VERSION)).getValue();
        int maxHeight = (version == VERSION_MCREGION) ? DEFAULT_MAX_HEIGHT_MCREGION : DEFAULT_MAX_HEIGHT_ANVIL;
        // TODO get rid of this hardcoded stuff and move it into the platform provider plugin API
        if (version == VERSION_MCREGION) {
            if (((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_MAP_HEIGHT) != null) {
                maxHeight = ((IntTag) ((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_MAP_HEIGHT)).getValue();
            } else {
                File maxheightFile = new File(levelDatFile.getParentFile(), "maxheight.txt");
                if (! maxheightFile.isFile()) {
                    maxheightFile = new File(levelDatFile.getParentFile(), "Height.txt");
                }
                if (maxheightFile.isFile()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(maxheightFile), "US-ASCII"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("height=")) {
                                int exp = Integer.parseInt(line.substring(7));
                                maxHeight = 1 << exp;
                                break;
                            }
                        }
                    }
                }
            }
        } else if (new File(levelDatFile.getParentFile(), "region3d").isDirectory()) {
            maxHeight = MAX_HEIGHT;
        }
        
        return new Level((CompoundTag) tag, maxHeight);
    }

    private final int maxHeight;
    private final Map<Integer, Dimension> dimensions = new HashMap<>();
    private final Set<Tag> extraTags;
    
    private static final long serialVersionUID = 1L;
}