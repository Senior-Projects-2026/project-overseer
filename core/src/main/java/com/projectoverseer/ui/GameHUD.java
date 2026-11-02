package com.projectoverseer.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Blending;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.gameplay.PlayerStats;

public final class GameHUD implements Disposable {


    private static final int ICON_COUNT = 10;
    private static final int ICON_SIZE = 13;

    private static final int SLOT_SIZE = 30;
    private static final int SLOT_GAP = 2;

    private static final int EMPTY = 0;
    private static final int HALF = 1;
    private static final int FULL = 2;

    private static final int[] NUMBER_KEYS = {
        Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3,
        Input.Keys.NUM_4, Input.Keys.NUM_5, Input.Keys.NUM_6,
        Input.Keys.NUM_7, Input.Keys.NUM_8, Input.Keys.NUM_9
    };


    private static final String HUD_ASSET_PATH = "ui/hud/";

    private final Texture[] iconTextures = new Texture[6];

    private final Drawable[] heartIcons = new Drawable[3];
    private final Drawable[] foodIcons = new Drawable[3];


    private final PlayerStats stats;
    private final Inventory inventory;
    private final Stage stage;
    private final BitmapFont font;

    private final Texture normalSlotTexture;
    private final Texture selectedSlotTexture;
    private final Drawable normalSlot;
    private final Drawable selectedSlot;

    private final Image[] hearts = new Image[ICON_COUNT];
    private final Image[] food = new Image[ICON_COUNT];

    private final Table[] slotTables =
        new Table[Inventory.SLOT_COUNT];

    private final Label[] itemLabels =
        new Label[Inventory.SLOT_COUNT];

    private final Label[] quantityLabels =
        new Label[Inventory.SLOT_COUNT];

    private int shownHealth = -1;
    private int shownHunger = -1;
    private int shownSelected = -1;
    private int shownRevision = -1;

