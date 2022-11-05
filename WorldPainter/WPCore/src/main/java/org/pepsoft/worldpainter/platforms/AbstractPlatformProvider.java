package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractPlatformProvider extends AbstractPlugin implements PlatformProvider {
    protected AbstractPlatformProvider(String version, Collection<Platform> platforms, String name) {
        super(name, version);
        this.platforms = ImmutableSet.copyOf(platforms);
    }

    @Override
    public final Collection<Platform> getKeys() {
        return platforms;
    }

    protected final void ensurePlatformSupported(Platform platform) {
        if (! platforms.contains(platform)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    private final Set<Platform> platforms;
}