package com.projectoverseer.shared.world;

import java.util.Objects;

public record WorldResource(ResourceType type, int tileX, int tileY, int count) {
    public WorldResource {
        Objects.requireNonNull(type, "type");

        if (count < 0 || (count == 0 && type != ResourceType.BERRY_BUSH)) {
            throw new IllegalArgumentException("Resource count must be at least 1");
        }
    }

    public WorldResource(ResourceType type, int tileX, int tileY) {
        this(type, tileX, tileY, 1);
    }
}