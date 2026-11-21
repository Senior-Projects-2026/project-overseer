package com.projectoverseer.shared.world;

import java.util.Objects;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;

public final class ResourceGatheringRules {
    public static final int TILE_SIZE = 32;
    public static final float PUNCH_RANGE = 64f;
    public static final int TREE_DAMAGE_REQUIRED = 6;
    public static final int ROCK_DAMAGE_REQUIRED = 7;
    public static final int BERRY_DAMAGE_REQUIRED = 1;

    private ResourceGatheringRules() {
    }

    public static int damageRequired(ResourceType type) {
        return switch (Objects.requireNonNull(type, "type")) {
            case TREE -> TREE_DAMAGE_REQUIRED;
            case ROCK -> ROCK_DAMAGE_REQUIRED;
            case BERRY_BUSH -> BERRY_DAMAGE_REQUIRED;
        };
    }

    public static int hitDamage(ResourceType type, Inventory inventory, int selectedSlot) {
        Objects.requireNonNull(type, "type");

        if (inventory == null || selectedSlot < 0 || selectedSlot >= Inventory.SLOT_COUNT) {
            return 0;
        }

        ItemStack item = inventory.getSlot(selectedSlot);
        String itemId = item == null ? null : item.itemId();

        return switch (type) {
            case TREE -> "axe".equals(itemId) ? 3 : 1;
            case ROCK -> "pickaxe".equals(itemId) ? 7 : 1;
            case BERRY_BUSH -> 1;
        };
    }
}
