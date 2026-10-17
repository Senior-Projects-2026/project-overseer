package com.projectoverseer.server.ai;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;


public class AiEntity {

    private final String id;
    private final Vector2 position = new Vector2();
    private final Vector2 spownPosition= new Vector2();
    private final Vector2 targetPosition= new Vector2();

    private final StateMachine<AiEntity, EntityState> stateMachine;

    public AiEntity(String id, float spawnX, float spawnY) {
        this.id = id;
        this.spownPosition.set(spawnX, spawnY);
        this.position.set(spawnX, spawnY);
        this.stateMachine = new DefaultStateMachine<>(this, EntityState.IDLE,EntityState.GLOBAL_STATE);

    }






}
