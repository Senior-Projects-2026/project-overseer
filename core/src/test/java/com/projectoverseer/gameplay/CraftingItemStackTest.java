package com.projectoverseer.gameplay;

import org.junit.Test;

import static org.junit.Assert.*;

public class CraftingItemStackTest {

    @Test
    public void materialsCanStackUpTo64() {
        assertEquals(64, ItemStack.maxStackSize("wood"));
        assertEquals(64, ItemStack.maxStackSize("stone"));
        assertEquals(64, new ItemStack("wood", 64).quantity());
        assertEquals(64, new ItemStack("stone", 64).quantity());
    }

    @Test
    public void toolsHaveMaximumStackSizeOfOne() {
        for (String itemId : new String[]{"axe", "pickaxe", "sword"}) {
            assertEquals(1, ItemStack.maxStackSize(itemId));
            assertEquals(1, new ItemStack(itemId, 1).quantity());
        }
    }

    @Test
    public void toolsCannotBeConstructedWithQuantityTwo() {
        for (String itemId : new String[]{"axe", "pickaxe", "sword"}) {
            assertThrows(
                IllegalArgumentException.class,
                () -> new ItemStack(itemId, 2)
            );
        }
    }

    @Test
    public void materialStacksCannotExceed64() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemStack("wood", 65)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemStack("stone", 65)
        );
    }

    @Test
    public void emptyAndNegativeQuantitiesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack("wood", 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack("axe", -1));
    }

    @Test
    public void nullAndBlankItemIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack("   ", 1));
        assertThrows(IllegalArgumentException.class, () -> ItemStack.maxStackSize(null));
    }

    @Test
    public void equalStacksHaveValueEquality() {
        assertEquals(new ItemStack("wood", 5), new ItemStack("wood", 5));
        assertNotEquals(new ItemStack("wood", 5), new ItemStack("stone", 5));
        assertNotEquals(new ItemStack("wood", 5), new ItemStack("wood", 6));
    }
}
