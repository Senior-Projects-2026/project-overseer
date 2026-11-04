package com.projectoverseer.shared.world;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChunkTest {
    @Test
    public void initializesAll256Cells() {
        Chunk chunk = new Chunk(2, -3, TileType.GRASS);
        assertEquals(16, Chunk.SIZE);
        assertEquals(2, chunk.getChunkX());
        assertEquals(-3, chunk.getChunkY());
        assertEquals(32, chunk.getOriginTileX());
        assertEquals(-48, chunk.getOriginTileY());
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                assertEquals(TileType.GRASS, chunk.getTile(x, y));
                assertTrue(chunk.isWalkable(x, y));
            }
        }
    }

    @Test
    public void obstacleChangesOnlyTheSelectedCellAndChunk() {
        Chunk chunk = new Chunk(0, 0, TileType.DIRT);
        Chunk other = new Chunk(1, 0, TileType.DIRT);
        chunk.setTile(15, 0, TileType.ROCK);
        chunk.setTile(0, 15, TileType.WATER);
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                boolean obstacle = (x == 15 && y == 0) || (x == 0 && y == 15);
                assertEquals(!obstacle, chunk.isWalkable(x, y));
                assertEquals(TileType.DIRT, other.getTile(x, y));
            }
        }
        chunk.setTile(15, 0, TileType.GRASS);
        assertTrue(chunk.isWalkable(15, 0));
        assertFalse(chunk.isWalkable(0, 15));
    }

    @Test
    public void convertsWorldCoordinatesAcrossChunkBoundaries() {
        int[][] cases = {
            {-17, -2, 15}, {-16, -1, 0}, {-1, -1, 15},
            {0, 0, 0}, {15, 0, 15}, {16, 1, 0}, {31, 1, 15}, {32, 2, 0},
            {Integer.MIN_VALUE, -134217728, 0}, {Integer.MAX_VALUE, 134217727, 15}
        };
        for (int[] example : cases) {
            int chunkCoordinate = Chunk.chunkCoordinate(example[0]);
            int localCoordinate = Chunk.localCoordinate(example[0]);
            assertEquals(example[1], chunkCoordinate);
            assertEquals(example[2], localCoordinate);
            Chunk chunk = new Chunk(chunkCoordinate, 0, TileType.GRASS);
            assertEquals(example[0], chunk.getOriginTileX() + localCoordinate);
        }
    }

    @Test
    public void rejectsCoordinatesOutsideChunkInsteadOfAccessingAnotherRow() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        int[][] invalid = {{-1, 0}, {16, 0}, {0, -1}, {0, 16}, {16, 15}};
        for (int[] point : invalid) {
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getTile(point[0], point[1]));
            assertThrows(IndexOutOfBoundsException.class,
                () -> chunk.setTile(point[0], point[1], TileType.ROCK));
            assertThrows(IndexOutOfBoundsException.class,
                () -> chunk.isWalkable(point[0], point[1]));
        }
    }

    @Test
    public void rejectsNullTerrainWithoutChangingTheCell() {
        assertThrows(NullPointerException.class, () -> new Chunk(0, 0, null));
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        assertThrows(NullPointerException.class, () -> chunk.setTile(0, 0, null));
        assertEquals(TileType.GRASS, chunk.getTile(0, 0));
    }

    @Test
    public void rejectsChunkCoordinatesThatOverflowWorldCoordinates() {
        assertThrows(ArithmeticException.class,
            () -> new Chunk(Integer.MAX_VALUE, 0, TileType.GRASS));
        assertThrows(ArithmeticException.class,
            () -> new Chunk(0, Integer.MIN_VALUE, TileType.GRASS));
    }
}
