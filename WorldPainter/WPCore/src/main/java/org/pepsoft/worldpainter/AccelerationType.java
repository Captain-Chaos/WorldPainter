package org.pepsoft.worldpainter;

import org.pepsoft.util.SystemUtils;

import java.util.*;

import static org.pepsoft.util.SystemUtils.OS.*;

/**
 * Created by pepijn on 7-5-15.
 */
public enum AccelerationType {
    DEFAULT(WINDOWS, MAC, LINUX, OTHER),
    DIRECT3D(WINDOWS),
    OPENGL(WINDOWS, MAC, LINUX), // TODO: Mac? Really?
    QUARTZ(MAC),
    XRENDER(LINUX),
    UNACCELERATED(WINDOWS, MAC, LINUX, OTHER);

    AccelerationType(SystemUtils.OS... oses) {
        this.oses = EnumSet.copyOf(Arrays.asList(oses));
    }

    public static List<AccelerationType> getForThisOS() {
        List<AccelerationType> types = new ArrayList<>(values().length);
        for (AccelerationType type : values()) {
            if (type.oses.contains(SystemUtils.getOS())) {
                types.add(type);
            }
        }
        return types;
    }

    private final Set<SystemUtils.OS> oses;
}
