package com.projectoverseer.world;

import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.TileType;
import org.junit.Test;

import static org.junit.Assert.*;

public class VisibleTileBoundsTest {
    private final Chunk origin = new Chunk(0, 0, TileType.GRASS);

    @Test
    public void exactTileEdgesExcludeAdjacentCells() {
        assertEquals(new VisibleTileBounds.TileRange(1, 2, 3, 4),
            new VisibleTileBounds(32, 64, 96, 128).inChunk(origin));
    }

    @Test
    public void partialCellsAreIncludedAtEveryEdge() {
        assertEquals(new VisibleTileBounds.TileRange(0, 1, 4, 5),
            new VisibleTileBounds(31.5, 63.5, 96.5, 128.5).inChunk(origin));
    }

    @Test
    public void excludesChunksOutsideOrOnlyTouchingTheView() {
        VisibleTileBounds[] views = {
            new VisibleTileBounds(-64, 0, 0, 32),
            new VisibleTileBounds(512, 0, 600, 32),
            new VisibleTileBounds(0, -64, 32, 0),
            new VisibleTileBounds(0, 512, 32, 600),
            new VisibleTileBounds(1000, 1000, 1100, 1100)
        };
        for (VisibleTileBounds view : views) {
            assertTrue(view.inChunk(origin).isEmpty());
        }
    }

    @Test
    public void clipsLargeViewToTheEntireChunk() {
        assertEquals(new VisibleTileBounds.TileRange(0, 0, 16, 16),
            new VisibleTileBounds(-1000, -1000, 1000, 1000).inChunk(origin));
    }

    @Test
    public void includesBothSidesOfPositiveAndNegativeChunkSeams() {
        VisibleTileBounds atZero = new VisibleTileBounds(-1, -1, 1, 1);
        assertEquals(new VisibleTileBounds.TileRange(15, 15, 16, 16),
            atZero.inChunk(new Chunk(-1, -1, TileType.GRASS)));
        assertEquals(new VisibleTileBounds.TileRange(0, 0, 1, 1), atZero.inChunk(origin));
        VisibleTileBounds at512 = new VisibleTileBounds(511, 511, 513, 513);
        assertEquals(new VisibleTileBounds.TileRange(15, 15, 16, 16), at512.inChunk(origin));
        assertEquals(new VisibleTileBounds.TileRange(0, 0, 1, 1),
            at512.inChunk(new Chunk(1, 1, TileType.GRASS)));
    }

    @Test
    public void accountsForCameraPositionViewportAndZoom() {
        assertEquals(new VisibleTileBounds.TileRange(7, 7, 9, 9),
            VisibleTileBounds.fromView(256, 256, 128, 128, 0.5f).inChunk(origin));
        assertEquals(new VisibleTileBounds.TileRange(4, 4, 12, 12),
            VisibleTileBounds.fromView(256, 256, 128, 128, 2f).inChunk(origin));
        assertEquals(new VisibleTileBounds.TileRange(5, 6, 9, 8),
            VisibleTileBounds.fromView(224, 224, 128, 64, 1f).inChunk(origin));
    }

    @Test
    public void matchesRectangleIntersectionWhileMovingAcrossTheMap() {
        // Independent rectangle-overlap check catches missing cells and unnecessary draws.
        for (float center : new float[]{-520.5f, -32, -0.5f, 0, 31.5f, 256, 511.5f, 1024}) {
            for (float zoom : new float[]{0.5f, 1, 2}) {
                VisibleTileBounds view = VisibleTileBounds.fromView(center, center, 640, 360, zoom);
                for (int cy = -1; cy <= 1; cy++) {
                    for (int cx = -1; cx <= 1; cx++) {
                        Chunk chunk = new Chunk(cx, cy, TileType.GRASS);
                        VisibleTileBounds.TileRange range = view.inChunk(chunk);
                        for (int y = 0; y < Chunk.SIZE; y++) {
                            for (int x = 0; x < Chunk.SIZE; x++) {
                                double left = (chunk.getOriginTileX() + x) * 32.0;
                                double bottom = (chunk.getOriginTileY() + y) * 32.0;
                                boolean intersects = left < view.right() && left + 32 > view.left()
                                    && bottom < view.top() && bottom + 32 > view.bottom();
                                boolean included = x >= range.startX() && x < range.endX()
                                    && y >= range.startY() && y < range.endY();
                                assertEquals(intersects, included);
                            }
                        }
                    }
                }
            }
        }
    }
}
