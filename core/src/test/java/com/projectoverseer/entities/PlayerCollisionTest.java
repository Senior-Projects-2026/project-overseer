
package com.projectoverseer.entities;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.TileType;

public class PlayerCollisionTest {

    private Player createPlayer() throws Exception {
        sun.misc.Unsafe unsafe;

        java.lang.reflect.Field field =
            sun.misc.Unsafe.class.getDeclaredField("theUnsafe");

        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe) field.get(null);

        return (Player) unsafe.allocateInstance(Player.class);
    }

    private boolean canMoveTo(Player player, float x, float y,
                              List<Chunk> chunks) throws Exception {
        Method method = Player.class.getDeclaredMethod(
            "canMoveTo", float.class, float.class, List.class);

        method.setAccessible(true);
        return (boolean) method.invoke(player, x, y, chunks);
    }

    private boolean isWalkable(Player player, float x, float y,
                               List<Chunk> chunks) throws Exception {
        Method method = Player.class.getDeclaredMethod(
            "isWalkable", float.class, float.class, List.class);

        method.setAccessible(true);
        return (boolean) method.invoke(player, x, y, chunks);
    }

    @Test
    public void grassAndDirtAreWalkable() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);
        chunk.setTile(1, 1, TileType.DIRT);

        assertTrue(isWalkable(player, 48, 48, List.of(chunk)));
        assertTrue(isWalkable(player, 32, 32, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void waterAndRockAreNotWalkable() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setTile(1, 1, TileType.WATER);
        assertFalse(isWalkable(player, 48, 48, List.of(chunk)));

        chunk.setTile(1, 1, TileType.ROCK);
        assertFalse(isWalkable(player, 48, 48, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void allowsMovementWhenAabbIsWalkable() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        assertTrue(canMoveTo(player, 100, 100, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void blocksMovementWhenAabbOverlapsWater() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setTile(4, 3, TileType.WATER);

        assertFalse(canMoveTo(player, 110, 100, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void blocksMovementWhenAabbOverlapsRock() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        chunk.setTile(4, 3, TileType.ROCK);

        assertFalse(canMoveTo(player, 110, 100, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void blocksMovementOutsideLoadedChunks() throws Exception {
        Player player = createPlayer();
        Chunk chunk = new Chunk(0, 0, TileType.GRASS);

        assertFalse(canMoveTo(player, 500, 100, List.of(chunk)));

        player.dispose();
    }

    @Test
    public void allowsMovementAcrossWalkableChunks() throws Exception {
        Player player = createPlayer();

        Chunk first = new Chunk(0, 0, TileType.GRASS);
        Chunk second = new Chunk(1, 0, TileType.GRASS);

        assertTrue(canMoveTo(player, 500, 100, List.of(first, second)));

        player.dispose();
    }
}
