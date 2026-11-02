package com.projectoverseer.world;

import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.projectoverseer.entities.Player;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.ResourceType;
import com.projectoverseer.shared.world.WorldResource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Draws resources and all players in a common back-to-front ground-Y order. */
public final class WorldObjectRenderer implements Disposable {
    private final List<Texture> textures = new ArrayList<>();
    private final List<DrawEntry> entries = new ArrayList<>();
    private final TextureRegion tree;
    private final TextureRegion[] rocks;
    private static final float TREE_SHAKE_DURATION = 0.18f;
    private static final float TREE_SHAKE_OFFSET = 2f;
    private final LongMap<Long> shakingTrees = new LongMap<>();

    public WorldObjectRenderer() {
        try {
            // Regions remove empty padding in the original generated PNGs without
            // rewriting them. Anchors and world dimensions are in ResourceAppearance.
            tree = load("world/tree.png", 22, 73, 1057, 1308);
            rocks = new TextureRegion[]{
                load("world/rock-1.png", 346, 278, 759, 609),
                load("world/rock-2.png", 48, 228, 1440, 684),
                load("world/rock-3.png", 234, 173, 1080, 708)
            };
        } catch (RuntimeException e) {
            dispose();
            throw e;
        }
    }

    private TextureRegion load(String path, int x, int y, int width, int height) {
        Texture texture = new Texture(path);
        textures.add(texture);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return new TextureRegion(texture, x, y, width, height);
    }

    /** Caller owns the begun batch, configured with the world camera projection. */
    public void render(SpriteBatch batch, Iterable<Chunk> chunks, OrthographicCamera camera,
                       Player localPlayer, Iterable<Player> otherPlayers) {
        entries.clear();
        VisibleTileBounds view = VisibleTileBounds.fromView(camera.position.x, camera.position.y,
            camera.viewportWidth, camera.viewportHeight, camera.zoom);
        for (Chunk chunk : chunks) {
            for (WorldResource resource : chunk.getResources()) {
                ResourceAppearance.Placement placement = ResourceAppearance.placement(resource);
                // Test the whole sprite, not its anchor tile: a canopy can remain on
                // screen even when the trunk's tile or chunk is outside the view.
                if (!placement.intersects(view)) continue;
                TextureRegion region = resource.type() == ResourceType.TREE ? tree
                    : rocks[ResourceAppearance.rockVariant(resource.tileX(), resource.tileY())];

                float drawX = placement.x();
                if (resource.type() == ResourceType.TREE) {
                    drawX += getTreeShakeX(
                        resource.tileX(),
                        resource.tileY()
                    );
                }
                entries.add(new DrawEntry(
                    placement.groundY(),
                    drawX,
                    region,
                    placement,
                    null
                ));
            }
        }
        addPlayer(localPlayer);
        for (Player player : otherPlayers) addPlayer(player);
        entries.sort(Comparator.comparingDouble(DrawEntry::groundY).reversed()
            .thenComparingDouble(DrawEntry::x));
        for (DrawEntry entry : entries) {
            if (entry.player() != null) {
                entry.player().render(batch);
            } else {
                ResourceAppearance.Placement p = entry.placement();
                batch.draw(
                    entry.region(),
                    entry.x(),
                    p.y(),
                    p.width(),
                    p.height()
                );

            }
        }
        entries.clear();
    }

    private void addPlayer(Player player) {
        // Player sprites are drawn from their bottom-left; Y is the feet baseline.
        entries.add(new DrawEntry(player.getY(), player.getX(), null, null, player));
    }

    public WorldResource findTreeAt(
        Iterable<Chunk> chunks,
        float worldX,
        float worldY
    ) {
        WorldResource selected = null;
        float selectedGroundY = Float.POSITIVE_INFINITY;
        for (Chunk chunk : chunks) {
            for (WorldResource resource : chunk.getResources()) {
                if (resource.type() != ResourceType.TREE) {
                    continue;
                }
                ResourceAppearance.Placement placement =
                    ResourceAppearance.placement(resource);
                boolean inside =
                    worldX >= placement.x()
                        && worldX <= placement.x() + placement.width()
                        && worldY >= placement.y()
                        && worldY <= placement.y() + placement.height();
                if (inside
                    && placement.groundY() < selectedGroundY) {
                    selected = resource;
                    selectedGroundY = placement.groundY();
                }
            }
        }
        return selected;
    }
    private static long treeKey(int tileX, int tileY) {
        return ((long) tileX << 32)
            | (tileY & 0xFFFFFFFFL);
    }

    private float getTreeShakeX(int tileX, int tileY) {
        long key = treeKey(tileX, tileY);
        Long endTime = shakingTrees.get(key);
        if (endTime == null) {
            return 0f;
        }
        long now = TimeUtils.nanoTime();
        if (now >= endTime) {
            shakingTrees.remove(key);
            return 0f;
        }
        long phase = (now / 30_000_000L) % 2L;
        return phase == 0L
            ? -TREE_SHAKE_OFFSET
            : TREE_SHAKE_OFFSET;
    }

    public void triggerTreeShake(int tileX, int tileY) {
        long key = treeKey(tileX, tileY);
        long endTime = TimeUtils.nanoTime()
            + (long) (TREE_SHAKE_DURATION * 1_000_000_000L);

        shakingTrees.put(key, endTime);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) texture.dispose();
        textures.clear();
        entries.clear();
    }

    private record DrawEntry(float groundY, float x, TextureRegion region,
                             ResourceAppearance.Placement placement, Player player) {}
}
