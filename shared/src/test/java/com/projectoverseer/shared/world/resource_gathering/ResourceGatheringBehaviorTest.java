package com.projectoverseer.shared.world.resource_gathering;

import com.projectoverseer.gameplay.Inventory;
import com.projectoverseer.gameplay.ItemStack;
import com.projectoverseer.shared.world.ResourceGatheringRules;
import com.projectoverseer.shared.world.ResourceType;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceGatheringBehaviorTest {

    private static Inventory withSelectedItem(String itemId) {
        Inventory inventory = new Inventory();
        if (itemId != null) {
            inventory.setSlot(2, new ItemStack(itemId, 1));
        }
        inventory.selectSlot(2);
        return inventory;
    }

    private static void assertBreaksOnHit(ResourceType type, Inventory inventory, int expectedHits) {
        int damage = 0;
        int required = ResourceGatheringRules.damageRequired(type);

        for (int hit = 1; hit <= expectedHits; hit++) {
            int inflicted = ResourceGatheringRules.hitDamage(type, inventory, inventory.getSelectedSlot());
            assertTrue("Every valid hit must deal damage", inflicted > 0);
            damage += inflicted;

            if (hit < expectedHits) {
                assertTrue("Destroyed too early at hit " + hit, damage < required);
            } else {
                assertTrue("Still standing at final hit " + hit, damage >= required);
            }
        }
    }

    @Test public void treeRequiresExactlySixDamage() {
        assertEquals(6, ResourceGatheringRules.damageRequired(ResourceType.TREE));
    }

    @Test public void rockRequiresExactlySevenDamage() {
        assertEquals(7, ResourceGatheringRules.damageRequired(ResourceType.ROCK));
    }

    @Test public void emptyHandDestroysTreeInExactlySixHits() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem(null), 6);
    }

    @Test public void emptyHandDestroysRockInExactlySevenHits() {
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem(null), 7);
    }

    @Test public void axeDestroysTreeInExactlyTwoHits() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem("axe"), 2);
    }

    @Test public void pickaxeDestroysRockInExactlyOneHit() {
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem("pickaxe"), 1);
    }

    @Test public void axeDealsThreeTreeDamage() {
        assertEquals(3, ResourceGatheringRules.hitDamage(ResourceType.TREE, withSelectedItem("axe"), 2));
    }

    @Test public void pickaxeDealsSevenRockDamage() {
        assertEquals(7, ResourceGatheringRules.hitDamage(ResourceType.ROCK, withSelectedItem("pickaxe"), 2));
    }

    @Test public void axeDoesNotSpeedUpRock() {
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem("axe"), 7);
    }

    @Test public void pickaxeDoesNotSpeedUpTree() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem("pickaxe"), 6);
    }

    @Test public void swordDoesNotSpeedUpTree() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem("sword"), 6);
    }

    @Test public void swordDoesNotSpeedUpRock() {
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem("sword"), 7);
    }

    @Test public void woodDoesNotSpeedUpGathering() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem("wood"), 6);
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem("wood"), 7);
    }

    @Test public void stoneDoesNotSpeedUpGathering() {
        assertBreaksOnHit(ResourceType.TREE, withSelectedItem("stone"), 6);
        assertBreaksOnHit(ResourceType.ROCK, withSelectedItem("stone"), 7);
    }

    @Test public void axeInAnotherSlotDoesNotSpeedUpTree() {
        Inventory inventory = withSelectedItem(null);
        inventory.setSlot(3, new ItemStack("axe", 1));
        assertBreaksOnHit(ResourceType.TREE, inventory, 6);
    }

    @Test public void pickaxeInAnotherSlotDoesNotSpeedUpRock() {
        Inventory inventory = withSelectedItem(null);
        inventory.setSlot(3, new ItemStack("pickaxe", 1));
        assertBreaksOnHit(ResourceType.ROCK, inventory, 7);
    }

    @Test public void changingSelectedSlotActivatesAxeBonus() {
        Inventory inventory = withSelectedItem(null);
        inventory.setSlot(3, new ItemStack("axe", 1));
        assertEquals(1, ResourceGatheringRules.hitDamage(ResourceType.TREE, inventory, inventory.getSelectedSlot()));
        inventory.selectSlot(3);
        assertEquals(3, ResourceGatheringRules.hitDamage(ResourceType.TREE, inventory, inventory.getSelectedSlot()));
    }

    @Test public void changingSelectedSlotActivatesPickaxeBonus() {
        Inventory inventory = withSelectedItem(null);
        inventory.setSlot(3, new ItemStack("pickaxe", 1));
        assertEquals(1, ResourceGatheringRules.hitDamage(ResourceType.ROCK, inventory, inventory.getSelectedSlot()));
        inventory.selectSlot(3);
        assertEquals(7, ResourceGatheringRules.hitDamage(ResourceType.ROCK, inventory, inventory.getSelectedSlot()));
    }

    @Test public void removingAxeRemovesItsBonus() {
        Inventory inventory = withSelectedItem("axe");
        assertEquals(3, ResourceGatheringRules.hitDamage(ResourceType.TREE, inventory, 2));
        inventory.clearSlot(2);
        assertEquals(1, ResourceGatheringRules.hitDamage(ResourceType.TREE, inventory, 2));
    }

    @Test public void removingPickaxeRemovesItsBonus() {
        Inventory inventory = withSelectedItem("pickaxe");
        assertEquals(7, ResourceGatheringRules.hitDamage(ResourceType.ROCK, inventory, 2));
        inventory.clearSlot(2);
        assertEquals(1, ResourceGatheringRules.hitDamage(ResourceType.ROCK, inventory, 2));
    }

    @Test public void invalidNegativeSlotDealsNoDamage() {
        assertEquals(0, ResourceGatheringRules.hitDamage(ResourceType.TREE, withSelectedItem("axe"), -1));
    }

    @Test public void slotPastInventoryEndDealsNoDamage() {
        assertEquals(0, ResourceGatheringRules.hitDamage(ResourceType.ROCK, withSelectedItem("pickaxe"), Inventory.SLOT_COUNT));
    }

    @Test public void missingServerInventoryDealsNoDamage() {
        assertEquals(0, ResourceGatheringRules.hitDamage(ResourceType.TREE, null, 0));
        assertEquals(0, ResourceGatheringRules.hitDamage(ResourceType.ROCK, null, 0));
    }

    @Test public void nullResourceTypeIsRejected() {
        assertThrows(NullPointerException.class,
            () -> ResourceGatheringRules.damageRequired(null));
        assertThrows(NullPointerException.class,
            () -> ResourceGatheringRules.hitDamage(null, new Inventory(), 0));
    }

    @Test public void mixedTreeHitsAccumulateCorrectly() {
        Inventory bare = withSelectedItem(null);
        Inventory axe = withSelectedItem("axe");
        int total = ResourceGatheringRules.hitDamage(ResourceType.TREE, bare, 2);
        assertEquals(1, total);
        total += ResourceGatheringRules.hitDamage(ResourceType.TREE, axe, 2);
        assertEquals(4, total);
        assertTrue(total < ResourceGatheringRules.damageRequired(ResourceType.TREE));
        total += ResourceGatheringRules.hitDamage(ResourceType.TREE, axe, 2);
        assertTrue(total >= ResourceGatheringRules.damageRequired(ResourceType.TREE));
    }

    @Test public void mixedTreeToolThenBareHandsAccumulateCorrectly() {
        Inventory bare = withSelectedItem(null);
        Inventory axe = withSelectedItem("axe");
        int total = ResourceGatheringRules.hitDamage(ResourceType.TREE, axe, 2);
        assertEquals(3, total);
        for (int i = 1; i <= 2; i++) {
            total += ResourceGatheringRules.hitDamage(ResourceType.TREE, bare, 2);
            assertTrue(total < ResourceGatheringRules.damageRequired(ResourceType.TREE));
        }
        total += ResourceGatheringRules.hitDamage(ResourceType.TREE, bare, 2);
        assertEquals(6, total);
    }

    @Test public void mixedRockHitsAreCompletedByOnePickaxeHit() {
        Inventory bare = withSelectedItem(null);
        Inventory pickaxe = withSelectedItem("pickaxe");
        int total = 0;
        for (int i = 0; i < 6; i++) {
            total += ResourceGatheringRules.hitDamage(ResourceType.ROCK, bare, 2);
        }
        assertEquals(6, total);
        assertTrue(total < ResourceGatheringRules.damageRequired(ResourceType.ROCK));
        total += ResourceGatheringRules.hitDamage(ResourceType.ROCK, pickaxe, 2);
        assertTrue(total >= ResourceGatheringRules.damageRequired(ResourceType.ROCK));
    }

    @Test public void evaluatingDamageDoesNotChangeInventory() {
        Inventory inventory = withSelectedItem("axe");
        inventory.setSlot(3, new ItemStack("wood", 10));
        int revisionBefore = inventory.getRevision();
        assertEquals(3, ResourceGatheringRules.hitDamage(ResourceType.TREE, inventory, 2));
        assertEquals(revisionBefore, inventory.getRevision());
        assertEquals(new ItemStack("axe", 1), inventory.getSlot(2));
        assertEquals(new ItemStack("wood", 10), inventory.getSlot(3));
    }

    @Test public void evaluatingDamageNeverConsumesTheTool() {
        Inventory inventory = withSelectedItem("pickaxe");
        for (int i = 0; i < 3; i++) {
            assertEquals(7, ResourceGatheringRules.hitDamage(ResourceType.ROCK, inventory, 2));
        }
        assertEquals(new ItemStack("pickaxe", 1), inventory.getSlot(2));
    }

    @Test public void differentPlayerInventoriesProduceDifferentDamage() {
        Inventory playerA = withSelectedItem("axe");
        Inventory playerB = withSelectedItem(null);
        assertEquals(3, ResourceGatheringRules.hitDamage(ResourceType.TREE, playerA, 2));
        assertEquals(1, ResourceGatheringRules.hitDamage(ResourceType.TREE, playerB, 2));
    }
}
