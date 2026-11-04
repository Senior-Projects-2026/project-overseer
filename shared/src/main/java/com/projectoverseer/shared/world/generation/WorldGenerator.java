package com.projectoverseer.shared.world.generation;

import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.TileType;
import com.projectoverseer.shared.world.WorldResource;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared, deterministic baseline generation. Same seed, coordinates and generator
 * version produce identical terrain/resources on server and client, in any order.
 * Generated chunks are fresh mutable instances. Keep harvested/built changes in
 * world state; regenerating a chunk restores its baseline, not its modified state.
 */
public final class WorldGenerator {
    public static final int VERSION = 2;
    private static final double TERRAIN_SCALE = 48.0;
    private static final double WATER_LEVEL = -0.10;
    private static final long TREE_SALT = 0x32a5b7c9d1e3f607L;
    private static final long ROCK_SALT = 0x71e8c2b493a5d60fL;
    private static final long SPACING_SALT = 0x483ac91ef207b65dL;
    private final long seed;
    private final PerlinNoise terrain;
    private final PerlinNoise detail;

    public WorldGenerator(long seed) {
        this.seed = seed;
        terrain = new PerlinNoise(seed);
        detail = new PerlinNoise(seed ^ 0x5deece66dL);
    }

    public long getSeed() {
        return seed;
    }

    /** Fixed starting region until dynamic chunk loading is implemented in #30. */
    public List<Chunk> generateInitialChunks() {
        List<Chunk> chunks = new ArrayList<>();
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                chunks.add(generateChunk(x, y));
            }
        }
        return List.copyOf(chunks);
    }

    /** Refuse incompatible generation rules rather than silently creating a different map. */
    public static WorldGenerator fromServer(long seed, int version) {
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported world generator version: " + version);
        }
        return new WorldGenerator(seed);
    }

    public Chunk generateChunk(int chunkX, int chunkY) {
        Chunk chunk = new Chunk(chunkX, chunkY, TileType.GRASS);
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                int tileX = chunk.getOriginTileX() + x;
                int tileY = chunk.getOriginTileY() + y;
                TileType type = terrainAt(tileX, tileY);
                chunk.setTile(x, y, type);
                ResourceType resource = resourceTypeAt(tileX, tileY, type);
                if (resource != null) chunk.setResource(x, y, resource);
            }
        }
        return chunk;
    }

    /** Global tile coordinates, not pixels or local chunk coordinates. */
    public TileType terrainAt(int tileX, int tileY) {
        double x = tileX / TERRAIN_SCALE;
        double y = tileY / TERRAIN_SCALE;
        double elevation = terrain.sample(x, y) * 0.75 + detail.sample(x * 2, y * 2) * 0.25;
        return elevation < WATER_LEVEL ? TileType.WATER : TileType.GRASS;
    }

    /** Baseline resource at a global tile, or null. Resources never occupy water. */
    public WorldResource resourceAt(int tileX, int tileY) {
        ResourceType type = resourceTypeAt(tileX, tileY, terrainAt(tileX, tileY));
        return type == null ? null : new WorldResource(type, tileX, tileY);
    }

    private ResourceType resourceTypeAt(int x, int y, TileType terrainType) {
        ResourceType candidate = candidateAt(x, y, terrainType);
        if (candidate == null) return null;
        long priority = SeedHash.at(seed ^ SPACING_SALT, x, y);
        // Compare baseline candidates, never generated chunks: request order and
        // chunk boundaries must not affect spacing (including diagonal neighbors).
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                long nx = (long) x + dx, ny = (long) y + dy;
                if (nx < Integer.MIN_VALUE || nx > Integer.MAX_VALUE
                    || ny < Integer.MIN_VALUE || ny > Integer.MAX_VALUE) continue;
                if (candidateAt((int) nx, (int) ny, terrainAt((int) nx, (int) ny)) == null) continue;
                long neighborPriority = SeedHash.at(seed ^ SPACING_SALT, nx, ny);
                int comparison = Long.compareUnsigned(neighborPriority, priority);
                if (comparison < 0 || (comparison == 0 && (ny < y || (ny == y && nx < x)))) {
                    return null;
                }
            }
        }
        return candidate;
    }

    private ResourceType candidateAt(int x, int y, TileType terrainType) {
        if (!terrainType.isWalkable()) return null;
        if (SeedHash.unit(seed ^ TREE_SALT, x, y) < 0.08) return ResourceType.TREE;
        if (SeedHash.unit(seed ^ ROCK_SALT, x, y) < 0.035) return ResourceType.ROCK;
        return null;
    }
}
