package com.projectoverseer.gameplay;

public record ItemStack(String itemId, int quantity) {

    public static final int MAX_STACK_SIZE = 64;

    public ItemStack {
        int maxStack = maxStackSize(itemId);

        if (quantity < 1 || quantity > maxStack) {
            throw new IllegalArgumentException(
                "Invalid quantity for " + itemId
            );
        }
    }

    public static int maxStackSize(String itemId) {

        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException(
                "Item ID must not be blank"
            );
        }

        return switch (itemId) {
            case "axe", "pickaxe", "sword" -> 1;
            default -> MAX_STACK_SIZE;
        };
    }
}
