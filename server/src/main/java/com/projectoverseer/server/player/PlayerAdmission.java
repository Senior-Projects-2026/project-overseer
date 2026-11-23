package com.projectoverseer.server.player;

import java.util.HashMap;
import java.util.Map;

import com.projectoverseer.shared.player.PlayerName;

public final class PlayerAdmission {

    public enum Status {
        ACCEPTED,
        INVALID_NAME,
        ALREADY_REGISTERED
    }

    public record Result(Status status, String playerName) {
    }

    private final Map<Integer, String> playerNames = new HashMap<>();

    public Result register(int playerId, String proposedName) {
        if (playerId <= 0) {
            return new Result(Status.INVALID_NAME, null);
        }

        String existingName = playerNames.get(playerId);

        if (existingName != null) {
            return new Result(Status.ALREADY_REGISTERED, existingName);
        }

        String normalizedName = PlayerName.normalize(proposedName);

        if (!PlayerName.isValid(normalizedName)) {
            return new Result(Status.INVALID_NAME, null);
        }

        playerNames.put(playerId, normalizedName);

        return new Result(Status.ACCEPTED, normalizedName);
    }

    public boolean isRegistered(int playerId) {
        return playerNames.containsKey(playerId);
    }

    public String getName(int playerId) {
        return playerNames.get(playerId);
    }

    public boolean removePlayer(int playerId) {
        return playerNames.remove(playerId) != null;
    }

    public boolean isEmpty() {
        return playerNames.isEmpty();
    }
}
