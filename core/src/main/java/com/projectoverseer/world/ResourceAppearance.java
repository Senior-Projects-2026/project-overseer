package com.projectoverseer.world;

import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;

/** Client-only sprite sizes and anchors; resource occupancy remains one tile. */
final class ResourceAppearance {
    private ResourceAppearance() {}

    static int rockVariant(int tileX, int tileY) {
        long value = ((long) tileX << 32) | (tileY & 0xffffffffL);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) Math.floorMod(value, 3L);
    }

    static Placement placement(WorldResource resource) {
        float width;
        float height;
        if (resource.type() == ResourceType.TREE) {
            width = 64f;
            height = 80f;
        } else {
            int variant = rockVariant(resource.tileX(), resource.tileY());
            width = 32f;
            height = switch (variant) {
                case 0 -> 26f;
                case 1 -> 16f;
                default -> 21f;
            };
        }
        float centerX = ((float) resource.tileX() + 0.5f) * TileRenderer.TILE_SIZE;
        float groundY = ((float) resource.tileY() + 0.5f) * TileRenderer.TILE_SIZE;
        return new Placement(centerX - width / 2f, groundY - 4f, width, height, groundY);
    }

    record Placement(float x, float y, float width, float height, float groundY) {
        boolean intersects(VisibleTileBounds view) {
            return x < view.right() && x + width > view.left()
                && y < view.top() && y + height > view.bottom();
        }
    }
}
