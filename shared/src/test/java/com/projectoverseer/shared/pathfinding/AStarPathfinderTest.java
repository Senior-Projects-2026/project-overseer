
package com.projectoverseer.shared.pathfinding;

import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.TileType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AStarPathfinderTest {
    private final Pathfinder pathfinder = new AStarPathfinder();

    private List<Chunk> createMap(int minChunkX, int maxChunkX,
                                  int minChunkY, int maxChunkY) {
        List<Chunk> chunks = new ArrayList<>();

        for (int y = minChunkY; y <= maxChunkY; y++) {
            for (int x = minChunkX; x <= maxChunkX; x++) {
                chunks.add(new Chunk(x, y, TileType.GRASS));
            }
        }

        return chunks;
    }

    private Chunk getChunk(List<Chunk> chunks, int x, int y) {
        return chunks.stream()
                .filter(c -> c.getChunkX() == Chunk.chunkCoordinate(x)
                        && c.getChunkY() == Chunk.chunkCoordinate(y))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private void setTerrain(List<Chunk> chunks, int x, int y, TileType type) {
        getChunk(chunks, x, y).setTile(
                Chunk.localCoordinate(x),
                Chunk.localCoordinate(y),
                type
        );
    }

    private boolean isWalkable(List<Chunk> chunks, TilePosition position) {
        return getChunk(chunks, position.x(), position.y()).isWalkable(
                Chunk.localCoordinate(position.x()),
                Chunk.localCoordinate(position.y())
        );
    }

    @Test
    public void findsPathAroundWall() {
        List<Chunk> chunks = createMap(0, 0, 0, 0);

        for (int y = 0; y <= 5; y++) {
            setTerrain(chunks, 4, y, TileType.ROCK);
        }

        TilePosition start = new TilePosition(1, 1);
        TilePosition end = new TilePosition(8, 1);

        List<TilePosition> path = pathfinder.findPath(start, end, chunks);

        assertFalse(path.isEmpty());
        assertEquals(start, path.get(0));
        assertEquals(end, path.get(path.size() - 1));
        assertTrue(path.size() > 8);

        for (TilePosition position : path) {
            assertTrue(isWalkable(chunks, position));
        }

        for (int i = 1; i < path.size(); i++) {
            TilePosition previous = path.get(i - 1);
            TilePosition current = path.get(i);

            int distance = Math.abs(current.x() - previous.x())
                    + Math.abs(current.y() - previous.y());

            assertEquals(1, distance);
        }
    }

    @Test
    public void returnsEmptyWhenDestinationIsBlocked() {
        List<Chunk> chunks = createMap(0, 0, 0, 0);
        setTerrain(chunks, 5, 5, TileType.WATER);

        List<TilePosition> path = pathfinder.findPath(
                new TilePosition(1, 1),
                new TilePosition(5, 5),
                chunks
        );

        assertTrue(path.isEmpty());
    }

    @Test
    public void returnsSingleTileWhenStartEqualsEnd() {
        List<Chunk> chunks = createMap(0, 0, 0, 0);
        TilePosition start = new TilePosition(5, 5);

        List<TilePosition> path = pathfinder.findPath(start, start, chunks);

        assertEquals(List.of(start), path);
    }

    @Test
    public void findsPathAcrossChunkBoundaryAndNegativeCoordinates() {
        List<Chunk> chunks = createMap(-1, 0, 0, 0);

        TilePosition start = new TilePosition(-2, 5);
        TilePosition end = new TilePosition(2, 5);

        List<TilePosition> path = pathfinder.findPath(start, end, chunks);

        assertFalse(path.isEmpty());
        assertEquals(start, path.get(0));
        assertEquals(end, path.get(path.size() - 1));
        assertEquals(5, path.size());

        for (TilePosition position : path) {
            assertTrue(isWalkable(chunks, position));
        }
    }

    @Test
    public void returnsEmptyWhenNoRouteExists() {
        List<Chunk> chunks = createMap(0, 0, 0, 0);

        for (int y = 0; y < Chunk.SIZE; y++) {
            setTerrain(chunks, 5, y, TileType.ROCK);
        }

        List<TilePosition> path = pathfinder.findPath(
                new TilePosition(1, 1),
                new TilePosition(8, 1),
                chunks
        );

        assertTrue(path.isEmpty());
    }

    @Test
    public void returnsEmptyWhenDestinationChunkIsMissing() {
        List<Chunk> chunks = createMap(0, 0, 0, 0);

        List<TilePosition> path = pathfinder.findPath(
                new TilePosition(1, 1),
                new TilePosition(20, 1),
                chunks
        );

        assertTrue(path.isEmpty());
    }

    @Test
    public void throwsWhenArgumentsAreNull() {
        AStarPathfinder pathfinder = new AStarPathfinder();
        List<Chunk> chunks = createMap(0, 0, 0, 0);


        assertThrows(IllegalArgumentException.class,
                () -> pathfinder.findPath(null, new TilePosition(1, 1), chunks));

        assertThrows(IllegalArgumentException.class,
                () -> pathfinder.findPath(new TilePosition(1, 1), null, chunks));

        assertThrows(IllegalArgumentException.class,
                () -> pathfinder.findPath(
                        new TilePosition(1, 1), new TilePosition(2, 2), null));

        chunks.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> pathfinder.findPath(
                        new TilePosition(1, 1), new TilePosition(2, 2), chunks));
    }
}
