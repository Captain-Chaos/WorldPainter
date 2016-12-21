package org.pepsoft.minecraft.mcpe;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Pepijn on 11-12-2016.
 */
public class MCPEKey {
    public MCPEKey(int x, int z, byte type) throws IOException {
        this.x = x;
        this.z = z;
        this.type = type;
        text = null;
    }

    public MCPEKey(String text) throws IOException {
        this.text = text;
        x = z = 0;
        type = 0;
    }

    public MCPEKey(byte[] data) throws IOException {
        if (data.length == 9) {
            try (LittleEndianDataInputStream in = new LittleEndianDataInputStream(new ByteArrayInputStream(data))) {
                x = in.readInt();
                z = in.readInt();
                type = in.readByte();
                text = null;
            }
        } else {
            StringBuilder sb = new StringBuilder(data.length * 3 + 3);
            sb.append(new String(data, "US-ASCII")).append(" (");
            for (byte b: data) {
                if (b < 10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            sb.append(')');
            text =  sb.toString();
            x = z = type = 0;
        }
    }

    public String describeData(byte[] data) throws IOException {
        switch (type) {
            case TYPE_TERRAIN:
                return data.length + " bytes of terrain data";
            case TYPE_TILE_ENTITY:
            case TYPE_ENTITY:
                try (NBTInputStream in = new NBTInputStream(new ByteArrayInputStream(data), true)) {
                    Tag tag = in.readTag();
                    return tag.toString();
                }
            default:
                StringBuilder sb = new StringBuilder();
                sb.append(data.length).append(" bytes (");
                for (int i = 0; i < Math.min(data.length, 16); i++) {
                    if (data[i] < 10) {
                        sb.append('0');
                    }
                    sb.append(Integer.toHexString(data[i]));
                }
                if (data.length > 16) {
                    sb.append("...");
                }
                sb.append(')');
                return sb.toString();
        }
    }

    public byte[] toBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(baos)) {
            if (text != null) {
                out.write(text.getBytes("US-ASCII"));
            } else {
                out.writeInt(x);
                out.writeInt(z);
                out.writeByte(type);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        if (text != null) {
            return text;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(x).append(',').append(z).append(" (");
            switch (type) {
                case TYPE_TERRAIN:
                    sb.append("terrain");
                    break;
                case TYPE_TILE_ENTITY:
                    sb.append("tile entity");
                    break;
                case TYPE_ENTITY:
                    sb.append("entity");
                    break;
                default:
                    sb.append(Integer.toHexString(type));
                    break;
            }
            sb.append(')');
            return sb.toString();
        }
    }

    public final int x, z;
    public final byte type;
    public final String text;

    public static final byte TYPE_TERRAIN     = 0x30;
    public static final byte TYPE_TILE_ENTITY = 0x31;
    public static final byte TYPE_ENTITY      = 0x32;
}