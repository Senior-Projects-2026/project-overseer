package com.projectoverseer.gameplay;

import com.projectoverseer.shared.crafting.CraftingRecipe;
import org.junit.Test;

import static org.junit.Assert.*;

public class CraftingInventoryTest {

    @Test
    public void craftingEachRecipeConsumesExactMaterials() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            Inventory inventory = new Inventory();
            inventory.addItem("wood", 10);
            inventory.addItem("stone", 10);

            assertTrue(inventory.tryCraft(recipe));
            assertEquals(10 - recipe.getWoodCost(), inventory.countItem("wood"));
            assertEquals(10 - recipe.getStoneCost(), inventory.countItem("stone"));
            assertEquals(1, inventory.countItem(recipe.getItemId()));
        }
    }

    @Test
    public void craftingAtExactCostRemovesBothMaterialStacks() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            Inventory inventory = new Inventory();
            inventory.addItem("wood", recipe.getWoodCost());
            inventory.addItem("stone", recipe.getStoneCost());

            assertTrue(inventory.tryCraft(recipe));
            assertEquals(0, inventory.countItem("wood"));
            assertEquals(0, inventory.countItem("stone"));
            assertEquals(1, inventory.countItem(recipe.getItemId()));
            assertEquals(new ItemStack(recipe.getItemId(), 1), inventory.getSlot(0));
        }
    }

    @Test
    public void craftingWithoutWoodDoesNotMutateInventory() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 1);
        inventory.addItem("stone", 20);
        assertUnchangedAfterFailure(inventory, CraftingRecipe.AXE);
    }

    @Test
    public void craftingWithoutStoneDoesNotMutateInventory() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 20);
        inventory.addItem("stone", 2);
        assertUnchangedAfterFailure(inventory, CraftingRecipe.PICKAXE);
    }

    @Test
    public void craftingWithEmptyInventoryDoesNothing() {
        Inventory inventory = new Inventory();
        assertUnchangedAfterFailure(inventory, CraftingRecipe.SWORD);
    }

    @Test
    public void craftingFailsAtomicallyWhenAllSlotsRemainOccupied() {
        Inventory inventory = new Inventory();
        inventory.setSlot(0, new ItemStack("wood", 3));
        inventory.setSlot(1, new ItemStack("stone", 4));
        for (int i = 2; i < Inventory.SLOT_COUNT; i++) {
            inventory.setSlot(i, new ItemStack("sword", 1));
        }

        assertUnchangedAfterFailure(inventory, CraftingRecipe.AXE);
    }

    @Test
    public void craftingSucceedsWithFullInventoryIfMaterialsFreeASlot() {
        Inventory inventory = new Inventory();
        inventory.setSlot(0, new ItemStack("wood", 3));
        inventory.setSlot(1, new ItemStack("stone", 3));
        for (int i = 2; i < Inventory.SLOT_COUNT; i++) {
            inventory.setSlot(i, new ItemStack("sword", 1));
        }

        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertEquals(1, inventory.countItem("wood"));
        assertEquals(0, inventory.countItem("stone"));
        assertEquals(1, inventory.countItem("axe"));
        assertEquals(new ItemStack("axe", 1), inventory.getSlot(1));
    }

    @Test
    public void eachCraftedToolOccupiesItsOwnSlot() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 10);
        inventory.addItem("stone", 10);

        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));

        assertEquals(2, inventory.countItem("axe"));
        int axeSlots = 0;
        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            ItemStack stack = inventory.getSlot(i);
            if (stack != null && stack.itemId().equals("axe")) {
                assertEquals(1, stack.quantity());
                axeSlots++;
            }
        }
        assertEquals(2, axeSlots);
    }

    @Test
    public void nonStackableToolsCannotFillAnAlreadyFullHotbar() {
        Inventory inventory = new Inventory();
        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            inventory.setSlot(i, new ItemStack("axe", 1));
        }

        int revision = inventory.getRevision();
        assertEquals(1, inventory.addItem("axe", 1));
        assertEquals(revision, inventory.getRevision());
    }

    @Test
    public void multipleCraftsStopWhenMaterialsRunOut() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 4);
        inventory.addItem("stone", 6);

        assertTrue(inventory.tryCraft(CraftingRecipe.PICKAXE));
        assertTrue(inventory.tryCraft(CraftingRecipe.PICKAXE));
        assertFalse(inventory.tryCraft(CraftingRecipe.PICKAXE));
        assertEquals(0, inventory.countItem("wood"));
        assertEquals(0, inventory.countItem("stone"));
        assertEquals(2, inventory.countItem("pickaxe"));
    }

    @Test
    public void craftingDifferentRecipesSharesMaterialBudget() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 3);
        inventory.addItem("stone", 5);

        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertTrue(inventory.tryCraft(CraftingRecipe.SWORD));
        assertFalse(inventory.tryCraft(CraftingRecipe.PICKAXE));
        assertEquals(1, inventory.countItem("axe"));
        assertEquals(1, inventory.countItem("sword"));
        assertEquals(0, inventory.countItem("wood"));
        assertEquals(0, inventory.countItem("stone"));
    }

    @Test
    public void craftingPreservesUnrelatedItems() {
        Inventory inventory = new Inventory();
        inventory.setSlot(5, new ItemStack("pickaxe", 1));
        inventory.addItem("wood", 5);
        inventory.addItem("stone", 5);

        assertTrue(inventory.tryCraft(CraftingRecipe.SWORD));
        assertEquals(new ItemStack("pickaxe", 1), inventory.getSlot(5));
        assertEquals(1, inventory.countItem("pickaxe"));
        assertEquals(1, inventory.countItem("sword"));
    }

    @Test
    public void craftingDoesNotChangeTheSelectedHotbarSlot() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 5);
        inventory.addItem("stone", 5);
        inventory.selectSlot(8);

        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertEquals(8, inventory.getSelectedSlot());
        assertNull(inventory.getSelectedItem());
    }

    @Test
    public void failedCraftKeepsSelectedSlotAndRevision() {
        Inventory inventory = new Inventory();
        inventory.selectSlot(7);
        inventory.addItem("wood", 5);

        int revision = inventory.getRevision();
        assertFalse(inventory.tryCraft(CraftingRecipe.SWORD));
        assertEquals(7, inventory.getSelectedSlot());
        assertEquals(revision, inventory.getRevision());
    }

    @Test
    public void successfulCraftIncreasesRevision() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 3);
        inventory.addItem("stone", 3);

        int revision = inventory.getRevision();
        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertTrue(inventory.getRevision() > revision);
    }

    @Test
    public void craftingWithMaterialsSpreadAcrossStacksWorks() {
        Inventory inventory = new Inventory();
        inventory.setSlot(0, new ItemStack("wood", 1));
        inventory.setSlot(3, new ItemStack("wood", 2));
        inventory.setSlot(5, new ItemStack("stone", 1));
        inventory.setSlot(7, new ItemStack("stone", 3));

        assertTrue(inventory.tryCraft(CraftingRecipe.AXE));
        assertEquals(1, inventory.countItem("wood"));
        assertEquals(1, inventory.countItem("stone"));
        assertEquals(1, inventory.countItem("axe"));
    }

    @Test
    public void twoPlayerInventoriesCannotSpendEachOthersMaterials() {
        Inventory playerA = new Inventory();
        Inventory playerB = new Inventory();
        playerA.addItem("wood", 2);
        playerA.addItem("stone", 3);
        playerB.addItem("wood", 2);

        assertTrue(playerA.tryCraft(CraftingRecipe.AXE));
        assertFalse(playerB.tryCraft(CraftingRecipe.AXE));
        assertEquals(1, playerA.countItem("axe"));
        assertEquals(0, playerB.countItem("axe"));
        assertEquals(2, playerB.countItem("wood"));
    }

    @Test
    public void craftingDoesNotModifyAnotherPlayersInventory() {
        Inventory playerA = new Inventory();
        Inventory playerB = new Inventory();
        playerA.addItem("wood", 2);
        playerA.addItem("stone", 3);
        playerB.addItem("stone", 12);

        ItemStack[] otherBefore = snapshot(playerB);
        assertTrue(playerA.tryCraft(CraftingRecipe.PICKAXE));
        assertArrayEquals(otherBefore, snapshot(playerB));
    }

    @Test
    public void nullRecipeIsRejectedWithoutChangingInventory() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 2);
        inventory.addItem("stone", 3);
        ItemStack[] before = snapshot(inventory);
        int revision = inventory.getRevision();

        assertThrows(NullPointerException.class, () -> inventory.tryCraft(null));
        assertArrayEquals(before, snapshot(inventory));
        assertEquals(revision, inventory.getRevision());
    }

    @Test
    public void craftingDoesNotAffectWoodStoneCountsOfOtherItems() {
        Inventory inventory = new Inventory();
        inventory.addItem("wood", 64);
        inventory.addItem("stone", 64);
        inventory.setSlot(8, new ItemStack("sword", 1));

        assertTrue(inventory.tryCraft(CraftingRecipe.PICKAXE));
        assertEquals(62, inventory.countItem("wood"));
        assertEquals(61, inventory.countItem("stone"));
        assertEquals(1, inventory.countItem("sword"));
        assertEquals(1, inventory.countItem("pickaxe"));
    }

    @Test
    public void inventoryStateCanBeCopiedExactlyAfterCrafting() {
        Inventory server = new Inventory();
        Inventory client = new Inventory();
        server.addItem("wood", 6);
        server.addItem("stone", 7);
        client.addItem("wood", 20);

        assertTrue(server.tryCraft(CraftingRecipe.AXE));
        ItemStack[] authoritativeSlots = snapshot(server);
        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            client.setSlot(i, authoritativeSlots[i]);
        }

        assertArrayEquals(snapshot(server), snapshot(client));
    }

    private static void assertUnchangedAfterFailure(
        Inventory inventory, CraftingRecipe recipe
    ) {
        ItemStack[] before = snapshot(inventory);
        int revision = inventory.getRevision();
        int selection = inventory.getSelectedSlot();

        assertFalse(inventory.tryCraft(recipe));
        assertArrayEquals(before, snapshot(inventory));
        assertEquals(revision, inventory.getRevision());
        assertEquals(selection, inventory.getSelectedSlot());
    }

    private static ItemStack[] snapshot(Inventory inventory) {
        ItemStack[] state = new ItemStack[Inventory.SLOT_COUNT];
        for (int i = 0; i < Inventory.SLOT_COUNT; i++) {
            state[i] = inventory.getSlot(i);
        }
        return state;
    }
}
