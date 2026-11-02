package com.projectoverseer.gameplay;

public record ItemStack(String itemId, int quantity) {

    public static final int MAX_STACK_SIZE = 64;

    public ItemStack {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException(
                "Item ID must not be blank"
            );
        }

        if (quantity < 1 || quantity > MAX_STACK_SIZE) {
            throw new IllegalArgumentException(
                "Quantity must be between 1 and " + MAX_STACK_SIZE
            );
        }
    }
}
