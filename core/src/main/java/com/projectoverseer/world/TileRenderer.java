package com.projectoverseer.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.projectoverseer.shared.world.Chunk;
import com.projectoverseer.shared.world.TileType;

import java.util.EnumMap;

/** Draws terrain using a small placeholder atlas owned by this renderer. */
public final class TileRenderer implements Disposable {
    /** One tile occupies 32 world units; a chunk occupies 512 world units. */
    public static final int TILE_SIZE = 32;
    private static final int TEXTURE_SIZE = 16;

    private final Texture atlas;
    private final EnumMap<TileType, TextureRegion> regions = new EnumMap<>(TileType.class);

    public TileRenderer() {
        TileType[] types = TileType.values();
        Pixmap pixels = new Pixmap(TEXTURE_SIZE * types.length, TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        try {
            for (int i = 0; i < types.length; i++) {
                int x = i * TEXTURE_SIZE;
                Color color = colorFor(types[i]);
                pixels.setColor(color);
                pixels.fillRectangle(x, 0, TEXTURE_SIZE, TEXTURE_SIZE);
                pixels.setColor(color.r * 0.8f, color.g * 0.8f, color.b * 0.8f, 1f);
                pixels.drawRectangle(x, 0, TEXTURE_SIZE, TEXTURE_SIZE);
            }
            atlas = new Texture(pixels);
        } finally {
            pixels.dispose();
        }
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (int i = 0; i < types.length; i++) {
            regions.put(types[i], new TextureRegion(atlas, i * TEXTURE_SIZE, 0, TEXTURE_SIZE, TEXTURE_SIZE));
        }
    }

    /**
     * Draws only cells intersecting the axis-aligned camera's visible rectangle.
     * The caller supplies a begun batch with this camera's projection; it owns the batch.
     */
    public void render(SpriteBatch batch, Iterable<Chunk> chunks, OrthographicCamera camera) {
        VisibleTileBounds visible = VisibleTileBounds.fromView(camera.position.x, camera.position.y,
            camera.viewportWidth, camera.viewportHeight, camera.zoom);
        for (Chunk chunk : chunks) {
            VisibleTileBounds.TileRange range = visible.inChunk(chunk);
            if (range.isEmpty()) {
                continue;
            }
            float originX = (float) chunk.getOriginTileX() * TILE_SIZE;
            float originY = (float) chunk.getOriginTileY() * TILE_SIZE;
            for (int y = range.startY(); y < range.endY(); y++) {
                for (int x = range.startX(); x < range.endX(); x++) {
                    batch.draw(regions.get(chunk.getTile(x, y)),
                        originX + x * TILE_SIZE, originY + y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private static Color colorFor(TileType type) {
        return switch (type) {
            case GRASS -> new Color(0.30f, 0.55f, 0.24f, 1f);
            case DIRT -> new Color(0.65f, 0.46f, 0.28f, 1f);
            case WATER -> new Color(0.20f, 0.48f, 0.78f, 1f);
            case ROCK -> new Color(0.48f, 0.49f, 0.53f, 1f);
        };
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
}
