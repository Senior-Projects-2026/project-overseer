package com.projectoverseer.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import com.projectoverseer.Assets;
import com.projectoverseer.config.ClientConfig;
import com.projectoverseer.gameplay.PlayerStats;
import com.projectoverseer.gameplay.Inventory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.projectoverseer.shared.world.Chunk;

public class Player {
    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int UP_LEFT = 4;
    public static final int UP_RIGHT = 5;
    public static final int DOWN_LEFT = 6;
    public static final int DOWN_RIGHT = 7;
    private static final float ATTACK_ANIMATION_DURATION = 0.18f;
    private static final float SWING_START_OFFSET = -70f;
    private static final float SWING_END_OFFSET = 70f;


    private final PlayerStats stats = new PlayerStats();
    private final Inventory inventory = new Inventory();

    private float x = ClientConfig.getFloat("player.spawn.x", 200f);
    private float y = ClientConfig.getFloat("player.spawn.y", 200f);
    private float speed = ClientConfig.getFloat("player.movement.speed", 100f);
    private final Vector2 movement = new Vector2();
    private final boolean localPlayer;
    private int direction = DOWN;
    private boolean moving;

    private Texture spriteSheet;
    private final Map<String, HeldItemVisual> heldItemVisuals = new HashMap<>();
    private String remoteHeldItem = "";
    private String attackingItemId = "";
    private boolean attacking;
    private float attackElapsed;

    private record HeldItemVisual(Texture texture, TextureRegion right, TextureRegion left) {}

    private static final Map<String, String> HELD_ITEM_ASSETS = Map.of(
        "axe", Assets.ITEM_AXE,
        "pickaxe", Assets.ITEM_PICKAXE,
        // The project's sword artwork is currently named dagger.png.
        "sword", Assets.ITEM_SWORD
    );

    public void setHeldItem(String itemId) {
        remoteHeldItem = itemId == null ? "" : itemId;
    }
    private Animation<TextureRegion> walkUp;
    private Animation<TextureRegion> walkLeft;
    private Animation<TextureRegion> walkDown;
    private Animation<TextureRegion> walkRight;
    private Animation<TextureRegion> walkUpLeft;
    private Animation<TextureRegion> walkUpRight;
    private Animation<TextureRegion> walkDownLeft;
    private Animation<TextureRegion> walkDownRight;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime = 0f;

    public Player(boolean localPlayer) {
        this.localPlayer = localPlayer;
        if (Gdx.app != null) {
            spriteSheet = new Texture(Assets.PLAYER_SPRITESHEET);
            for (Map.Entry<String, String> item : HELD_ITEM_ASSETS.entrySet()) {
                Texture texture = new Texture(item.getValue());
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                TextureRegion right = new TextureRegion(texture);
                TextureRegion left = new TextureRegion(texture);
                left.flip(true, false);
                heldItemVisuals.put(item.getKey(), new HeldItemVisual(texture, right, left));
            }

            TextureRegion[][] frames = TextureRegion.split(spriteSheet,
                spriteSheet.getWidth() / 6,
                spriteSheet.getHeight() / 16);

            walkUp = new Animation<>(0.1f, frames[12]);
            walkLeft = new Animation<>(0.1f, frames[14]);
            walkDown = new Animation<>(0.1f, frames[8]);
            walkRight = new Animation<>(0.1f, frames[10]);

            walkUpLeft = new Animation<>(0.1f, frames[13]);
            walkUpRight = new Animation<>(0.1f, frames[11]);
            walkDownLeft = new Animation<>(0.1f, frames[15]);
            walkDownRight = new Animation<>(0.1f, frames[9]);

            currentAnimation = walkDown;
        } else {
            // for testing
            walkUp = walkLeft = walkDown = walkRight = walkUpLeft = walkUpRight = walkDownLeft = walkDownRight = null;
            currentAnimation = null;
        }

    }


