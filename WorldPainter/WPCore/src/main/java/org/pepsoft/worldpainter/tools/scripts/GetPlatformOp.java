package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

public class GetPlatformOp extends AbstractOperation<Platform> {
    protected GetPlatformOp(ScriptingContext context) {
        super(context);
    }

    public GetPlatformOp withId(String id) {
        this.id = id;
        return this;
    }

    public GetPlatformOp withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Platform go() throws ScriptException {
        if (id != null) {
            if (name != null) {
                throw new ScriptException("Both name and id set");
            }
            final Platform platform = Platform.getById(id);
            if (platform == null) {
                throw new ScriptException("There is no map format with ID \"" + id + "\"");
            } else {
                return platform;
            }
        } else if (name != null) {
            for (Platform platform: PlatformManager.getInstance().getAllPlatforms()) {
                if (platform.displayName.equalsIgnoreCase(name)) {
                    return platform;
                }
            }
            throw new ScriptException("There is no map format with name \"" + name + "\"");
        } else {
            throw new ScriptException("name or id not set");
        }
    }

    private String id, name;
}
