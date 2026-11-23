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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.projectoverseer.Assets;
import com.projectoverseer.config.ClientConfig;
import com.projectoverseer.shared.player.PlayerName;

/**
 * The Scene2D user interface displayed before the player connects to a game.
 */
public class MainMenu implements Disposable {
    private final Stage stage;
    private final Label statusLabel;
    private final BitmapFont menuFont;
    private final BitmapFont titleFont;
    private final Texture buttonUpTexture;
    private final Texture buttonOverTexture;
    private final Texture buttonDownTexture;
    private final Texture cursorTexture;
    private final TextField nameField;

    private final Sound backgroundMusic = Gdx.audio.newSound(Gdx.files.internal(Assets.MUSIC_MENU));
    private final Sound buttonClickSound = Gdx.audio.newSound(Gdx.files.internal(Assets.SFX_MENU_CLICK));
    private final Sound buttonHoverSound = Gdx.audio.newSound(Gdx.files.internal(Assets.SFX_MENU_HOVER));

    private final float musicVolume = ClientConfig.getFloat("audio.music.volume", 0.5f);
    private final float sfxVolume = ClientConfig.getFloat("audio.sfx.volume", 1.0f);

    public MainMenu(Runnable playAction) {
        stage = new Stage(new ScreenViewport());

        menuFont = new BitmapFont();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2f);

        buttonUpTexture = createSolidTexture(new Color(0.12f, 0.36f, 0.68f, 1f));
        buttonOverTexture = createSolidTexture(new Color(0.18f, 0.50f, 0.88f, 1f));
        buttonDownTexture = createSolidTexture(new Color(0.07f, 0.22f, 0.45f, 1f));
        cursorTexture = createSolidTexture(Color.WHITE);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(new TextureRegion(buttonUpTexture));
        buttonStyle.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        buttonStyle.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));
        buttonStyle.disabled = buttonStyle.down;
        buttonStyle.font = menuFont;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.disabledFontColor = Color.GRAY;

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label.LabelStyle statusStyle = new Label.LabelStyle(menuFont, new Color(0.75f, 0.85f, 1f, 1f));

        TextField.TextFieldStyle nameStyle = new TextField.TextFieldStyle();
        nameStyle.font = menuFont;
        nameStyle.fontColor = Color.WHITE;
        nameStyle.messageFontColor = Color.LIGHT_GRAY;
        nameStyle.background = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));
        nameStyle.cursor = new TextureRegionDrawable(new TextureRegion(cursorTexture));

        nameField = new TextField("", nameStyle);
        nameField.setMessageText("Enter your name");
        nameField.setMaxLength(PlayerName.MAX_LENGTH);
        nameField.setFocusTraversal(false);
        nameField.setTextFieldFilter(
            (field, character) -> PlayerName.isAllowedCharacter(character)
        );

        TextButton playButton = new TextButton("Play", buttonStyle);
        TextButton exitButton = new TextButton("Exit", buttonStyle);
        statusLabel = new Label("Choose Play to connect to the server.", statusStyle);

        playButton.setDisabled(true);

        nameField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playButton.setDisabled(
                    !PlayerName.isValid(nameField.getText())
                );
            }
        });

        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!PlayerName.isValid(nameField.getText())) {
                    return;
                }

                buttonClickSound.play(sfxVolume);
                playAction.run();
                backgroundMusic.stop();
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                buttonClickSound.play(sfxVolume);
                backgroundMusic.stop();
                Gdx.app.exit();
            }
        });

        playButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                buttonHoverSound.play(sfxVolume);
            }
        });

        exitButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                buttonHoverSound.play(sfxVolume);
            }
        });

        Table menu = new Table();
        menu.setFillParent(true);
        menu.center();
        menu.add(new Label("Project Overseer", titleStyle)).padBottom(30).row();
        menu.add(new Label("Player name", statusStyle)).padBottom(6).row();
        menu.add(nameField).width(220).height(42).padBottom(16).row();
        menu.add(playButton).width(220).height(55).padBottom(12).row();
        menu.add(exitButton).width(220).height(55).padBottom(25).row();
        menu.add(statusLabel);
        stage.addActor(menu);

        backgroundMusic.loop(musicVolume);
    }

    public String getPlayerName() {
        return PlayerName.normalize(nameField.getText());
    }

    public void show(String status) {
        statusLabel.setText(status);
        Gdx.input.setInputProcessor(stage);
        stage.setKeyboardFocus(nameField);
    }

    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.08f, 0.15f, 1f);
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

    @Override
    public void dispose() {
        stage.dispose();
        menuFont.dispose();
        titleFont.dispose();
        buttonUpTexture.dispose();
        buttonOverTexture.dispose();
        buttonDownTexture.dispose();
        cursorTexture.dispose();
        backgroundMusic.dispose();
    }
}
