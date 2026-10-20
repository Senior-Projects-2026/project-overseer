package com.projectoverseer.world;

import com.projectoverseer.shared.world.Chunk;

/** Visible world rectangle for the game's axis-aligned orthographic camera. */
record VisibleTileBounds(double left, double bottom, double right, double top) {
    static VisibleTileBounds fromView(float centerX, float centerY,
                                     float viewportWidth, float viewportHeight, float zoom) {
        double halfWidth = (double) viewportWidth * zoom / 2;
        double halfHeight = (double) viewportHeight * zoom / 2;
        return new VisibleTileBounds(centerX - halfWidth, centerY - halfHeight,
            centerX + halfWidth, centerY + halfHeight);
    }

    TileRange inChunk(Chunk chunk) {
        double originX = (double) chunk.getOriginTileX() * TileRenderer.TILE_SIZE;
        double originY = (double) chunk.getOriginTileY() * TileRenderer.TILE_SIZE;
        // Floor includes partially visible cells at the lower edges. Ceil makes the
        // upper bound exclusive and excludes cells merely touching the outer edge.
        return new TileRange(
            clamp(Math.floor((left - originX) / TileRenderer.TILE_SIZE)),
            clamp(Math.floor((bottom - originY) / TileRenderer.TILE_SIZE)),
            clamp(Math.ceil((right - originX) / TileRenderer.TILE_SIZE)),
            clamp(Math.ceil((top - originY) / TileRenderer.TILE_SIZE)));
    }

    private static int clamp(double coordinate) {
        return (int) Math.max(0, Math.min(Chunk.SIZE, coordinate));
    }

    /** Local cell ranges, with exclusive end coordinates. */
    record TileRange(int startX, int startY, int endX, int endY) {
        boolean isEmpty() {
            return startX >= endX || startY >= endY;
        }
    }
}
