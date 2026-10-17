package com.projectoverseer.server.ai;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum EntityState implements State<AiEntity> {

    GLOBAL_STATE {
        @Override
        public void enter(AiEntity entity) {
            // This state is always active, so no need to implement enter logic.
        }

        @Override
        public void update(AiEntity entity) {
            // This state is always active, so no need to implement update logic.
        }

        @Override
        public void exit(AiEntity entity) {
            // This state is always active, so no need to implement exit logic.
        }

        @Override
        public boolean onMessage(AiEntity aiEntity, Telegram telegram) {
            return false;
        }
    },

    IDLE{

        @Override
        public void enter(AiEntity entity) {
            // Logic for entering the IDLE state
        }

        @Override
        public void update(AiEntity entity) {
            // Logic for updating the IDLE state
        }

        @Override
        public void exit(AiEntity entity) {
            // Logic for exiting the IDLE state
        }

        @Override
        public boolean onMessage(AiEntity aiEntity, Telegram telegram) {
            return false;
        }
    },

    WANDER{

        @Override
        public void enter(AiEntity entity) {
            // Logic for entering the WANDER state
        }

        @Override
        public void update(AiEntity entity) {
            // Logic for updating the WANDER state
        }

        @Override
        public void exit(AiEntity entity) {
            // Logic for exiting the WANDER state
        }

        @Override
        public boolean onMessage(AiEntity aiEntity, Telegram telegram) {
            return false;
        }
    },

    ATTACK{

        @Override
        public void enter(AiEntity entity) {
            // Logic for entering the ATTACK state
        }

        @Override
        public void update(AiEntity entity) {
            // Logic for updating the ATTACK state
        }

        @Override
        public void exit(AiEntity entity) {
            // Logic for exiting the ATTACK state
        }

        @Override
        public boolean onMessage(AiEntity aiEntity, Telegram telegram) {
            return false;
        }
    },
    FLEE{

        @Override
        public void enter(AiEntity entity) {
            // Logic for entering the FLEE state
        }

        @Override
        public void update(AiEntity entity) {
            // Logic for updating the FLEE state
        }

        @Override
        public void exit(AiEntity entity) {
            // Logic for exiting the FLEE state
        }

        @Override
        public boolean onMessage(AiEntity aiEntity, Telegram telegram) {
            return false;
        }
    };
}
