package com.projectoverseer;

/**
 * Centralized asset path constants for textures and audio files in the client module.
 */
public final class Assets {
    private Assets() {}

    // --- Entities ---
    public static final String PLAYER_SPRITESHEET = "player.png";

    // --- Items ---
    public static final String ITEM_AXE = "ui/items/axe.png";
    public static final String ITEM_PICKAXE = "ui/items/pickaxe.png";
    public static final String ITEM_SWORD = "ui/items/dagger.png";
    public static final String ITEM_WOOD = "ui/items/wood.png";
    public static final String ITEM_STONE = "ui/items/stone.png";

    // --- HUD ---
    public static final String HUD_HEART_EMPTY = "ui/hud/health/container.png";
    public static final String HUD_HEART_HALF = "ui/hud/health/half.png";
    public static final String HUD_HEART_FULL = "ui/hud/health/full.png";
    public static final String HUD_FOOD_EMPTY = "ui/hud/hunger/food_empty.png";
    public static final String HUD_FOOD_HALF = "ui/hud/hunger/food_half.png";
    public static final String HUD_FOOD_FULL = "ui/hud/hunger/food_full.png";

    // --- World ---
    public static final String WORLD_TREE = "world/tree.png";
    public static final String WORLD_ROCK_1 = "world/rock-1.png";
    public static final String WORLD_ROCK_2 = "world/rock-2.png";
    public static final String WORLD_ROCK_3 = "world/rock-3.png";

    // --- Audio ---
    public static final String MUSIC_MENU = "sounds/background/menu_background.mp3";
    public static final String SFX_MENU_CLICK = "sounds/sfx/menu_click.mp3";
    public static final String SFX_MENU_HOVER = "sounds/sfx/menu_button_hover.mp3";
}
