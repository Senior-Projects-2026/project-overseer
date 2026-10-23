package com.projectoverseer.shared.pathfinding;

import com.projectoverseer.shared.world.Chunk;

import java.util.List;

public interface Pathfinder {

    List<TilePosition> findPath(
            TilePosition start,
            TilePosition end,
            List<Chunk> chunks
        ) throws IllegalArgumentException;
}
