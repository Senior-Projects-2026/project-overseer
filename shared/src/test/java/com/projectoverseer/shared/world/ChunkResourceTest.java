package com.projectoverseer.shared.world;

import org.junit.Test;
import com.projectoverseer.shared.pathfinding.AStarPathfinder;
import com.projectoverseer.shared.pathfinding.TilePosition;

import java.util.List;

import static org.junit.Assert.*;

public class ChunkResourceTest {
    @Test
    public void newChunkHasNoResources() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        assertTrue(chunk.getResources().isEmpty());
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                assertNull(chunk.getResource(x, y));
            }
        }
    }

    @Test
    public void resourceBlocksOnlyItsCellAndRemovalPreservesTerrain() {
        for (ResourceType type : ResourceType.values()) {
            Chunk chunk = new Chunk(0, 0, TileType.GRASS);
            chunk.setResource(5, 3, type);
            assertEquals(TileType.GRASS, chunk.getTile(5, 3));
            assertFalse(chunk.isWalkable(5, 3));
            assertTrue(chunk.isWalkable(4, 3));
            assertEquals(new WorldResource(type, 5, 3), chunk.removeResource(5, 3));
            assertTrue(chunk.isWalkable(5, 3));
            assertEquals(TileType.GRASS, chunk.getTile(5, 3));
            assertNull(chunk.removeResource(5, 3));
        }
    }

    @Test
    public void resourceCoordinatesAreGlobalIncludingNegativeChunks() {
        Chunk negative = new Chunk(-1, -2, TileType.GRASS);
        negative.setResource(15, 0, ResourceType.TREE);
        assertEquals(new WorldResource(ResourceType.TREE, -1, -32), negative.getResource(15, 0));
        Chunk positive = new Chunk(1, 2, TileType.GRASS);
        positive.setResource(0, 15, ResourceType.ROCK);
        assertEquals(new WorldResource(ResourceType.ROCK, 16, 47), positive.getResource(0, 15));
    }

    @Test
    public void replacingResourceDoesNotDuplicateItAndSnapshotsStayUnchanged() {
        Chunk chunk = new Chunk(0, 0, TileType.DIRT);
        chunk.setResource(2, 3, ResourceType.TREE);
        var snapshot = chunk.getResources();
        chunk.setResource(2, 3, ResourceType.ROCK);
        assertEquals(1, chunk.getResources().size());
        assertEquals(ResourceType.ROCK, chunk.getResource(2, 3).type());
        assertEquals(ResourceType.TREE, snapshot.get(0).type());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        assertEquals(TileType.DIRT, chunk.getTile(2, 3));
    }

    @Test
    public void removingResourceDoesNotMakeBlockedTerrainWalkable() {
        Chunk chunk = new Chunk(0, 0, TileType.WATER);
        chunk.setResource(0, 0, ResourceType.ROCK);
        chunk.removeResource(0, 0);
        assertFalse(chunk.isWalkable(0, 0));
    }

    @Test
    public void rejectsInvalidCoordinatesAndNullTypesWithoutReplacingResource() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        chunk.setResource(0, 0, ResourceType.TREE);
        assertThrows(NullPointerException.class, () -> chunk.setResource(0, 0, null));
        assertEquals(ResourceType.TREE, chunk.getResource(0, 0).type());
        assertThrows(NullPointerException.class, () -> new WorldResource(null, 0, 0));
        for (int[] cell : new int[][]{{-1, 0}, {16, 0}, {0, -1}, {0, 16}}) {
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.getResource(cell[0], cell[1]));
            assertThrows(IndexOutOfBoundsException.class,
                () -> chunk.setResource(cell[0], cell[1], ResourceType.ROCK));
            assertThrows(IndexOutOfBoundsException.class, () -> chunk.removeResource(cell[0], cell[1]));
        }
    }

    @Test
    public void pathfinderAvoidsResourceAndUsesCellAfterRemoval() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        var pathfinder = new AStarPathfinder();
        var start = new TilePosition(1, 1);
        var end = new TilePosition(3, 1);
        var obstacle = new TilePosition(2, 1);
        chunk.setResource(2, 1, ResourceType.TREE);
        var path = pathfinder.findPath(start, end, List.of(chunk));
        assertEquals(5, path.size());
        assertFalse(path.contains(obstacle));
        chunk.removeResource(2, 1);
        assertEquals(List.of(start, obstacle, end),
            pathfinder.findPath(start, end, List.of(chunk)));
    }
}
