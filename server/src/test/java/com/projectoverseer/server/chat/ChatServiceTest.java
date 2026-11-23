
package com.projectoverseer.server.chat;

import com.projectoverseer.server.config.ServerConfig;
import com.projectoverseer.shared.chat.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private static final long INTERVAL_NS =
        ServerConfig.getLong(
            "chat.message.interval",
            500_000_000L
        );

    private List<ChatMessage> broadcastMessages;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        broadcastMessages = new ArrayList<>();
        chatService = new ChatService(broadcastMessages::add);
    }

    @Test
    void validMessageIsBroadcast() {
        boolean accepted = chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Salut!"),
            0L
        );

        assertTrue(accepted);
        assertEquals(1, broadcastMessages.size());
        assertEquals("Salut!", broadcastMessages.get(0).text);
    }

    @Test
    void senderIdentityComesFromServer() {
        ChatMessage forged = new ChatMessage(
            999,
            "Admin",
            "Salut!"
        );

        boolean accepted = chatService.handleMessage(
            7,
            "Maria",
            forged,
            0L
        );

        assertTrue(accepted);
        assertEquals(1, broadcastMessages.size());

        ChatMessage result = broadcastMessages.get(0);

        assertEquals(7, result.playerId);
        assertEquals("Maria", result.playerName);
        assertEquals("Salut!", result.text);
    }

    @Test
    void messageIsTrimmedBeforeBroadcast() {
        boolean accepted = chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("   Salut!   "),
            0L
        );

        assertTrue(accepted);
        assertEquals("Salut!", broadcastMessages.get(0).text);
    }

    @Test
    void nullMessageIsRejected() {
        assertFalse(chatService.handleMessage(
            1, "Alex", null, 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void nullTextIsRejected() {
        assertFalse(chatService.handleMessage(
            1, "Alex", new ChatMessage(null), 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void emptyAndWhitespaceMessagesAreRejected() {
        assertFalse(chatService.handleMessage(
            1, "Alex", new ChatMessage(""), 0L
        ));

        assertFalse(chatService.handleMessage(
            1, "Alex", new ChatMessage("   "), 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void invalidSenderIdIsRejected() {
        assertFalse(chatService.handleMessage(
            0, "Alex", new ChatMessage("Salut"), 0L
        ));

        assertFalse(chatService.handleMessage(
            -1, "Alex", new ChatMessage("Salut"), 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void missingSenderNameIsRejected() {
        assertFalse(chatService.handleMessage(
            1, null, new ChatMessage("Salut"), 0L
        ));

        assertFalse(chatService.handleMessage(
            1, "   ", new ChatMessage("Salut"), 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void messageAtMaximumLengthIsAccepted() {
        String text = "A".repeat(
            ChatMessage.MAX_TEXT_LENGTH
        );

        assertTrue(chatService.handleMessage(
            1, "Alex", new ChatMessage(text), 0L
        ));

        assertEquals(
            ChatMessage.MAX_TEXT_LENGTH,
            broadcastMessages.get(0).text.length()
        );
    }

    @Test
    void messageExceedingMaximumLengthIsRejected() {
        String text = "A".repeat(
            ChatMessage.MAX_TEXT_LENGTH + 1
        );

        assertFalse(chatService.handleMessage(
            1, "Alex", new ChatMessage(text), 0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void controlCharactersInsideMessageAreRejected() {
        assertFalse(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Salut\nMaria"),
            0L
        ));

        assertFalse(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Salut\u0000Maria"),
            0L
        ));

        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    void cooldownRejectsMessagesSentTooQuickly() {
        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Primul"),
            0L
        ));

        assertFalse(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Al doilea"),
            INTERVAL_NS - 1
        ));

        assertEquals(1, broadcastMessages.size());
    }

    @Test
    void messageIsAcceptedAtCooldownBoundary() {
        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Primul"),
            0L
        ));

        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Al doilea"),
            INTERVAL_NS
        ));

        assertEquals(2, broadcastMessages.size());
    }

    @Test
    void cooldownIsIndependentForEachPlayer() {
        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Salut"),
            0L
        ));

        assertTrue(chatService.handleMessage(
            2,
            "Maria",
            new ChatMessage("Salut"),
            0L
        ));

        assertEquals(2, broadcastMessages.size());
    }

    @Test
    void invalidMessageDoesNotConsumeCooldown() {
        assertFalse(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage(""),
            0L
        ));

        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Salut"),
            0L
        ));

        assertEquals(1, broadcastMessages.size());
    }

    @Test
    void disconnectRemovesPlayerCooldown() {
        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Primul"),
            0L
        ));

        chatService.removePlayer(1);

        assertTrue(chatService.handleMessage(
            1,
            "Alex",
            new ChatMessage("Dupa reconectare"),
            1L
        ));

        assertEquals(2, broadcastMessages.size());
    }

    @Test
    void nullBroadcasterIsRejected() {
        assertThrows(
            NullPointerException.class,
            () -> new ChatService(null)
        );
    }
}
