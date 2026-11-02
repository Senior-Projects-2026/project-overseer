package com.projectoverseer.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import com.projectoverseer.gameplay.PlayerStats;
import com.projectoverseer.gameplay.Inventory;


public class Player {
    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int UP_LEFT = 4;
    public static final int UP_RIGHT = 5;
    public static final int DOWN_LEFT = 6;
    public static final int DOWN_RIGHT = 7;


    private final PlayerStats stats = new PlayerStats();
    private final Inventory inventory = new Inventory();

    private float x = 400;
    private float y = 300;
    private float speed = 100;
    private final Vector2 movement = new Vector2();
    private final boolean localPlayer;
    private int direction = DOWN;
    private boolean moving;

    private Texture spriteSheet;
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
        spriteSheet = new Texture("player.png");

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
    }


    public PlayerStats getStats() {
        return stats;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void update() {
        if (!localPlayer) {
            stateTime = moving ? stateTime + Gdx.graphics.getDeltaTime() : 0f;
            return;
        }

        movement.set(0, 0);

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

        x += movement.x;
        y += movement.y;
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        batch.draw(currentFrame, x, y);
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

    public void dispose() {
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
