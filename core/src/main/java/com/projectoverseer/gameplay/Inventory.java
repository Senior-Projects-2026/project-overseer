package com.projectoverseer.gameplay;

import java.util.Objects;

public final class Inventory {

    public static final int SLOT_COUNT = 9;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    private int selectedSlot = 0;
    private int revision = 0;

    public void selectSlot(int index) {
        checkIndex(index);
        selectedSlot = index;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public ItemStack getSelectedItem() {
        return slots[selectedSlot];
    }

    public ItemStack getSlot(int index) {
        checkIndex(index);
        return slots[index];
    }

    public void setSlot(int index, ItemStack stack) {
        checkIndex(index);

        if (!Objects.equals(slots[index], stack)) {
            slots[index] = stack;
            revision++;
        }
    }

    public void clearSlot(int index) {
        setSlot(index, null);
    }

    public int addItem(String itemId, int quantity) {
        validateItem(itemId, quantity);

        int remaining = quantity;

        for (int i = 0; i < SLOT_COUNT && remaining > 0; i++) {
            ItemStack stack = slots[i];

            if (stack != null
                && stack.itemId().equals(itemId)
                && stack.quantity() < ItemStack.MAX_STACK_SIZE) {

                int added = Math.min(
                    remaining,
                    ItemStack.MAX_STACK_SIZE - stack.quantity()
                );

                setSlot(i, new ItemStack(
                    itemId,
                    stack.quantity() + added
                ));

                remaining -= added;
            }
        }

        for (int i = 0; i < SLOT_COUNT && remaining > 0; i++) {
            if (slots[i] == null) {
                int added = Math.min(
                    remaining,
                    ItemStack.MAX_STACK_SIZE
                );

                setSlot(i, new ItemStack(itemId, added));
                remaining -= added;
            }
        }

        return remaining;
    }

    public int removeItem(String itemId, int quantity) {
        validateItem(itemId, quantity);

        int remaining = quantity;

        for (int i = 0; i < SLOT_COUNT && remaining > 0; i++) {
            ItemStack stack = slots[i];

            if (stack != null && stack.itemId().equals(itemId)) {
                int removed = Math.min(remaining, stack.quantity());
                int newQuantity = stack.quantity() - removed;

                setSlot(i, newQuantity == 0
                    ? null
                    : new ItemStack(itemId, newQuantity));

                remaining -= removed;
            }
        }

        return quantity - remaining;
    }
    public int getRevision() {
        return revision;
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                "Invalid inventory slot: " + index
            );
        }
    }

    private static void validateItem(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Invalid item ID");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException(
                "Quantity must be positive"
            );
        }
    }
}

