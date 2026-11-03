package com.projectoverseer.shared.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectoverseer.shared.world.generation.WorldGenerator;

public final class ChunkManager {
    private final WorldGenerator generator;
    private final Map<Long, Chunk> chunks = new HashMap<>();

    public ChunkManager(WorldGenerator generator) {
        this.generator = generator;
    }

    public Chunk getOrGenerate(int chunkX, int chunkY) {
        return chunks.computeIfAbsent(
            key(chunkX, chunkY),
            ignored -> generator.generateChunk(chunkX, chunkY)
        );
    }

    public Chunk getChunk(int chunkX, int chunkY) {
        return chunks.get(key(chunkX, chunkY));
    }

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

    public Collection<Chunk> getLoadedChunks() {
        return List.copyOf(chunks.values());
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }
}