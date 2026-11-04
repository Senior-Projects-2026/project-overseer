package com.projectoverseer.shared.world;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.projectoverseer.shared.world.generation.WorldGenerator;

public class ChunkManagerTest {
    private ChunkManager manager;

    @Before
    public void setUp() {
        manager = new ChunkManager(new WorldGenerator(123L));
    }

    @Test
    public void getOrGenerateCreatesChunk() {
        Chunk chunk = manager.getOrGenerate(2, 3);

        assertNotNull(chunk);
        assertEquals(2, chunk.getChunkX());
        assertEquals(3, chunk.getChunkY());
        assertEquals(1, manager.getLoadedChunkCount());
    }

    @Test
    public void getOrGenerateReturnsExistingChunk() {
        Chunk first = manager.getOrGenerate(1, 1);
        Chunk second = manager.getOrGenerate(1, 1);

        assertSame(first, second);
        assertEquals(1, manager.getLoadedChunkCount());
    }

    @Test
    public void getChunkReturnsNullWhenNotLoaded() {
        assertNull(manager.getChunk(5, 5));
    }

    @Test
    public void loadAroundLoadsCorrectNumberOfChunks() {
        manager.loadAround(0, 0, 1);

        assertEquals(9, manager.getLoadedChunkCount());
    }

    @Test
    public void loadAroundLoadsChunksAtExpectedCoordinates() {
        manager.loadAround(2, -1, 1);

        assertNotNull(manager.getChunk(1, -2));
        assertNotNull(manager.getChunk(2, -1));
        assertNotNull(manager.getChunk(3, 0));

        assertNull(manager.getChunk(0, -1));
        assertEquals(9, manager.getLoadedChunkCount());
    }

    @Test
    public void loadAroundRadiusZeroLoadsOnlyCenterChunk() {
        manager.loadAround(4, -3, 0);

        assertEquals(1, manager.getLoadedChunkCount());
        assertNotNull(manager.getChunk(4, -3));
    }

    @Test
    public void loadAroundDoesNotDuplicateExistingChunks() {
        manager.loadAround(0, 0, 1);
        manager.loadAround(0, 0, 1);

        assertEquals(9, manager.getLoadedChunkCount());
    }

    @Test
    public void unloadOutsideRemovesChunksOutsideRadius() {
        manager.loadAround(0, 0, 2);

        manager.unloadOutside(0, 0, 1);

        assertEquals(9, manager.getLoadedChunkCount());
        assertNull(manager.getChunk(2, 0));
        assertNull(manager.getChunk(0, -2));
        assertNotNull(manager.getChunk(1, 1));
    }

    @Test
    public void unloadOutsideKeepsOnlyCenterWhenRadiusIsZero() {
        manager.loadAround(0, 0, 2);

        manager.unloadOutside(0, 0, 0);

        assertEquals(1, manager.getLoadedChunkCount());
        assertNotNull(manager.getChunk(0, 0));
    }

    @Test
    public void getLoadedChunksReturnsSnapshot() {
        manager.loadAround(0, 0, 1);

        Collection<Chunk> chunks = manager.getLoadedChunks();

        manager.getOrGenerate(5, 5);

        assertEquals(9, chunks.size());
        assertEquals(10, manager.getLoadedChunkCount());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getLoadedChunksCannotBeModified() {
        manager.loadAround(0, 0, 0);

        manager.getLoadedChunks().clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadAroundRejectsNegativeRadius() {
        manager.loadAround(0, 0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unloadOutsideRejectsNegativeRadius() {
        manager.unloadOutside(0, 0, -1);
    }

    @Test(expected = ArithmeticException.class)
    public void loadAroundRejectsCoordinateOverflow() {
        manager.loadAround(Integer.MAX_VALUE, 0, 1);
    }

    @Test
    public void unloadOutsideWithLargeRadiusKeepsAllChunks() {
        manager.loadAround(0, 0, 2);
        int before = manager.getLoadedChunkCount();
        
        manager.unloadOutside(0, 0, 1000);
        
        assertEquals(before, manager.getLoadedChunkCount());
    }

    @Test
    public void unloadOutsideWithLargeCenter() {
        manager.loadAround(1000, 1000, 1);
        
        manager.unloadOutside(1000, 1000, 0);
        
        assertEquals(1, manager.getLoadedChunkCount());
        assertNotNull(manager.getChunk(1000, 1000));
    }

    @Test
    public void loadAroundWorksWithNegativeCoordinates() {
        manager.loadAround(-5, -3, 1);
        
        assertEquals(9, manager.getLoadedChunkCount());
        assertNotNull(manager.getChunk(-6, -4));
        assertNotNull(manager.getChunk(-4, -2));
    }

    @Test
    public void getLoadedChunksReturnsCorrectChunks() {
        manager.getOrGenerate(1, 2);
        manager.getOrGenerate(3, 4);
        
        Collection<Chunk> loaded = manager.getLoadedChunks();
        
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream()
            .allMatch(c -> (c.getChunkX() == 1 && c.getChunkY() == 2) ||
                        (c.getChunkX() == 3 && c.getChunkY() == 4)));
    }

    @Test(expected = ArithmeticException.class)
    public void loadAroundRejectsYCoordinateOverflow() {
        manager.loadAround(0, Integer.MAX_VALUE, 1);
    }

    @Test
    public void differentCoordinatesProduceDifferentKeys() {
        Chunk chunk1 = manager.getOrGenerate(1, 2);
        Chunk chunk2 = manager.getOrGenerate(2, 1);
        
        assertNotNull(chunk1);
        assertNotNull(chunk2);
        assertEquals(2, manager.getLoadedChunkCount());
        
        assertNotSame(chunk1, chunk2);
        
        assertEquals(1, chunk1.getChunkX());
        assertEquals(2, chunk1.getChunkY());
        assertEquals(2, chunk2.getChunkX());
        assertEquals(1, chunk2.getChunkY());
    }
}