package com.projectoverseer.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class DayNightRenderer {
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private float currentAlpha = 0f;
    private float transitionSpeed = 0.2f;

    public DayNightRenderer() {
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
    }

    public void render(OrthographicCamera camera, SpriteBatch batch, int currentDay, boolean isNight) {
        float delta = Gdx.graphics.getDeltaTime();

        if (isNight) {
            currentAlpha = Math.min(0.6f, currentAlpha + delta * transitionSpeed);
        } else {
            currentAlpha = Math.max(0f, currentAlpha - delta * transitionSpeed);
        }

        if (currentAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, currentAlpha);

            shapeRenderer.rect(
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
            );

            shapeRenderer.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        batch.begin();
        font.draw(batch, "Day: " + currentDay,
            camera.position.x - camera.viewportWidth / 2 + 10,
            camera.position.y + camera.viewportHeight / 2 - 10);
        batch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }

    public void renderVictoryScreen(OrthographicCamera camera, SpriteBatch batch) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        //todo: actual number of days
        font.draw(batch, "VICTORY! YOU SURVIVED 100 DAYS!",
            camera.position.x - 180,
            camera.position.y);
        batch.end();
    }
}
