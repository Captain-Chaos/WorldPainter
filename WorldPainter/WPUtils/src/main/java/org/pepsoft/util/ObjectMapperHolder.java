package org.pepsoft.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ObjectMapperHolder {
    private ObjectMapperHolder() {
        // Prevent instantiation
    }

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}