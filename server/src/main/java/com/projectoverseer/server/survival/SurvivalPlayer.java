package com.projectoverseer.server.survival;

public final class SurvivalPlayer implements SurvivalTarget {
    private float hunger = SurvivalRules.PLAYER_MAX_HUNGER;

    @Override
    public float hunger() {
        return hunger;
    }

    @Override
    public boolean isStarving() {
        return hunger <= 0;
    }

    @Override
    public void update(float deltaTime) {
        if (deltaTime < 0 || !Float.isFinite(deltaTime)) {
            throw new IllegalArgumentException("Invalid delta time");
        }

        hunger = Math.max(0, hunger - SurvivalRules.HUNGER_DEPLETION_RATE * deltaTime);
    }

    @Override
    public void reset() {
        hunger = SurvivalRules.PLAYER_MAX_HUNGER;
    }
}