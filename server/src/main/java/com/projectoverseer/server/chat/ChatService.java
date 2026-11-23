package com.projectoverseer.server.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.projectoverseer.server.config.ServerConfig;
import com.projectoverseer.shared.chat.ChatMessage;

public final class ChatService {

    private static final long MIN_MESSAGE_INTERVAL_NS = ServerConfig.getLong("chat.message.interval", 500_000_000L);

    private final Consumer<ChatMessage> broadcaster;
    private final Map<Integer, Long> lastMessageTimes = new HashMap<>();

    public ChatService(Consumer<ChatMessage> broadcaster) {

        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
    }


    public boolean handleMessage(
        int senderId,
        String senderName,
        ChatMessage incoming,
        long nowNanos
    ) {
        if (senderId <= 0
            || senderName == null
            || senderName.isBlank()
            || incoming == null
            || incoming.text == null) {
            return false;
        }

        String text = incoming.text.strip();

        if (text.isEmpty() || text.length() > ChatMessage.MAX_TEXT_LENGTH) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return false;
            }
        }

        Long lastMessageTime = lastMessageTimes.get(senderId);

        if (lastMessageTime != null
            && nowNanos - lastMessageTime < MIN_MESSAGE_INTERVAL_NS) {
            return false;
        }

        lastMessageTimes.put(senderId, nowNanos);

        broadcaster.accept(
            new ChatMessage(senderId, senderName, text)
        );

        return true;
    }


    public void removePlayer(int playerId) {
        lastMessageTimes.remove(playerId);
    }
}
