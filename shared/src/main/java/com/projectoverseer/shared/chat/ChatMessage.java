
package com.projectoverseer.shared.chat;

import com.projectoverseer.config.SharedConfig;

public class ChatMessage {
    public static final int MAX_TEXT_LENGTH = SharedConfig.getInt("chat.message.maxlength",200);

    public int playerId = -1;
    public String text;

    public ChatMessage() {
    }

    public ChatMessage(String text) {
        this.text = text;
    }

    public ChatMessage(int playerId, String text) {
        this.playerId = playerId;
        this.text = text;
    }
}
