package com.projectoverseer.gameplay;

public final class PlayerStats {

    public static final int MAX_HEALTH = 20; //minecraft like health/hunger system cus i like minecraft and u should too
    public static final int MAX_HUNGER = 20;

    private int health = MAX_HEALTH;
    private int hunger = MAX_HUNGER;

    public int getHealth() {
        return health;
    }

    public int getHunger() {
        return hunger;
    }

    public void setHealth(int health) {
        this.health = clamp(health, MAX_HEALTH);
    }

    public void setHunger(int hunger) {
        this.hunger = clamp(hunger, MAX_HUNGER);
    }

    public void damage(int amount) {
        requireNonNegative(amount);
        health = clamp((long) health - amount, MAX_HEALTH);
    }

    public void heal(int amount) {
        requireNonNegative(amount);
        health = clamp((long) health + amount, MAX_HEALTH);
    }

    public void consumeHunger(int amount) {
        requireNonNegative(amount);
        hunger = clamp((long) hunger - amount, MAX_HUNGER);
    }

    public void feed(int amount) {
        requireNonNegative(amount);
        hunger = clamp((long) hunger + amount, MAX_HUNGER);
    }

    private static int clamp(long value, int maximum) {
        return (int) Math.max(0, Math.min(maximum, value));
    }

    private static void requireNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                "Amount must not be negative"
            );
        }
    }
}
