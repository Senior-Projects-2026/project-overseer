package com.projectoverseer.shared.crafting;

import org.junit.Test;

import static org.junit.Assert.*;

public class CraftingRecipeTest {

    @Test
    public void exactlyThreeRecipesExist() {
        assertEquals(3, CraftingRecipe.values().length);
    }

    @Test
    public void axeRecipeHasExpectedCosts() {
        assertEquals("axe", CraftingRecipe.AXE.getItemId());
        assertEquals(2, CraftingRecipe.AXE.getWoodCost());
        assertEquals(3, CraftingRecipe.AXE.getStoneCost());
    }

    @Test
    public void pickaxeRecipeHasExpectedCosts() {
        assertEquals("pickaxe", CraftingRecipe.PICKAXE.getItemId());
        assertEquals(2, CraftingRecipe.PICKAXE.getWoodCost());
        assertEquals(3, CraftingRecipe.PICKAXE.getStoneCost());
    }

    @Test
    public void swordRecipeHasExpectedCosts() {
        assertEquals("sword", CraftingRecipe.SWORD.getItemId());
        assertEquals(1, CraftingRecipe.SWORD.getWoodCost());
        assertEquals(2, CraftingRecipe.SWORD.getStoneCost());
    }

    @Test
    public void everyRecipeCanBeLookedUpByItemId() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertSame(recipe, CraftingRecipe.fromId(recipe.getItemId()));
        }
    }

    @Test
    public void invalidItemIdsDoNotResolveToRecipes() {
        assertNull(CraftingRecipe.fromId(null));
        assertNull(CraftingRecipe.fromId(""));
        assertNull(CraftingRecipe.fromId("wood"));
        assertNull(CraftingRecipe.fromId("dagger"));
        assertNull(CraftingRecipe.fromId("AXE"));
    }

    @Test
    public void exactMaterialRequirementsAllowCrafting() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertTrue(recipe.canCraft(
                recipe.getWoodCost(), recipe.getStoneCost()
            ));
        }
    }

    @Test
    public void extraMaterialsAllowCrafting() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertTrue(recipe.canCraft(
                recipe.getWoodCost() + 50, recipe.getStoneCost() + 50
            ));
        }
    }

    @Test
    public void missingWoodPreventsCrafting() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertFalse(recipe.canCraft(
                recipe.getWoodCost() - 1, recipe.getStoneCost()
            ));
        }
    }

    @Test
    public void missingStonePreventsCrafting() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertFalse(recipe.canCraft(
                recipe.getWoodCost(), recipe.getStoneCost() - 1
            ));
        }
    }

    @Test
    public void noMaterialsPreventCrafting() {
        for (CraftingRecipe recipe : CraftingRecipe.values()) {
            assertFalse(recipe.canCraft(0, 0));
        }
    }
}
