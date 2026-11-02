package com.projectoverseer.shared.world;

import java.util.Objects;

/**
 * A resource anchored to one global tile, not a pixel position or inventory item.
 * Its coordinates identify the occupied cell for future server updates.
 * Visuals may extend beyond that cell. Harvesting behavior is implemented separately.
 */
public record WorldResource(ResourceType type, int tileX, int tileY) {
    public WorldResource {
        Objects.requireNonNull(type, "type");
    }
}
