package org.pepsoft.minecraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jnbt.CompoundTag;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.ObjectMapperHolder.OBJECT_MAPPER;

public class Sign extends TileEntity {
    public Sign() {
        super(ID_SIGN);
    }

    protected Sign(CompoundTag tag) {
        super(tag);
    }

    public String[] getText() {
        return new String[] {getPayload(getString(TAG_TEXT1)).text,
                getPayload(getString(TAG_TEXT2)).text,
                getPayload(getString(TAG_TEXT3)).text,
                getPayload(getString(TAG_TEXT4)).text};
    }

    public void setText(String... text) {
        try {
            setString(TAG_TEXT1, OBJECT_MAPPER.writeValueAsString(new Payload(text[0])));
            setString(TAG_TEXT2, OBJECT_MAPPER.writeValueAsString(new Payload(text[1])));
            setString(TAG_TEXT3, OBJECT_MAPPER.writeValueAsString(new Payload(text[2])));
            setString(TAG_TEXT4, OBJECT_MAPPER.writeValueAsString(new Payload(text[3])));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Payload getPayload(String text) {
        try {
            return OBJECT_MAPPER.readValue(text, Payload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long serialVersionUID = 1L;

    public static class Payload {
        public Payload(String text) {
            this.text = text;
        }

        public final String text;
    }
}