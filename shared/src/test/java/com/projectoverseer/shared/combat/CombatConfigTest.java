package com.projectoverseer.shared.combat;

import com.projectoverseer.gameplay.ItemStack;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatConfigTest {
    @Test public void selectsWeaponOrFallback() {
        CombatConfig config = CombatConfig.defaults();
        assertEquals(20, config.forItem(new ItemStack("sword", 1)).damage(), 0);
        assertEquals(15, config.forItem(new ItemStack("axe", 1)).damage(), 0);
        assertEquals(10, config.forItem(new ItemStack("pickaxe", 1)).damage(), 0);
        assertEquals(config.forItem(null), config.forItem(new ItemStack("wood", 3)));
        assertEquals(5, config.forItem(null).damage(), 0);
    }

    @Test public void customProfilesAreIndependentOfSourceMap() {
        WeaponStats basic = new WeaponStats(1, 20, 10, 1, 0);
        WeaponStats sword = new WeaponStats(7, 30, 15, 2, 3);
        var profiles = new HashMap<String, WeaponStats>();
        profiles.put("sword", sword);
        CombatConfig config = new CombatConfig(basic, profiles);
        profiles.clear();
        assertEquals(sword, config.forItem(new ItemStack("sword", 1)));
        assertEquals(basic, config.forItem(null));
    }

    @Test public void rejectsInvalidStats() {
        for (float bad : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new WeaponStats(bad, 32, 24, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> new WeaponStats(5, bad, 24, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> new WeaponStats(5, 32, bad, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> new WeaponStats(5, 32, 24, bad, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new WeaponStats(5, 32, 24, 1, -1));
    }
}
