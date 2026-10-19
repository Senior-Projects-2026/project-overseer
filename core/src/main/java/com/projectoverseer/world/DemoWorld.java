package com.projectoverseer.world;

import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.TileType;

import java.util.ArrayList;
import java.util.List;

/** Fixed client-side preview terrain, to be replaced by the world's actual data source. */
public final class DemoWorld {
    private DemoWorld() {
    }

    /** Nine chunks spanning world tiles [-16, 31] on both axes. */
    public static List<Chunk> createChunks() {
        List<Chunk> chunks = new ArrayList<>();
        for (int chunkY = -1; chunkY <= 1; chunkY++) {
            for (int chunkX = -1; chunkX <= 1; chunkX++) {
                Chunk chunk = new Chunk(chunkX, chunkY, TileType.GRASS);
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        int worldX = chunk.getOriginTileX() + x;
                        int worldY = chunk.getOriginTileY() + y;
                        chunk.setTile(x, y, terrainAt(worldX, worldY));
                    }
                }
                chunks.add(chunk);
            }
        }
        return List.copyOf(chunks);
    }

    private static TileType terrainAt(int x, int y) {
        // Paths cross chunk boundaries, making alignment easy to check while walking.
        if (y == 8 || y == 9 || x == 12 || x == 13) {
            return TileType.DIRT;
        }
        if (x >= 16 && x <= 21 && y >= 11 && y <= 15) {
            return TileType.WATER;
        }
        if (x >= 5 && x <= 8 && y >= 13 && y <= 15) {
            return TileType.ROCK;
        }
        return TileType.GRASS;
    }
}
