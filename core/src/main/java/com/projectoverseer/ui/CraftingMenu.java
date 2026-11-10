
package com.projectoverseer.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Disposable;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.shared.crafting.CraftingRecipe;

import java.util.function.Consumer;

public final class CraftingMenu implements Disposable {

    private final Inventory inventory;
    private final Consumer<String> onCraft;
    private final Table root;
    private final BitmapFont font;

    private final CraftingRecipe[] recipes =
        CraftingRecipe.values();

    private final TextButton[] craftButtons =
        new TextButton[recipes.length];

    private final Texture[] itemTextures =
        new Texture[recipes.length];

    private final Texture[] backgroundTextures =
        new Texture[6];

    public CraftingMenu(
        Stage stage,
        Inventory inventory,
        Consumer<String> onCraft
    ) {
        this.inventory = inventory;
        this.onCraft = onCraft;

        font = new BitmapFont();
        font.getData().setScale(0.85f);

        Label.LabelStyle titleStyle =
            new Label.LabelStyle(font, Color.WHITE);

        Label.LabelStyle costStyle =
            new Label.LabelStyle(
                font,
                Color.valueOf("BFC9D8")
            );

        Drawable overlay = createBackground(
            0, "000000B8"
        );

        Drawable panelBackground = createBackground(
            1, "1B2635FF"
        );

        Drawable rowBackground = createBackground(
            2, "2A394CFF"
        );

        TextButton.TextButtonStyle buttonStyle =
            new TextButton.TextButtonStyle();

        buttonStyle.up = createBackground(
            3, "45617EFF"
        );

        buttonStyle.down = createBackground(
            4, "30465FFF"
        );

        buttonStyle.disabled = createBackground(
            5, "343B45FF"
        );

        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.disabledFontColor = Color.GRAY;

        root = new Table();
        root.setFillParent(true);
        root.setBackground(overlay);
        root.setVisible(false);

        Table panel = new Table();
        panel.setBackground(panelBackground);
        panel.pad(12);

        Label title = new Label(
            "CRAFTING", titleStyle
        );

        panel.add(title)
            .padBottom(10)
            .row();

        for (int i = 0; i < recipes.length; i++) {

            CraftingRecipe recipe = recipes[i];

            String texturePath = switch (recipe) {
                case AXE -> "ui/items/axe.png";
                case PICKAXE -> "ui/items/pickaxe.png";
                case SWORD -> "ui/items/dagger.png";
            };

            Texture texture = new Texture(
                Gdx.files.internal(texturePath)
            );

            texture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            );

            itemTextures[i] = texture;

            Image icon = new Image(
                new TextureRegionDrawable(
                    new TextureRegion(texture)
                )
            );

            Label name = new Label(
                recipe.getItemId().toUpperCase(),
                titleStyle
            );

            Label cost = new Label(
                recipe.getWoodCost() + " Wood + "
                    + recipe.getStoneCost() + " Stone",
                costStyle
            );

            Table information = new Table();
            information.add(name).left().row();
            information.add(cost).left();

            TextButton craftButton = new TextButton(
                "Craft",
                buttonStyle
            );

            craftButtons[i] = craftButton;

            craftButton.addListener(new ChangeListener() {
                @Override
                public void changed(
                    ChangeEvent event,
                    Actor actor
                ) {
                    if (!craftButton.isDisabled()) {
                        onCraft.accept(recipe.getItemId());
                    }
                }
            });

            Table row = new Table();
            row.setBackground(rowBackground);

            row.add(icon)
                .size(30)
                .padLeft(8)
                .padRight(10);

            row.add(information)
                .expandX()
                .left();

            row.add(craftButton)
                .size(65, 28)
                .padRight(8);

            panel.add(row)
                .width(295)
                .height(56)
                .padBottom(5)
                .row();
        }

        root.add(panel);
        stage.addActor(root);

        refresh();
    }

    public void toggle() {
        if (root.isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        refresh();
        root.setVisible(true);
        root.toFront();
    }

    public void hide() {
        root.setVisible(false);
    }

    public boolean isOpen() {
        return root.isVisible();
    }

    public void refresh() {

        int wood = inventory.countItem("wood");
        int stone = inventory.countItem("stone");

        for (int i = 0; i < recipes.length; i++) {

            boolean available = recipes[i].canCraft(
                wood,
                stone
            );

            craftButtons[i].setDisabled(!available);
        }
    }

    private Drawable createBackground(
        int index,
        String color
    ) {
        Pixmap pixmap = new Pixmap(
            1, 1, Pixmap.Format.RGBA8888
        );

        pixmap.setColor(Color.valueOf(color));
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        backgroundTextures[index] = texture;

        pixmap.dispose();

        return new TextureRegionDrawable(
            new TextureRegion(texture)
        );
    }

    @Override
    public void dispose() {
        root.remove();

        for (Texture texture : itemTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }

        for (Texture texture : backgroundTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }

        font.dispose();
    }
}
