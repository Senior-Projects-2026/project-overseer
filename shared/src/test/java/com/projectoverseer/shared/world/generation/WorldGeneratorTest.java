package com.projectoverseer.shared.world.generation;

import com.projectoverseer.shared.world.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldGeneratorTest {
    private void assertSameContents(Chunk a, Chunk b) {
        assertEquals(a.getChunkX(), b.getChunkX());
        assertEquals(a.getChunkY(), b.getChunkY());
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                assertEquals(a.getTile(x, y), b.getTile(x, y));
                assertEquals(a.getResource(x, y), b.getResource(x, y));
            }
        }
    }

    @Test
    public void independentGeneratorsMatchRegardlessOfRequestOrder() {
        for (long seed : new long[]{0, 123, -42, Long.MIN_VALUE, Long.MAX_VALUE}) {
            WorldGenerator server = new WorldGenerator(seed);
            WorldGenerator client = new WorldGenerator(seed);
            Chunk first = server.generateChunk(-2, 3);
            Chunk second = server.generateChunk(1, -1);
            assertSameContents(second, client.generateChunk(1, -1));
            assertSameContents(first, client.generateChunk(-2, 3));
            assertSameContents(first, server.generateChunk(-2, 3));
        }
    }

    @Test
    public void chunksMatchGlobalSamplesIncludingSeamsAndNegativePositions() {
        WorldGenerator generator = new WorldGenerator(123);
        for (int cy = -2; cy <= 2; cy++) {
            for (int cx = -2; cx <= 2; cx++) {
                Chunk chunk = generator.generateChunk(cx, cy);
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        int wx = cx * Chunk.SIZE + x;
                        int wy = cy * Chunk.SIZE + y;
                        assertEquals(generator.terrainAt(wx, wy), chunk.getTile(x, y));
                        assertEquals(generator.resourceAt(wx, wy), chunk.getResource(x, y));
                    }
                }
            }
        }
    }

    @Test
    public void sampleRegionContainsAllRequiredTypesAndNoResourcesOnWater() {
        WorldGenerator generator = new WorldGenerator(123);
        int water = 0, grass = 0, trees = 0, rocks = 0;
        for (int cy = -4; cy <= 4; cy++) {
            for (int cx = -4; cx <= 4; cx++) {
                Chunk chunk = generator.generateChunk(cx, cy);
                for (int y = 0; y < Chunk.SIZE; y++) {
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        WorldResource resource = chunk.getResource(x, y);
                        if (chunk.getTile(x, y) == TileType.WATER) {
                            water++;
                            assertNull(resource);
                        } else {
                            assertEquals(TileType.GRASS, chunk.getTile(x, y));
                            grass++;
                        }
                        if (resource != null) {
                            assertFalse(chunk.isWalkable(x, y));
                            if (resource.type() == ResourceType.TREE) trees++;
                            else if (resource.type() == ResourceType.ROCK) rocks++;
                        }
                    }
                }
            }
        }
        assertTrue(water > 0 && grass > 0 && trees > 0 && rocks > 0);
    }

    @Test
    public void differentSeedsChangeBothTerrainAndResources() {
        WorldGenerator a = new WorldGenerator(123);
        WorldGenerator b = new WorldGenerator(456);
        int terrainChanges = 0, resourceChanges = 0;
        for (int y = -64; y < 64; y++) {
            for (int x = -64; x < 64; x++) {
                if (a.terrainAt(x, y) != b.terrainAt(x, y)) terrainChanges++;
                if (!java.util.Objects.equals(a.resourceAt(x, y), b.resourceAt(x, y))) resourceChanges++;
            }
        }
        assertTrue(terrainChanges > 0);
        assertTrue(resourceChanges > 0);
    }

    @Test
    public void modifyingOneResultDoesNotChangeBaselineOrAnotherChunk() {
        WorldGenerator generator = new WorldGenerator(123);
        Chunk original = generator.generateChunk(0, 0);
        Chunk changed = generator.generateChunk(0, 0);
        changed.setTile(0, 0, TileType.DIRT);
        changed.setResource(0, 0, ResourceType.TREE);
        assertSameContents(original, generator.generateChunk(0, 0));
        assertNotEquals(TileType.DIRT, original.getTile(0, 0));
    }

    @Test
    public void supportsExtremeValidChunkCoordinates() {
        WorldGenerator generator = new WorldGenerator(-123);
        for (int coordinate : new int[]{Integer.MIN_VALUE / 16, Integer.MAX_VALUE / 16}) {
            Chunk chunk = generator.generateChunk(coordinate, coordinate);
            assertEquals(generator.terrainAt(chunk.getOriginTileX(), chunk.getOriginTileY()), chunk.getTile(0, 0));
            assertEquals(generator.terrainAt(chunk.getOriginTileX() + 15, chunk.getOriginTileY() + 15),
                chunk.getTile(15, 15));
        }
    }

    @Test
    public void resourcesHaveAnEmptyNeighborCellEvenAcrossChunkSeams() {
        for (long seed : new long[]{123, 456, -42}) {
            WorldGenerator generator = new WorldGenerator(seed);
            int checked = 0;
            for (int cy = -2; cy <= 2; cy++) {
                for (int cx = -2; cx <= 2; cx++) {
                    for (WorldResource resource : generator.generateChunk(cx, cy).getResources()) {
                        checked++;
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                if (dx == 0 && dy == 0) continue;
                                assertNull("Adjacent resource at " + resource,
                                    generator.resourceAt(resource.tileX() + dx, resource.tileY() + dy));
                            }
                        }
                    }
                }
            }
            assertTrue(checked > 0);
        }
    }

    @Test
    public void noiseIsContinuousAcrossLatticeEdgesAndNotConstant() {
        PerlinNoise noise = new PerlinNoise(123);
        boolean nonzero = false;
        for (int i = -4; i <= 4; i++) {
            assertEquals(noise.sample(i - 0.000001, 0.37), noise.sample(i + 0.000001, 0.37), 0.00001);
            assertEquals(noise.sample(0.37, i - 0.000001), noise.sample(0.37, i + 0.000001), 0.00001);
            nonzero |= Math.abs(noise.sample(i + 0.31, 0.37)) > 0.001;
        }
        assertTrue(nonzero);
    }
}
