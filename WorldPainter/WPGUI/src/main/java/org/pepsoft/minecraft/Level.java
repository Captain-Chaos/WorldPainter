/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.worldpainter.AccessDeniedException;
import org.pepsoft.worldpainter.Generator;

/**
 *
 * @author pepijn
 */
public final class Level extends AbstractNBTItem {
    public Level(int mapHeight, int version) {
        super(new CompoundTag(TAG_DATA, new HashMap<String, Tag>()));
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        if ((mapHeight & (mapHeight - 1)) != 0) {
            throw new IllegalArgumentException("mapHeight " + mapHeight + " not a power of two");
        }
        if (mapHeight != ((version == SUPPORTED_VERSION_1) ? DEFAULT_MAX_HEIGHT_1 : DEFAULT_MAX_HEIGHT_2)) {
            setInt(TAG_MAP_HEIGHT, mapHeight);
        }
        this.maxHeight = mapHeight;
        setInt(TAG_VERSION, version);
        addDimension(0);
    }

    public Level(CompoundTag tag, int mapHeight) {
        super((CompoundTag) tag.getTag(TAG_DATA));
        if ((mapHeight & (mapHeight - 1)) != 0) {
            throw new IllegalArgumentException("mapHeight " + mapHeight + " not a power of two");
        }
        int version = getInt(TAG_VERSION);
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        if (mapHeight != ((version == SUPPORTED_VERSION_1) ? DEFAULT_MAX_HEIGHT_1 : DEFAULT_MAX_HEIGHT_2)) {
            setInt(TAG_MAP_HEIGHT, mapHeight);
        }
        this.maxHeight = mapHeight;
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
        DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile));
        try {
            sessionOut.writeLong(System.currentTimeMillis());
        } finally {
            sessionOut.close();
        }
        
        // Write level.dat file
        File levelDatFile = new File(worldDir, "level.dat");
        // Make it show at the top of the single player map list:
        setLong(TAG_LAST_PLAYED, System.currentTimeMillis());
        NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(new FileOutputStream(levelDatFile)));
        try {
            out.writeTag(toNBT());
        } finally {
            out.close();
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

        // Write chunks for all dimensions to region files
//        int totalDimensions = dimensions.size(), dim = 0;
//        for (Dimension dimension: dimensions.values()) {
//            dimension.save(worldDir, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dim / totalDimensions, 1f / totalDimensions) : null, getVersion());
//            dim++;
//        }
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
    
//    public Collection<Chunk> getChunks(int dim) {
//        return getDimension(dim).getChunks();
//    }
//
//    public Chunk getChunk(int dim, int x, int z) {
//        return getDimension(dim).getChunk(x, z);
//    }
//
//    public void addChunk(int dim, Chunk chunk) {
//        getDimension(dim).addChunk(chunk);
//    }

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
        } else {
            return Generator.DEFAULT;
        }
    }
    
    public String getGeneratorOptions() {
        return getString(TAG_GENERATOR_OPTIONS);
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
    
    public void setGenerator(Generator generator) {
        switch (generator) {
            case DEFAULT:
                if (getVersion() == SUPPORTED_VERSION_1) {
                    setString(TAG_GENERATOR_NAME, "DEFAULT");
                } else {
                    setString(TAG_GENERATOR_NAME, "default");
                    setInt(TAG_GENERATOR_VERSION, 1);
                }
                break;
            case FLAT:
                if (getVersion() == SUPPORTED_VERSION_1) {
                    setString(TAG_GENERATOR_NAME, "FLAT");
                } else {
                    setString(TAG_GENERATOR_NAME, "flat");
                }
                break;
            case LARGE_BIOMES:
                if (getVersion() == SUPPORTED_VERSION_1) {
                    throw new IllegalArgumentException("Large biomes not supported for Minecraft 1.1 maps");
                } else {
                    setString(TAG_GENERATOR_NAME, "largeBiomes");
                    setInt(TAG_GENERATOR_VERSION, 0);
                }
                break;
            default:
                throw new InternalError();
        }
    }
    
    public void setGeneratorOptions(String generatorOptions) {
        setString(TAG_GENERATOR_OPTIONS, generatorOptions);
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
    
    @Override
    public Tag toNBT() {
//        Map<String, Tag> levelData = new HashMap<String, Tag>();
//        levelData.put(TAG_TIME, new LongTag(TAG_TIME, time));
//        levelData.put(TAG_LAST_PLAYED, new LongTag(TAG_LAST_PLAYED, System.currentTimeMillis()));
////        levelData.put(TAG_PLAYER, new CompoundTag(TAG_PLAYER, player.getData()));
//        levelData.put(TAG_SPAWN_X, new IntTag(TAG_SPAWN_X, spawnX));
//        levelData.put(TAG_SPAWN_Y, new IntTag(TAG_SPAWN_Y, spawnY));
//        levelData.put(TAG_SPAWN_Z, new IntTag(TAG_SPAWN_Z, spawnZ));
//        levelData.put(TAG_SIZE_ON_DISK, new LongTag(TAG_SIZE_ON_DISK, 0));
//        levelData.put(TAG_RANDOM_SEED, new LongTag(TAG_RANDOM_SEED, seed));
//        levelData.put(TAG_VERSION, new IntTag(TAG_VERSION, SUPPORTED_VERSION));
//        levelData.put(TAG_LEVEL_NAME, new StringTag(TAG_LEVEL_NAME, name));
        return new CompoundTag("", Collections.singletonMap("", super.toNBT()));
    }
    
    public static Level load(File levelDatFile) throws IOException {
        Tag tag;
        NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(levelDatFile)));
        try {
            tag = in.readTag();
        } finally {
            in.close();
        }
        
        int version = ((IntTag) ((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_VERSION)).getValue();
        int maxHeight = (version == SUPPORTED_VERSION_1) ? DEFAULT_MAX_HEIGHT_1 : DEFAULT_MAX_HEIGHT_2;
        if (version == SUPPORTED_VERSION_1) {
            if (((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_MAP_HEIGHT) != null) {
                maxHeight = ((IntTag) ((CompoundTag) ((CompoundTag) tag).getTag(TAG_DATA)).getTag(TAG_MAP_HEIGHT)).getValue();
            } else {
                File maxheightFile = new File(levelDatFile.getParentFile(), "maxheight.txt");
                if (! maxheightFile.isFile()) {
                    maxheightFile = new File(levelDatFile.getParentFile(), "Height.txt");
                }
                if (maxheightFile.isFile()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(maxheightFile), "US-ASCII"));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("height=")) {
                                int exp = Integer.parseInt(line.substring(7));
                                maxHeight = 1 << exp;
                                break;
                            }
                        }
                    } finally {
                        reader.close();
                    }
                }
            }
        }
        
        return new Level((CompoundTag) tag, maxHeight);
    }

    private final int maxHeight;
    private final Map<Integer, Dimension> dimensions = new HashMap<Integer, Dimension>();
    
    private static final long serialVersionUID = 1L;
}