
package com.projectoverseer.gameplay;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceInventoryTest {

    @Test
    public void hotbarHasNineSlotsAndStartsEmpty() {
        Inventory inventory = new Inventory();

        assertEquals(9, Inventory.SLOT_COUNT);

        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            assertNull(inventory.getSlot(i));
        }

        assertEquals(0, inventory.getSelectedSlot());
    }

    @Test
    public void woodAndStoneNeverMergeIntoTheSameStack() {
        Inventory inventory = new Inventory();

        assertEquals(0, inventory.addItem("wood", 3));
        assertEquals(0, inventory.addItem("stone", 7));

        assertEquals(
            new ItemStack("wood", 3),
            inventory.getSlot(0)
        );

        assertEquals(
            new ItemStack("stone", 7),
            inventory.getSlot(1)
        );
    }

    @Test
    public void newWoodStackSplitsAt64WithoutLosingItems() {
        Inventory inventory = new Inventory();

        assertEquals(0, inventory.addItem("wood", 130));

        assertEquals(
            new ItemStack("wood", 64),
            inventory.getSlot(0)
        );

        assertEquals(
            new ItemStack("wood", 64),
            inventory.getSlot(1)
        );

        assertEquals(
            new ItemStack("wood", 2),
            inventory.getSlot(2)
        );
    }

    @Test
    public void existingPartialStackIsFilledBeforeUsingAnEmptySlot() {
        Inventory inventory = new Inventory();

        inventory.setSlot(
            2,
            new ItemStack("wood", 60)
        );

        assertEquals(0, inventory.addItem("wood", 10));

        assertEquals(
            new ItemStack("wood", 64),
            inventory.getSlot(2)
        );

        assertEquals(
            new ItemStack("wood", 6),
            inventory.getSlot(0)
        );
    }

    @Test
    public void completelyFullInventoryRejectsAnAdditionalResourceWithoutSideEffects() {
        Inventory inventory = new Inventory();

        assertEquals(
            0,
            inventory.addItem("wood", 9 * 64)
        );

        int revision = inventory.getRevision();

        assertEquals(
            1,
            inventory.addItem("stone", 1)
        );

        assertEquals(
            revision,
            inventory.getRevision()
        );

        for (int i = 0; i < 9; i++) {
            assertEquals(
                new ItemStack("wood", 64),
                inventory.getSlot(i)
            );
        }
    }

    @Test
    public void partialCapacityReturnsCorrectRemainderWhenGatheringStone() {
        Inventory inventory = new Inventory();

        assertEquals(
            0,
            inventory.addItem("wood", 8 * 64)
        );

        assertEquals(
            6,
            inventory.addItem("stone", 70)
        );

        assertEquals(
            new ItemStack("stone", 64),
            inventory.getSlot(8)
        );
    }

    @Test
    public void removingStoneDoesNotChangeWood() {
        Inventory inventory = new Inventory();

        inventory.addItem("wood", 12);
        inventory.addItem("stone", 7);

        assertEquals(
            4,
            inventory.removeItem("stone", 4)
        );

        assertEquals(
            new ItemStack("wood", 12),
            inventory.getSlot(0)
        );

        assertEquals(
            new ItemStack("stone", 3),
            inventory.getSlot(1)
        );
    }

    @Test
    public void removingMoreThanAvailableDoesNotProduceNegativeStacks() {
        Inventory inventory = new Inventory();

        inventory.addItem("stone", 3);

        assertEquals(
            3,
            inventory.removeItem("stone", 100)
        );

        assertNull(inventory.getSlot(0));

        assertEquals(
            0,
            inventory.removeItem("stone", 1)
        );
    }

    @Test
    public void removingAcrossMultipleStacksLeavesExpectedRemainder() {
        Inventory inventory = new Inventory();

        inventory.addItem("wood", 140);

        assertEquals(
            100,
            inventory.removeItem("wood", 100)
        );

        assertNull(inventory.getSlot(0));

        assertEquals(
            new ItemStack("wood", 28),
            inventory.getSlot(1)
        );

        assertEquals(
            new ItemStack("wood", 12),
            inventory.getSlot(2)
        );
    }

    @Test
    public void harvestingDoesNotChangeSelectedSlot() {
        Inventory inventory = new Inventory();

        inventory.selectSlot(8);

        inventory.addItem("wood", 2);
        inventory.addItem("stone", 2);

        assertEquals(
            8,
            inventory.getSelectedSlot()
        );

        assertNull(inventory.getSelectedItem());
    }

    @Test
    public void twoPlayersInventoriesAreIndependent() {
        Inventory playerA = new Inventory();
        Inventory playerB = new Inventory();

        playerA.addItem("wood", 1);
        playerB.addItem("stone", 2);

        assertNull(playerA.getSlot(1));

        assertEquals(
            new ItemStack("wood", 1),
            playerA.getSlot(0)
        );

        assertEquals(
            new ItemStack("stone", 2),
            playerB.getSlot(0)
        );
    }

    @Test
    public void revisionOnlyChangesWhenAnActualStackChanges() {
        Inventory inventory = new Inventory();

        int initial = inventory.getRevision();

        inventory.setSlot(0, null);

        assertEquals(
            initial,
            inventory.getRevision()
        );

        inventory.addItem("wood", 1);

        assertEquals(
            initial + 1,
            inventory.getRevision()
        );

        inventory.setSlot(
            0,
            new ItemStack("wood", 1)
        );

        assertEquals(
            initial + 1,
            inventory.getRevision()
        );
    }

    @Test
    public void invalidQuantityAndItemIdsAreRejected() {
        Inventory inventory = new Inventory();

        assertThrows(
            IllegalArgumentException.class,
            () -> inventory.addItem("", 1)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> inventory.addItem("wood", 0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> inventory.removeItem("stone", -1)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemStack("stone", 0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemStack("wood", 65)
        );
    }

    @Test
    public void invalidSlotIndicesAreRejected() {
        Inventory inventory = new Inventory();

        assertThrows(
            IndexOutOfBoundsException.class,
            () -> inventory.selectSlot(-1)
        );

        assertThrows(
            IndexOutOfBoundsException.class,
            () -> inventory.selectSlot(9)
        );

        assertThrows(
            IndexOutOfBoundsException.class,
            () -> inventory.getSlot(9)
        );
    }

    @Test
    public void exactlyOneWoodAndStoneUseOnlyTwoSlots() {
        Inventory inventory = new Inventory();

        assertEquals(
            0,
            inventory.addItem("wood", 1)
        );

        assertEquals(
            0,
            inventory.addItem("stone", 1)
        );

        assertEquals(
            new ItemStack("wood", 1),
            inventory.getSlot(0)
        );

        assertEquals(
            new ItemStack("stone", 1),
            inventory.getSlot(1)
        );

        assertNull(inventory.getSlot(2));
    }
}
