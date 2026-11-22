package com.projectoverseer.shared.combat;

import com.projectoverseer.gameplay.ItemStack;
import java.util.Map;
import java.util.Objects;

/** Immutable weapon profiles; the server selects stats from its own inventory state. */
public final class CombatConfig {
    private final WeaponStats fallback;
    private final Map<String, WeaponStats> weapons;

    public CombatConfig(WeaponStats fallback, Map<String, WeaponStats> weapons) {
        this.fallback = Objects.requireNonNull(fallback);
        this.weapons = Map.copyOf(weapons);
    }

    public static CombatConfig defaults() {
        return new CombatConfig(new WeaponStats(5, 32, 24, 0.5f, 8), Map.of(
            "sword", new WeaponStats(20, 48, 40, 0.5f, 20),
            "axe", new WeaponStats(15, 40, 32, 0.8f, 24),
            "pickaxe", new WeaponStats(10, 40, 24, 0.7f, 16)));
    }

    public WeaponStats forItem(ItemStack selectedItem) {
        return selectedItem == null ? fallback : weapons.getOrDefault(selectedItem.itemId(), fallback);
    }
}