    public PlayerStats getStats() {
        return stats;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void update(List<Chunk> chunks) {
        update(chunks, true);
    }

    public void update(List<Chunk> chunks, boolean allowMovement) {
        updateAttackAnimation();
        if (stats.getHealth() == 0) {
            moving = false;
            stateTime = 0f;
            return;
        }
        if (!localPlayer) {
            stateTime = moving ? stateTime + Gdx.graphics.getDeltaTime() : 0f;
            return;
        }

        movement.set(0, 0);

        if (!allowMovement) {
            moving = false;
            stateTime = 0f;
            return;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            movement.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            movement.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            movement.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            movement.x += 1;
        }

        if (!movement.isZero()) {
            movement.nor();
            stateTime += Gdx.graphics.getDeltaTime();
            moving = true;

            // 1. Check diagonals FIRST (both X and Y are non-zero)
            if (movement.x > 0 && movement.y > 0) {
                currentAnimation = walkUpRight;
                direction = UP_RIGHT;
            } else if (movement.x < 0 && movement.y > 0) {
                currentAnimation = walkUpLeft;
                direction = UP_LEFT;
            } else if (movement.x > 0 && movement.y < 0) {
                currentAnimation = walkDownRight;
                direction = DOWN_RIGHT;
            } else if (movement.x < 0 && movement.y < 0) {
                currentAnimation = walkDownLeft;
                direction = DOWN_LEFT;
            }

            // 2. Check straight directions ONLY IF no diagonal is pressed
            else if (movement.y > 0) {
                currentAnimation = walkUp;
                direction = UP;
            } else if (movement.y < 0) {
                currentAnimation = walkDown;
                direction = DOWN;
            } else if (movement.x < 0) {
                currentAnimation = walkLeft;
                direction = LEFT;
            } else if (movement.x > 0) {
                currentAnimation = walkRight;
                direction = RIGHT;
            }
        } else {
            moving = false;
            stateTime = 0f;
        }

        movement.scl(speed * Gdx.graphics.getDeltaTime());
        if (movement.x != 0 && canMoveTo(x + movement.x, y, chunks)) {
            x += movement.x;
        }
        if (movement.y != 0 && canMoveTo(x, y + movement.y, chunks)) {
            y += movement.y;
        }
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        var selected = inventory.getSlot(inventory.getSelectedSlot());
        String heldItem = localPlayer ? (selected == null ? "" : selected.itemId()) : remoteHeldItem;
        if (attacking && !attackingItemId.isEmpty()) heldItem = attackingItemId;
        boolean showHeldItem = heldItemVisuals.containsKey(heldItem) && stats.getHealth() > 0;
        boolean facingAway = direction == UP || direction == UP_LEFT || direction == UP_RIGHT;
        if (showHeldItem && facingAway) renderHeldItem(batch, heldItem);
        batch.draw(currentFrame, x, y);
        if (showHeldItem && !facingAway) renderHeldItem(batch, heldItem);
    }

    private void renderHeldItem(SpriteBatch batch, String itemId) {
        HeldItemVisual visual = heldItemVisuals.get(itemId);
        if (visual == null) return;

        boolean facingLeft = direction == LEFT || direction == UP_LEFT || direction == DOWN_LEFT;
        float width = 16f;
        Texture texture = visual.texture();
        float height = width * texture.getHeight() / texture.getWidth();
        // Anchor the grip, rather than the image corner, to the hand in the 32x32 player frame.
        float gripX = width * (facingLeft ? 0.77f : 0.23f);
        float gripY = height * 0.25f;
        float handX = x + (facingLeft ? 9f : 22f);
        float handY = y + 10f;
        TextureRegion region = facingLeft ? visual.left() : visual.right();
        float textureForwardAngle = facingLeft ? 135f : 45f;
        float rotation = facingAngle(direction) - textureForwardAngle + currentSwingOffset();
        batch.draw(region,
            handX - gripX, handY - gripY, gripX, gripY,
            width, height, 1f, 1f, rotation);
    }

    private static float facingAngle(int direction) {
        return switch (direction) {
            case UP -> 90f;
            case LEFT -> 180f;
            case DOWN -> 270f;
            case UP_LEFT -> 135f;
            case UP_RIGHT -> 45f;
            case DOWN_LEFT -> 225f;
            case DOWN_RIGHT -> 315f;
            default -> 0f;
        };
    }

    private float currentSwingOffset() {
        if (!attacking) return 0f;
        float progress = MathUtils.clamp(attackElapsed / ATTACK_ANIMATION_DURATION, 0f, 1f);
        if (progress < 0.8f) {
            return MathUtils.lerp(SWING_START_OFFSET, SWING_END_OFFSET, progress / 0.8f);
        }
        return MathUtils.lerp(SWING_END_OFFSET, 0f, (progress - 0.8f) / 0.2f);
    }

    private void updateAttackAnimation() {
        if (!attacking) return;
        attackElapsed += Gdx.graphics.getDeltaTime();
        if (attackElapsed >= ATTACK_ANIMATION_DURATION) {
            attackElapsed = ATTACK_ANIMATION_DURATION;
            attacking = false;
        }
    }

    /** Starts a short swing after the server confirms this player's attack. */
    public void triggerAttack(String itemId) {
        attackingItemId = itemId == null ? "" : itemId;
        attackElapsed = 0f;
        attacking = true;
    }

    /** Applies authoritative position and animation state received for a remote player. */
    public void setRemoteState(float x, float y, int direction, boolean moving) {
        this.x = x;
        this.y = y;
        if (direction >= DOWN && direction <= DOWN_RIGHT && this.direction != direction) {
            this.direction = direction;
            switch (direction) {
                case DOWN: currentAnimation = walkDown; break;
                case UP: currentAnimation = walkUp; break;
                case LEFT: currentAnimation = walkLeft; break;
                case RIGHT: currentAnimation = walkRight; break;
                case UP_LEFT: currentAnimation = walkUpLeft; break;
                case UP_RIGHT: currentAnimation = walkUpRight; break;
                case DOWN_LEFT: currentAnimation = walkDownLeft; break;
                case DOWN_RIGHT: currentAnimation = walkDownRight; break;
                default: break;
            }
            stateTime = 0f;
        }
        if (this.moving != moving) stateTime = 0f;
        this.moving = moving;
    }

    //// Checks if a specific point in the world coordinates is valid (free of water, rocks, or trees).
    private boolean isWalkable(float worldX, float worldY, List<Chunk> chunks) {
        int tileSize = 32;
        int globalTileX = (int) Math.floor(worldX / tileSize);
        int globalTileY = (int) Math.floor(worldY / tileSize);

        int chunkX = Chunk.chunkCoordinate(globalTileX);
        int chunkY = Chunk.chunkCoordinate(globalTileY);

        for (Chunk chunk : chunks) {
            if (chunk.getChunkX() == chunkX && chunk.getChunkY() == chunkY) {
                int localX = Chunk.localCoordinate(globalTileX);
                int localY = Chunk.localCoordinate(globalTileY);
                return chunk.isWalkable(localX, localY);
            }
        }
        return false;
    }

    //// Checks if the player's entire Axis-Aligned Bounding Box (AABB) fits at the target coordinates.
    private boolean canMoveTo(float newX, float newY, List<Chunk> chunks) {
        float hitboxWidth = 12f;
        float hitboxHeight = 6f;
        float offsetX = 10f;
        float offsetY = 0f;

        return isWalkable(newX + offsetX, newY + offsetY, chunks) &&
            isWalkable(newX + offsetX + hitboxWidth, newY + offsetY, chunks) &&
            isWalkable(newX + offsetX, newY + offsetY + hitboxHeight, chunks) &&
            isWalkable(newX + offsetX + hitboxWidth, newY + offsetY + hitboxHeight, chunks);
    }

    public void dispose() {
        for (HeldItemVisual visual : heldItemVisuals.values()) visual.texture().dispose();
        heldItemVisuals.clear();
        if (spriteSheet != null) {
            spriteSheet.dispose();
        }
    }
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getDirection() { return direction; }
    public boolean isMoving() { return moving; }
}
