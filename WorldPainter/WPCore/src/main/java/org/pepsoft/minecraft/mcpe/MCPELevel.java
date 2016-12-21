package org.pepsoft.minecraft.mcpe;

import org.jnbt.*;
import org.pepsoft.minecraft.AbstractNBTItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.minecraft.mcpe.MCPEConstants.*;

// Reference (from Windows 10 native version):
// TAG_Long: 10995116463872
// TAG_Int("lightningTime"): 95403
// TAG_Long("RandomSeed"): 616143223
// TAG_Int("Platform"): 2
// TAG_Int("NetworkVersion"): 91
// TAG_Int("Difficulty"): 2
// TAG_Compound("fixedInventory"): 1 entries
// {
//     TAG_List("fixedInventoryItems"): 0 entries of type TAG_Byte
//     {
//     }
// }
// TAG_Byte("texturePacksRequired"): 0
// TAG_Long("Time"): 597
// TAG_Int("GameType"): 1
// TAG_Byte("commandsEnabled"): 0
// TAG_Byte("spawnMobs"): 1
// TAG_Int("LimitedWorldOriginY"): 128
// TAG_Byte("hasBeenLoadedInCreative"): 1
// TAG_Float("lightningLevel"): 0.0
// TAG_Int("LimitedWorldOriginX"): 0
// TAG_Byte("falldamage"): 1
// TAG_Int("LimitedWorldOriginZ"): 4
// TAG_Byte("MultiplayerGame"): 1
// TAG_Byte("drowningdamage"): 1
// TAG_Float("rainLevel"): 0.0
// TAG_Long("currentTick"): 597
// TAG_Int("SpawnY"): 128
// TAG_Int("rainTime"): 47403
// TAG_Int("SpawnZ"): 4
// TAG_Byte("eduLevel"): 0
// TAG_Int("SpawnX"): 0
// TAG_Byte("LANBroadcast"): 1
// TAG_Byte("pvp"): 1
// TAG_Int("DayCycleStopTime"): -1
// TAG_Long("LastPlayed"): 1481408001
// TAG_Long("worldStartCount"): 4294967294
// TAG_Byte("ForceGameType"): 0
// TAG_Byte("XBLBroadcast"): 1
// TAG_String("LevelName"): My World
// TAG_Int("StorageVersion"): 4
// TAG_Byte("firedamage"): 1
// TAG_Byte("immutableWorld"): 0
// TAG_Int("Generator"): 1

/**
 * Created by Pepijn on 11-12-2016.
 */
public class MCPELevel extends AbstractNBTItem {
    public MCPELevel() {
        super(new CompoundTag("", Collections.emptyMap()));
    }

    private MCPELevel(CompoundTag tag) {
        super(tag);
    }

    public int getGameType() {
        return getInt(TAG_GAME_TYPE);
    }

    public void setGameType(int gameType) {
        setInt(TAG_GAME_TYPE, gameType);
    }

    public String getLevelName() {
        return getString(TAG_LEVEL_NAME);
    }

    public void setLevelName(String levelName) {
        setString(TAG_LEVEL_NAME, levelName);
    }

    public int getGenerator() {
        return getInt(TAG_GENERATOR);
    }

    public void setGenerator(int generator) {
        setInt(TAG_GENERATOR, generator);
    }

    public long getLastPlayed() {
        return getLong(TAG_LAST_PLAYED);
    }

    public void setLastPlayed(long lastPlayed) {
        setLong(TAG_LAST_PLAYED, lastPlayed);
    }

    public int getLimitedWorldOriginX() {
        return getInt(TAG_LIMITED_WORLD_ORIGIN_X);
    }

    public void setLimitedWorldOriginX(int limitedWorldOriginX) {
        setInt(TAG_LIMITED_WORLD_ORIGIN_X, limitedWorldOriginX);
    }

    public int getLimitedWorldOriginY() {
        return getInt(TAG_LIMITED_WORLD_ORIGIN_Y);
    }

