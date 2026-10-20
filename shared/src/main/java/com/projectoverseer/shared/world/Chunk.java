package com.projectoverseer.shared.world;

import java.util.Arrays;
import java.util.Objects;

/**
 * A 16 by 16 grid of terrain cells. Coordinates are measured in tiles, not pixels
 * Local coordinates range from 0 to 15, with X increasing right and Y increasing up
 * Chunk (0, 0) starts at world tile (0, 0); chunk (-1, 0) starts at (-16, 0)
 */
public final class Chunk {
    public static final int SIZE = 16;

    private final int chunkX;
    private final int chunkY;
    private final int originTileX;
    private final int originTileY;
    private final TileType[] tiles = new TileType[SIZE * SIZE];

    /**
     * Creates a fully initialized chunk; no cell is null
     *
     * @throws ArithmeticException if the chunk's world tile coordinates overflow an int
     */
    public Chunk(int chunkX, int chunkY, TileType initialType) {
        Objects.requireNonNull(initialType, "initialType");
        this.originTileX = Math.multiplyExact(chunkX, SIZE);
        this.originTileY = Math.multiplyExact(chunkY, SIZE);
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        Arrays.fill(tiles, initialType);
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public int getOriginTileX() {
        return originTileX;
    }

    public int getOriginTileY() {
        return originTileY;
    }

    public TileType getTile(int localX, int localY) {
        return tiles[index(localX, localY)];
    }

    public void setTile(int localX, int localY, TileType type) {
        int index = index(localX, localY);
        tiles[index] = Objects.requireNonNull(type, "type");
    }

    /** Terrain query for collision/pathfinding consumers; does not move or stop entities. */
    public boolean isWalkable(int localX, int localY) {
        return getTile(localX, localY).isWalkable();
    }

    /** Finds the owning chunk on either axis, including negative world coordinates. */
    public static int chunkCoordinate(int worldTileCoordinate) {
        return Math.floorDiv(worldTileCoordinate, SIZE);
    }

    /** Finds the local coordinate (0..15) on either axis. */
    public static int localCoordinate(int worldTileCoordinate) {
        return Math.floorMod(worldTileCoordinate, SIZE);
    }

    private static int index(int localX, int localY) {
        if (localX < 0 || localX >= SIZE || localY < 0 || localY >= SIZE) {
            throw new IndexOutOfBoundsException(
                "Local tile coordinates must be between 0 and " + (SIZE - 1)
                    + ": (" + localX + ", " + localY + ")");
        }
        return localY * SIZE + localX;
    }
}
