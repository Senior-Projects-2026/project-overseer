
package com.projectoverseer.shared.crafting;

public enum CraftingRecipe {

    AXE("axe", 2, 3),
    PICKAXE("pickaxe", 2, 3),
    SWORD("sword", 1, 2);

    private final String itemId;
    private final int woodCost;
    private final int stoneCost;

    CraftingRecipe(String itemId, int woodCost, int stoneCost) {
        this.itemId = itemId;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
    }

    public String getItemId() {
        return itemId;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    public boolean canCraft(int wood, int stone) {
        return wood >= woodCost && stone >= stoneCost;
    }

    public static CraftingRecipe fromId(String itemId) {
        for (CraftingRecipe recipe : values()) {
            if (recipe.itemId.equals(itemId)) {
                return recipe;
            }
        }

        return null;
    }
}
