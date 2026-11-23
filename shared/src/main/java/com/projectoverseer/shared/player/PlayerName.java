
package com.projectoverseer.shared.player;

import com.projectoverseer.config.SharedConfig;

public final class PlayerName {

    public static final int MAX_LENGTH = SharedConfig.getInt("player.name.max_length",16);

    private PlayerName() {
    }

    public static String normalize(String name) {
        return name == null ? "" : name.strip();
    }

    public static boolean isValid(String name) {
        String normalized = normalize(name);

        if (normalized.isEmpty()
            || normalized.length() > MAX_LENGTH) {
            return false;
        }

        for (int i = 0; i < normalized.length(); i++) {
            if (!isAllowedCharacter(normalized.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean isAllowedCharacter(char character) {
        return Character.isLetterOrDigit(character)
            || character == '_'
            || character == '-';
    }
}
