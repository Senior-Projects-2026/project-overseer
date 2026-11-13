package com.projectoverseer.server.ai;

import com.badlogic.gdx.math.Vector2;

/**
 * Anything an AI entity can aggro on or attack (players, NPCs, structures...).
 * The server's player entity should implement this too.
 */
public interface Targetable {

    Vector2 getPosition();

    boolean isAlive();

    void takeDamage(float amount);
}
