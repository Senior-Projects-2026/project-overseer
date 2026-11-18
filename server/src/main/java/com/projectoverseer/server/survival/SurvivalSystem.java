package com.projectoverseer.server.survival;

import com.projectoverseer.server.combat.CombatPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SurvivalSystem {
    public List<Integer> update(
        float deltaTime,
        Map<Integer, SurvivalPlayer> survivalPlayers,
        Map<Integer, CombatPlayer> combatPlayers
    ) {
        List<Integer> deadPlayers = new ArrayList<>();

        for (Map.Entry<Integer, SurvivalPlayer> entry : survivalPlayers.entrySet()) {
            int playerId = entry.getKey();
            SurvivalPlayer survivalPlayer = entry.getValue();
            CombatPlayer combatPlayer = combatPlayers.get(playerId);

            if (combatPlayer == null || !combatPlayer.isAlive()) {
                continue;
            }

            survivalPlayer.update(deltaTime);

            if (survivalPlayer.isStarving()) {
                combatPlayer.takeDamage(
                    SurvivalRules.STARVATION_DAMAGE_RATE * deltaTime
                );

                if (!combatPlayer.isAlive()) {
                    deadPlayers.add(playerId);
                }
            }
        }

        return deadPlayers;
    }
}
