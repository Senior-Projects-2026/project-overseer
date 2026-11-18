package com.projectoverseer.server.survival;

public interface SurvivalTarget {
    float hunger();

    boolean isStarving();

    void update(float deltaTime);

    void reset();
}