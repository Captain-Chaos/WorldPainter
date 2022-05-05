package org.pepsoft.worldpainter.platforms;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.util.Collection;

import static java.util.Collections.singleton;

public abstract class AbstractPlatformProvider extends AbstractPlugin implements PlatformProvider {
    protected AbstractPlatformProvider(String version, Platform platform) {
        super(platform.displayName.replaceAll("\\s", "") + "PlatformProvider", version);
        this.platform = platform;
    }

    @Override
    public final Collection<Platform> getKeys() {
        return singleton(platform);
    }

    public final Platform getPlatform() {
        return platform;
    }

    protected final void ensurePlatformSupported(Platform platform) {
        if (platform != this.platform) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    private final Platform platform;
}