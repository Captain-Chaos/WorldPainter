/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.ShortTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class RespawnPlayer {
    public static void respawnPlayer(File levelDatFile) throws IOException {
        CompoundTag outerTag;
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(levelDatFile)))) {
            outerTag = (CompoundTag) in.readTag();
        }
        CompoundTag dataTag = (CompoundTag) outerTag.getTag(TAG_DATA);
        int spawnX = ((IntTag) dataTag.getTag(TAG_SPAWN_X)).getValue();
        int spawnY = ((IntTag) dataTag.getTag(TAG_SPAWN_Y)).getValue();
        int spawnZ = ((IntTag) dataTag.getTag(TAG_SPAWN_Z)).getValue();
        CompoundTag playerTag = (CompoundTag) dataTag.getTag(TAG_PLAYER);
        playerTag.setTag(TAG_DEATH_TIME, new ShortTag(TAG_DEATH_TIME, (short) 0));
        playerTag.setTag(TAG_HEALTH, new ShortTag(TAG_HEALTH, (short) 20));
        List<Tag> motionList = new ArrayList<>(3);
        motionList.add(new DoubleTag(null, 0));
        motionList.add(new DoubleTag(null, 0));
        motionList.add(new DoubleTag(null, 0));
        playerTag.setTag(TAG_MOTION, new ListTag(TAG_MOTION, DoubleTag.class, motionList));
        List<Tag> posList = new ArrayList<>(3);
        posList.add(new DoubleTag(null, spawnX + 0.5));
        posList.add(new DoubleTag(null, spawnY + 3));
        posList.add(new DoubleTag(null, spawnZ + 0.5));
        playerTag.setTag(TAG_POS, new ListTag(TAG_POS, DoubleTag.class, posList));
        try (NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(new FileOutputStream(levelDatFile)))) {
            out.writeTag(outerTag);
        }
    }
}