    public GameHUD(PlayerStats stats, Inventory inventory) {
        this.stats = stats;
        this.inventory = inventory;

        stage = new Stage(new FitViewport(640, 360));

        font = new BitmapFont();
        font.getData().setScale(0.65f);

        heartIcons[EMPTY] = loadIcon(
            HUD_ASSET_PATH + "health/container.png", 0
        );

        heartIcons[HALF] = loadIcon(
            HUD_ASSET_PATH + "health/half.png", 1
        );

        heartIcons[FULL] = loadIcon(
            HUD_ASSET_PATH + "health/full.png", 2
        );

        foodIcons[EMPTY] = loadIcon(
            HUD_ASSET_PATH + "hunger/food_empty.png", 3
        );

        foodIcons[HALF] = loadIcon(
            HUD_ASSET_PATH + "hunger/food_half.png", 4
        );

        foodIcons[FULL] = loadIcon(
            HUD_ASSET_PATH + "hunger/food_full.png", 5
        );


        normalSlotTexture = createSlotTexture(false);
        selectedSlotTexture = createSlotTexture(true);

        normalSlot = new TextureRegionDrawable(
            new TextureRegion(normalSlotTexture)
        );
        selectedSlot = new TextureRegionDrawable(
            new TextureRegion(selectedSlotTexture)
        );

        Label.LabelStyle textStyle =
            new Label.LabelStyle(font, Color.WHITE);

        Table healthRow = new Table();
        Table hungerRow = new Table();

        for (int i = 0; i < ICON_COUNT; i++) {
            hearts[i] = new Image(heartIcons[FULL]);
            food[i] = new Image(foodIcons[FULL]);

            healthRow.add(hearts[i])
                .size(ICON_SIZE).pad(0);

            hungerRow.add(food[i])
                .size(ICON_SIZE).pad(0);
        }

        Table statsRow = new Table();
        statsRow.add(healthRow).left();
        statsRow.add().expandX();
        statsRow.add(hungerRow).right();

        Table hotbarRow = new Table();

        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            final int slotIndex = i;

            Table slot = new Table();
            slot.setBackground(normalSlot);
            slot.top();
            slotTables[i] = slot;

            Label number = new Label(
                Integer.toString(i + 1), textStyle
            );
            itemLabels[i] = new Label("", textStyle);
            quantityLabels[i] = new Label("", textStyle);

            slot.add(number)
                .left().padLeft(4).expandX().row();

            slot.add(itemLabels[i])
                .center().expand().row();

            slot.add(quantityLabels[i])
                .right().padRight(3).padBottom(2);

            slot.addListener(new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event, float x, float y
                ) {
                    inventory.selectSlot(slotIndex);
                }
            });

            hotbarRow.add(slot).size(SLOT_SIZE);

            if (i < Inventory.SLOT_COUNT - 1) {
                hotbarRow.add().width(SLOT_GAP);
            }
        }

        int hotbarWidth =
            Inventory.SLOT_COUNT * SLOT_SIZE
                + (Inventory.SLOT_COUNT - 1) * SLOT_GAP;

        Table content = new Table();
        content.add(statsRow)
            .width(hotbarWidth).padBottom(6).row();
        content.add(hotbarRow);

        Table anchor = new Table();
        anchor.setFillParent(true);
        anchor.bottom();
        anchor.add(content).padBottom(30);

        stage.addActor(anchor);

        refresh();
    }


    private Drawable loadIcon(String path, int index) {

        Pixmap source = new Pixmap(
            Gdx.files.internal(path)
        );

        Pixmap outlined = new Pixmap(
            source.getWidth() + 2,
            source.getHeight() + 2,
            Pixmap.Format.RGBA8888
        );

        outlined.setBlending(Pixmap.Blending.None);
        outlined.setColor(0, 0, 0, 0);
        outlined.fill();

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {

                int pixel = source.getPixel(x, y);
                int alpha = pixel & 0xFF;

                if (alpha == 0) {
                    continue;
                }

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        outlined.drawPixel(
                            x + 1 + dx,
                            y + 1 + dy,
                            0x000000FF
                        );
                    }
                }
            }
        }

        outlined.setBlending(Pixmap.Blending.SourceOver);
        outlined.drawPixmap(source, 1, 1);

        Texture texture = new Texture(outlined);

        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        iconTextures[index] = texture;

        source.dispose();
        outlined.dispose();

        return new TextureRegionDrawable(
            new TextureRegion(texture)
        );
    }


    public void render(float delta) {
        for (int i = 0; i < NUMBER_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(NUMBER_KEYS[i])) {
                inventory.selectSlot(i);
            }
        }

        stage.act(delta);
        refresh();
        stage.draw();
    }

    private void refresh() {
        if (shownHealth != stats.getHealth()) {
            shownHealth = stats.getHealth();

            for (int i = 0; i < ICON_COUNT; i++) {
                hearts[i].setDrawable(
                    heartIcons[fillLevel(shownHealth - i * 2)]
                );
            }
        }

        if (shownHunger != stats.getHunger()) {
            shownHunger = stats.getHunger();

            for (int i = 0; i < ICON_COUNT; i++) {
                food[i].setDrawable(
                    foodIcons[fillLevel(
                        shownHunger - (ICON_COUNT - 1 - i) * 2
                    )]
                );
            }
        }

        if (shownSelected != inventory.getSelectedSlot()) {
            shownSelected = inventory.getSelectedSlot();

            for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
                slotTables[i].setBackground(
                    i == shownSelected
                        ? selectedSlot : normalSlot
                );
            }
        }

        if (shownRevision != inventory.getRevision()) {
            shownRevision = inventory.getRevision();

            for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
                ItemStack item = inventory.getSlot(i);

                String name = item == null
                    ? "" : item.itemId();

                itemLabels[i].setText(
                    name.length() > 6
                        ? name.substring(0, 6) : name
                );

                quantityLabels[i].setText(
                    item != null && item.quantity() > 1
                        ? Integer.toString(item.quantity()) : ""
                );
            }
        }
    }

    private static int fillLevel(int points) {
        if (points >= 2) return FULL;
        if (points == 1) return HALF;
        return EMPTY;
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private static Texture createSlotTexture(boolean selected) {
        Pixmap map = new Pixmap(
            SLOT_SIZE, SLOT_SIZE, Pixmap.Format.RGBA8888
        );

        map.setColor(Color.valueOf(
            selected ? "E7BA55" : "647389"
        ));
        map.fill();

        map.setColor(Color.valueOf("1B2635"));
        map.fillRectangle(
            2, 2, SLOT_SIZE - 4, SLOT_SIZE - 4
        );

        map.setColor(Color.valueOf("2B3A4B"));
        map.fillRectangle(
            4, 4, SLOT_SIZE - 8, SLOT_SIZE - 8
        );

        Texture texture = new Texture(map);
        texture.setFilter(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest
        );

        map.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();

        for (Texture texture : iconTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }

        normalSlotTexture.dispose();
        selectedSlotTexture.dispose();
    }

}
