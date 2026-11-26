
package com.projectoverseer.shared.chat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Client;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.player.JoinRejected;
import com.projectoverseer.shared.player.JoinRequest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChatProtocolTest {

    @Test
    public void clientChatMessageContainsOnlyText() {
        ChatMessage message = new ChatMessage("Salut!");

        assertEquals("Salut!", message.text);
        assertEquals(-1, message.playerId);
        assertNull(message.playerName);
    }

    @Test
    public void serverChatMessageContainsSenderIdentity() {
        ChatMessage message = new ChatMessage(
            7, "Alex", "Salut!"
        );

        assertEquals(7, message.playerId);
        assertEquals("Alex", message.playerName);
        assertEquals("Salut!", message.text);
    }

    @Test
    public void chatMessageSupportsNoArgumentConstructor() {
        ChatMessage message = new ChatMessage();

        assertEquals(-1, message.playerId);
        assertNull(message.playerName);
        assertNull(message.text);
    }

    @Test
    public void joinRequestContainsPlayerName() {
        JoinRequest request = new JoinRequest("Maria");

        assertEquals("Maria", request.playerName);
    }

    @Test
    public void joinRequestSupportsNoArgumentConstructor() {
        JoinRequest request = new JoinRequest();

        assertNull(request.playerName);
    }

    @Test
    public void joinRejectedContainsReason() {
        JoinRejected rejected = new JoinRejected(
            "Invalid player name"
        );

        assertEquals(
            "Invalid player name",
            rejected.reason
        );
    }

    @Test
    public void joinRejectedSupportsNoArgumentConstructor() {
        JoinRejected rejected = new JoinRejected();

        assertNull(rejected.reason);
    }

    @Test
    public void joinRequestSurvivesKryoSerialization() {
        JoinRequest original = new JoinRequest("Alex");

        JoinRequest restored = (JoinRequest) roundTrip(original);

        assertEquals(original.playerName, restored.playerName);
    }

    @Test
    public void joinRejectedSurvivesKryoSerialization() {
        JoinRejected original = new JoinRejected(
            "Name rejected"
        );

        JoinRejected restored = (JoinRejected) roundTrip(original);

        assertEquals(original.reason, restored.reason);
    }

    @Test
    public void chatMessageSurvivesKryoSerialization() {
        ChatMessage original = new ChatMessage(
            12, "Ștefan", "Salut tuturor!"
        );

        ChatMessage restored = (ChatMessage) roundTrip(original);

        assertEquals(original.playerId, restored.playerId);
        assertEquals(original.playerName, restored.playerName);
        assertEquals(original.text, restored.text);
    }

    private Object roundTrip(Object message) {
        Client client = new Client();
        Network.register(client);

        Kryo kryo = client.getKryo();

        Output output = new Output(256, -1);
        kryo.writeClassAndObject(output, message);

        Input input = new Input(output.toBytes());

        return kryo.readClassAndObject(input);
    }
}
