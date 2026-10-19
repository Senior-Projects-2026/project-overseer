package com.projectoverseer.shared.world;

/** Terrain data shared by the client and server, independent of rendering */
public enum TileType {
    GRASS(true),
    DIRT(true),
    WATER(false),
    ROCK(false);

    private final boolean walkable;

    TileType(boolean walkable) {
        this.walkable = walkable;
    }

    /** Whether this terrain permits walking; water and rock are obstacles */
    public boolean isWalkable() {
        return walkable;
    }
}