    public void setLimitedWorldOriginY(int limitedWorldOriginY) {
        setInt(TAG_LIMITED_WORLD_ORIGIN_Y, limitedWorldOriginY);
    }

    public int getLimitedWorldOriginZ() {
        return getInt(TAG_LIMITED_WORLD_ORIGIN_Z);
    }

    public void setLimitedWorldOriginZ(int limitedWorldOriginZ) {
        setInt(TAG_LIMITED_WORLD_ORIGIN_Z, limitedWorldOriginZ);
    }

    public int getPlatform() {
        return getInt(TAG_PLATFORM);
    }

    public void setPlatform(int platform) {
        setInt(TAG_PLATFORM, platform);
    }

    public long getRandomSeed() {
        return getLong(TAG_RANDOM_SEED);
    }

    public void setRandomSeed(long randomSeed) {
        setLong(TAG_RANDOM_SEED, randomSeed);
    }

    public int getSpawnX() {
        return getInt(TAG_SPAWN_X);
    }

    public void setSpawnX(int spawnX) {
        setInt(TAG_SPAWN_X, spawnX);
    }

    public int getSpawnY() {
        return getInt(TAG_SPAWN_Y);
    }

    public void setSpawnY(int spawnY) {
        setInt(TAG_SPAWN_Y, spawnY);
    }

    public int getSpawnZ() {
        return getInt(TAG_SPAWN_Z);
    }

    public void setSpawnZ(int spawnZ) {
        setInt(TAG_SPAWN_Z, spawnZ);
    }

    public int getStorageVersion() {
        return getInt(TAG_STORAGE_VERSION);
    }

    public void setStorageVersion(int storageVersion) {
        setInt(TAG_STORAGE_VERSION, storageVersion);
    }

    public long getTime() {
        return getLong(TAG_TIME);
    }

    public void setTime(long time) {
        setLong(TAG_TIME, time);
    }

    public int getDayCycleStopTime() {
        return getInt(TAG_DAY_CYCLE_STOP_TIME);
    }

    public void setDayCycleStopTime(int dayCycleStopTime) {
        setInt(TAG_DAY_CYCLE_STOP_TIME, dayCycleStopTime);
    }

    public byte getSpawnMobs() {
        return getByte(TAG_SPAWN_MOBS);
    }

    public void setSpawnMobs(byte spawnMobs) {
        setByte(TAG_SPAWN_MOBS, spawnMobs);
    }

    public void save(File worldDir) throws IOException {
        if (! worldDir.isDirectory()) {
            if (!worldDir.mkdirs()) {
                throw new IOException("Could not create world directory " + worldDir);
            }
        }
        File levelDatFile = new File(worldDir, "level.dat");
        try (NBTOutputStream out = new NBTOutputStream(new FileOutputStream(levelDatFile), true)) {
            Map<String, Tag> properties = ((CompoundTag) toNBT()).getValue();

            // Write the mysterious unnamed number first
            Tag mysteriousEmptyTag = properties.get("");
            if (mysteriousEmptyTag != null) {
                out.writeTag(mysteriousEmptyTag);
            }

            // Write all other properties
            properties.entrySet().stream()
                    .filter(entry -> ! entry.getKey().isEmpty())
                    .map(Map.Entry::getValue)
                    .forEach(tag -> {
                        try {
                            out.writeTag(tag);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // Write an end tag
            out.writeTag(new EndTag());
        }
    }

    public static MCPELevel load(File levelDatFile) throws IOException {
        try (NBTInputStream in = new NBTInputStream(new FileInputStream(levelDatFile), true)) {
            Map<String, Tag> properties = new HashMap<>();
            Tag tag;
            do {
                tag = in.readTag();
                if (! (tag instanceof EndTag)) {
                    properties.put(tag.getName(), tag);
                }
            } while (! (tag instanceof EndTag));
            return new MCPELevel(new CompoundTag("", properties));
        }
    }
}