package com.projectoverseer.server.combat;

import com.projectoverseer.gameplay.*;
import com.projectoverseer.shared.Network.AttackRequest;
import com.projectoverseer.shared.combat.CombatConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackValidatorTest {
    @Test void usesServerInventoryAndNormalizesAim() {
        var validator = new AttackValidator(CombatConfig.defaults());
        var inventory = new Inventory();
        inventory.setSlot(2, new ItemStack("sword", 1));
        var hit = validator.accept(1, inventory, 16, 16, true, new AttackRequest(2, 1016, 16), 0);
        assertNotNull(hit);
        assertEquals(20f, hit.stats().damage());
        assertEquals(1f, hit.directionX());
        assertEquals(0f, hit.directionY());
        assertFalse(hit.hitbox().intersects(100, 16, 1, 1));
        assertEquals(2, inventory.getSelectedSlot());
    }

    @Test void switchingWeaponsDoesNotBypassPreviousCooldown() {
        var validator = new AttackValidator(CombatConfig.defaults());
        var inventory = new Inventory();
        inventory.setSlot(0, new ItemStack("axe", 1));
        assertNotNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(0, 1, 0), 0));
        assertNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(1, 1, 0), 600_000_000));
        assertNotNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(1, 1, 0), 900_000_000));
    }

    @Test void rejectsInvalidRequestsWithoutConsumingCooldown() {
        var validator = new AttackValidator(CombatConfig.defaults());
        var inventory = new Inventory();
        for (int slot : new int[]{-1, 9}) {
            assertNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(slot, 1, 0), 0));
        }
        assertNull(validator.accept(1, inventory, 0, 0, false, new AttackRequest(0, 1, 0), 0));
        assertNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(0, Float.NaN, 0), 0));
        assertNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(0, 0, 0), 0));
        assertNull(validator.accept(1, null, 0, 0, true, new AttackRequest(0, 1, 0), 0));
        assertNotNull(validator.accept(1, inventory, 0, 0, true, new AttackRequest(0, 1, 0), 0));
    }

    @Test void cooldownIsPerPlayerAndRemovedOnDisconnect() {
        var validator = new AttackValidator(CombatConfig.defaults());
        var inventory = new Inventory();
        var request = new AttackRequest(0, 1, 0);
        assertNotNull(validator.accept(1, inventory, 0, 0, true, request, 0));
        assertNotNull(validator.accept(2, inventory, 0, 0, true, request, 0));
        assertNull(validator.accept(1, inventory, 0, 0, true, request, 1));
        validator.removePlayer(1);
        assertNotNull(validator.accept(1, inventory, 0, 0, true, request, 1));
    }
}
