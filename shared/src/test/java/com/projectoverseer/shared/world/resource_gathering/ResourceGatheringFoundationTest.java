
package com.projectoverseer.shared.world.resource_gathering;


import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.TileType;
import com.projectoverseer.shared.world.WorldResource;
import com.projectoverseer.shared.world.generation.WorldGenerator;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ResourceGatheringFoundationTest {

    @Test
    public void bothResourceTypesCanBeRemovedWithoutChangingTerrain() {
        for (ResourceType type : ResourceType.values()) {
            Chunk chunk = new Chunk(0, 0, TileType.GRASS);
            chunk.setResource(3, 4, type);

            assertFalse(chunk.isWalkable(3, 4));

            assertEquals(
                new WorldResource(type, 3, 4),
                chunk.removeResource(3, 4)
            );

            assertNull(chunk.getResource(3, 4));
            assertTrue(chunk.isWalkable(3, 4));
            assertEquals(TileType.GRASS, chunk.getTile(3, 4));
        }
    }

    @Test
    public void destroyingOneObjectDoesNotDestroyItsNeighbor() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setResource(4, 4, ResourceType.TREE);
        chunk.setResource(5, 4, ResourceType.ROCK);

        chunk.removeResource(4, 4);

        assertNull(chunk.getResource(4, 4));
        assertEquals(
            ResourceType.ROCK,
            chunk.getResource(5, 4).type()
        );
        assertEquals(1, chunk.getResources().size());
    }

    @Test
    public void duplicateRemovalCannotYieldAnotherResource() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setResource(2, 2, ResourceType.TREE);

        assertNotNull(chunk.removeResource(2, 2));
        assertNull(chunk.removeResource(2, 2));
        assertNull(chunk.removeResource(2, 2));

        assertTrue(chunk.getResources().isEmpty());
    }

    @Test
    public void removalDoesNotTurnWaterIntoWalkableLand() {
        Chunk chunk = new Chunk(0, 0, TileType.WATER);

        chunk.setResource(3, 2, ResourceType.ROCK);
        chunk.removeResource(3, 2);

        assertEquals(TileType.WATER, chunk.getTile(3, 2));
        assertFalse(chunk.isWalkable(3, 2));
    }

    @Test
    public void resourceIdentityIsStableAcrossNegativeChunkSeams() {
        for (int worldX : new int[]{
            -17, -16, -1, 0, 15, 16, 31
        }) {
            for (int worldY : new int[]{
                -17, -16, -1, 0, 15, 16
            }) {
                Chunk chunk = new Chunk(
                    Chunk.chunkCoordinate(worldX),
                    Chunk.chunkCoordinate(worldY),
                    TileType.GRASS
                );

                int lx = Chunk.localCoordinate(worldX);
                int ly = Chunk.localCoordinate(worldY);

                chunk.setResource(lx, ly, ResourceType.TREE);

                assertEquals(
                    worldX,
                    chunk.getResource(lx, ly).tileX()
                );

                assertEquals(
                    worldY,
                    chunk.getResource(lx, ly).tileY()
                );

                assertNotNull(chunk.removeResource(lx, ly));
                assertTrue(chunk.getResources().isEmpty());
            }
        }
    }

    @Test
    public void generationIsDeterministicForTreeAndRockOnDifferentClients() {
        for (long seed : new long[]{
            123L, 456L, -42L, Long.MIN_VALUE
        }) {
            WorldGenerator server = new WorldGenerator(seed);
            WorldGenerator client = new WorldGenerator(seed);

            for (int cx = -2; cx <= 2; cx++) {
                for (int cy = -2; cy <= 2; cy++) {
                    Chunk a = server.generateChunk(cx, cy);
                    Chunk b = client.generateChunk(cx, cy);

                    assertEquals(
                        a.getResources(),
                        b.getResources()
                    );
                }
            }
        }
    }

    @Test
    public void removedResourceHasSameIdentityInIndependentWorldCopies() {
        WorldGenerator generator = new WorldGenerator(123);

        List<Chunk> worldA = generator.generateInitialChunks();
        List<Chunk> worldB = generator.generateInitialChunks();

        WorldResource picked = null;

        for (Chunk chunk : worldA) {
            if (!chunk.getResources().isEmpty()) {
                picked = chunk.getResources().get(0);
                break;
            }
        }

        assertNotNull(
            "Seed should generate at least one resource",
            picked
        );

        Chunk a = findChunk(
            worldA, picked.tileX(), picked.tileY()
        );

        Chunk b = findChunk(
            worldB, picked.tileX(), picked.tileY()
        );

        int x = Chunk.localCoordinate(picked.tileX());
        int y = Chunk.localCoordinate(picked.tileY());

        assertEquals(picked, a.removeResource(x, y));
        assertEquals(picked, b.removeResource(x, y));

        assertNull(a.getResource(x, y));
        assertNull(b.getResource(x, y));
    }

    @Test
    public void newlyGeneratedChunkRestoresBaselineSoSessionNeedsDestroyedState() {
        WorldGenerator generator = new WorldGenerator(123);

        WorldResource resource = generator
            .generateInitialChunks()
            .stream()
            .flatMap(c -> c.getResources().stream())
            .findFirst()
            .orElseThrow();

        Chunk modified = generator.generateChunk(
            Chunk.chunkCoordinate(resource.tileX()),
            Chunk.chunkCoordinate(resource.tileY())
        );

        int localX = Chunk.localCoordinate(resource.tileX());
        int localY = Chunk.localCoordinate(resource.tileY());

        assertNotNull(
            modified.removeResource(localX, localY)
        );

        assertNull(modified.getResource(localX, localY));

        Chunk regenerated = generator.generateChunk(
            modified.getChunkX(),
            modified.getChunkY()
        );

        assertEquals(
            "Baseline generation is not a harvested-state store",
            resource,
            regenerated.getResource(localX, localY)
        );
    }

    @Test
    public void resourceSnapshotsAreImmutableAndRemainStableAfterHarvest() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setResource(1, 1, ResourceType.TREE);

        List<WorldResource> snapshot = chunk.getResources();

        chunk.removeResource(1, 1);

        assertEquals(1, snapshot.size());
        assertTrue(chunk.getResources().isEmpty());

        assertThrows(
            UnsupportedOperationException.class,
            snapshot::clear
        );
    }

    @Test
    public void generatedResourcesHaveUniqueCoordinatesAndNoWaterOverlap() {
        WorldGenerator generator = new WorldGenerator(123);

        Set<Long> locations = new HashSet<>();

        int trees = 0;
        int rocks = 0;

        for (Chunk chunk : generator.generateInitialChunks()) {
            for (WorldResource resource : chunk.getResources()) {

                long key =
                    ((long) resource.tileX() << 32)
                        | (resource.tileY() & 0xffffffffL);

                assertTrue(
                    "Resource coordinate used more than once",
                    locations.add(key)
                );

                int x = Chunk.localCoordinate(resource.tileX());
                int y = Chunk.localCoordinate(resource.tileY());

                assertTrue(chunk.getTile(x, y).isWalkable());

                if (resource.type() == ResourceType.TREE) {
                    trees++;
                }

                if (resource.type() == ResourceType.ROCK) {
                    rocks++;
                }
            }
        }

        assertTrue("No trees in test world", trees > 0);
        assertTrue("No rocks in test world", rocks > 0);
    }

    @Test
    public void invalidChunkCoordinatesDoNotMutateResources() {
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setResource(0, 0, ResourceType.ROCK);

        for (int[] invalid : new int[][]{
            {-1, 0}, {0, -1}, {16, 0}, {0, 16}
        }) {
            assertThrows(
                IndexOutOfBoundsException.class,
                () -> chunk.removeResource(
                    invalid[0], invalid[1]
                )
            );
        }

        assertEquals(
            ResourceType.ROCK,
            chunk.getResource(0, 0).type()
        );
    }

    private static Chunk findChunk(
        List<Chunk> chunks,
        int worldX,
        int worldY
    ) {
        int cx = Chunk.chunkCoordinate(worldX);
        int cy = Chunk.chunkCoordinate(worldY);

        return chunks.stream()
            .filter(c ->
                c.getChunkX() == cx
                    && c.getChunkY() == cy
            )
            .findFirst()
            .orElseThrow();
    }
}
