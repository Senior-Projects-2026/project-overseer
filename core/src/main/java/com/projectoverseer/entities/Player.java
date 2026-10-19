package com.projectoverseer.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private float x = 400;
    private float y = 300;
    private float speed = 100;
    private Vector2 movement = new Vector2();

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

    public Player() {
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

    public void update() {
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


            // 1. Check diagonals FIRST (both X and Y are non-zero)
            if (movement.x > 0 && movement.y > 0) {
                currentAnimation = walkUpRight;
            } else if (movement.x < 0 && movement.y > 0) {
                currentAnimation = walkUpLeft;
            } else if (movement.x > 0 && movement.y < 0) {
                currentAnimation = walkDownRight;
            } else if (movement.x < 0 && movement.y < 0) {
                currentAnimation = walkDownLeft;
            }

            // 2. Check straight directions ONLY IF no diagonal is pressed
            else if (movement.y > 0) {
                currentAnimation = walkUp;
            } else if (movement.y < 0) {
                currentAnimation = walkDown;
            } else if (movement.x < 0) {
                currentAnimation = walkLeft;
            } else if (movement.x > 0) {
                currentAnimation = walkRight;
            }
        } else {
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

    public void renderAt(SpriteBatch batch, float renderX, float renderY) {
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        batch.draw(currentFrame, renderX, renderY);
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
}
