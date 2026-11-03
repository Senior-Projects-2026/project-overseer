package com.projectoverseer.shared.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectoverseer.shared.world.generation.WorldGenerator;

/**
 * Manages the chunks currently loaded in memory.
 * Generates missing chunks on demand and removes chunks that are too far away.
 */
public final class ChunkManager {
    private final WorldGenerator generator;
    private final Map<Long, Chunk> chunks = new HashMap<>();

    public ChunkManager(WorldGenerator generator) {
        this.generator = generator;
    }

    /**
     * Returns an already loaded chunk or generates it if it does not exist.
     */
    public Chunk getOrGenerate(int chunkX, int chunkY) {
        return chunks.computeIfAbsent(
            key(chunkX, chunkY),
            ignored -> generator.generateChunk(chunkX, chunkY)
        );
    }

    /**
     * Returns a loaded chunk without generating a new one.
     * Returns null if the chunk is not currently loaded.
     */
    public Chunk getChunk(int chunkX, int chunkY) {
        return chunks.get(key(chunkX, chunkY));
    }

    /**
     * Loads all chunks within the specified square radius around the center.
     * Existing chunks are reused instead of being generated again.
     */
    public void loadAround(int centerX, int centerY, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                getOrGenerate(
                    Math.addExact(centerX, x),
                    Math.addExact(centerY, y)
                );
            }
        }
    }

    /**
     * Unloads chunks whose distance from the center exceeds the specified
     * radius on either axis.
     *
     * Uses a square area rather than a circular distance calculation.
     */
    public void unloadOutside(int centerX, int centerY, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        chunks.entrySet().removeIf(entry -> {
            Chunk chunk = entry.getValue();

            return Math.abs((long) chunk.getChunkX() - centerX) > radius
                || Math.abs((long) chunk.getChunkY() - centerY) > radius;
        });
    }

    /**
     * Returns an immutable snapshot of the currently loaded chunks.
     */
    public Collection<Chunk> getLoadedChunks() {
        return List.copyOf(chunks.values());
    }

    /**
     * Returns the number of chunks currently stored in memory.
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }

    /**
     * Creates a unique map key from the two chunk coordinates.
     * The X coordinate occupies the upper 32 bits and Y the lower 32 bits.
     */
    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }
}