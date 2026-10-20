
package com.projectoverseer.shared.pathfinding;

import com.projectoverseer.shared.world.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class AStarPathfinder implements Pathfinder {
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 0}, {0, -1}, {-1, 0}
    };

    @Override
    public List<TilePosition> findPath(
            TilePosition start,
            TilePosition end,
            List<Chunk> chunks
    ) {
        Map<Long, Chunk> chunkMap = new HashMap<>();

        for (Chunk chunk : chunks) {
            chunkMap.put(chunkKey(chunk.getChunkX(), chunk.getChunkY()), chunk);
        }

        if (!isWalkable(start, chunkMap) || !isWalkable(end, chunkMap)) {
            return List.of();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(
                Comparator.comparingLong(Node::f)
        );

        Map<TilePosition, TilePosition> cameFrom = new HashMap<>();
        Map<TilePosition, Long> gScore = new HashMap<>();

        gScore.put(start, 0L);
        openSet.add(new Node(start, 0, heuristic(start, end)));

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            TilePosition position = current.position();

            if (current.g() != gScore.getOrDefault(position, Long.MAX_VALUE)) {
                continue;
            }

            if (position.equals(end)) {
                return reconstructPath(cameFrom, end);
            }

            for (int[] direction : DIRECTIONS) {
                long nextX = (long) position.x() + direction[0];
                long nextY = (long) position.y() + direction[1];

                if (nextX < Integer.MIN_VALUE || nextX > Integer.MAX_VALUE
                        || nextY < Integer.MIN_VALUE || nextY > Integer.MAX_VALUE) {
                    continue;
                }

                TilePosition next = new TilePosition((int) nextX, (int) nextY);

                if (!isWalkable(next, chunkMap)) {
                    continue;
                }

                long tentativeG = current.g() + 1;

                if (tentativeG < gScore.getOrDefault(next, Long.MAX_VALUE)) {
                    cameFrom.put(next, position);
                    gScore.put(next, tentativeG);

                    openSet.add(new Node(
                            next,
                            tentativeG,
                            tentativeG + heuristic(next, end)
                    ));
                }
            }
        }

        return List.of();
    }

    private boolean isWalkable(TilePosition position, Map<Long, Chunk> chunks) {
        int chunkX = Chunk.chunkCoordinate(position.x());
        int chunkY = Chunk.chunkCoordinate(position.y());

        Chunk chunk = chunks.get(chunkKey(chunkX, chunkY));

        return chunk != null && chunk.isWalkable(
                Chunk.localCoordinate(position.x()),
                Chunk.localCoordinate(position.y())
        );
    }

    private long heuristic(TilePosition a, TilePosition b) {
        return Math.abs((long) a.x() - b.x())
                + Math.abs((long) a.y() - b.y());
    }

    private List<TilePosition> reconstructPath(
            Map<TilePosition, TilePosition> cameFrom,
            TilePosition end
    ) {
        List<TilePosition> path = new ArrayList<>();
        TilePosition current = end;

        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    private static long chunkKey(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private record Node(TilePosition position, long g, long f) {}
}
