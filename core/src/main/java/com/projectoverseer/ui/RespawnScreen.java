package com.projectoverseer.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.projectoverseer.Assets;
import com.projectoverseer.config.ClientConfig;

public class RespawnScreen implements Disposable {
    private final Stage stage;
    private final BitmapFont font;
    private final BitmapFont titleFont;
    private final Texture buttonTexture;
    private final Texture buttonHoverTexture;
    private final Texture buttonDownTexture;

    private final Sound buttonClickSound = Gdx.audio.newSound(Gdx.files.internal(Assets.SFX_MENU_CLICK));
    private final Sound buttonHoverSound = Gdx.audio.newSound(Gdx.files.internal(Assets.SFX_MENU_HOVER));

    private final float sfxVolume = ClientConfig.getFloat("audio.sfx.volume", 1f);

    public RespawnScreen(Runnable respawnAction) {
        stage = new Stage(new ScreenViewport());

        font = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);

        buttonTexture = createSolidTexture(new Color(0.65f, 0.12f, 0.12f, 1f));
        buttonHoverTexture = createSolidTexture(new Color(0.85f, 0.18f, 0.18f, 1f));
        buttonDownTexture = createSolidTexture(new Color(0.4f, 0.08f, 0.08f, 1f));

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(new TextureRegion(buttonTexture));
        buttonStyle.over = new TextureRegionDrawable(new TextureRegion(buttonHoverTexture));
        buttonStyle.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label.LabelStyle messageStyle = new Label.LabelStyle(font, Color.LIGHT_GRAY);

        TextButton respawnButton = new TextButton("Respawn", buttonStyle);

        respawnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                buttonClickSound.play(sfxVolume);
                respawnAction.run();
            }
        });

        respawnButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                buttonHoverSound.play(sfxVolume);
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(new Label("YOU DIED", titleStyle)).padBottom(20).row();
        table.add(new Label("You have been defeated.", messageStyle)).padBottom(35).row();
        table.add(respawnButton).width(220).height(55);

        stage.addActor(table);
    }

    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.02f, 0.02f, 1f);
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();
        titleFont.dispose();
        buttonTexture.dispose();
        buttonHoverTexture.dispose();
        buttonDownTexture.dispose();
        buttonClickSound.dispose();
        buttonHoverSound.dispose();
    }
}
