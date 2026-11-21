package com.projectoverseer.shared.items;

public enum FoodItem {
    BERRY("berry", 4);

    private final String itemId;
    private final int hungerRestoration;

    FoodItem(String itemId, int hungerRestoration) {
        this.itemId = itemId;
        this.hungerRestoration = hungerRestoration;
    }

    public int hungerRestoration() {
        return hungerRestoration;
    }

    public static FoodItem fromId(String itemId) {
        for (FoodItem food : values()) {
            if (food.itemId.equals(itemId)) {
                return food;
            }
        }
        return null;
    }
}