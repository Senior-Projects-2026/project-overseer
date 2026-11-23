
package com.projectoverseer.server.player;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.Network.Ping;
import com.projectoverseer.shared.Network.Welcome;
import com.projectoverseer.shared.player.JoinRejected;
import com.projectoverseer.shared.player.JoinRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAdmissionNetworkTest {

    private Server server;
    private Client client;
    private PlayerAdmission admission;

    private BlockingQueue<Object> receivedMessages;
    private CountDownLatch disconnected;
    private AtomicInteger successfulJoins;

    @BeforeEach
    void setUp() throws IOException {
        admission = new PlayerAdmission();
        receivedMessages = new LinkedBlockingQueue<>();
        disconnected = new CountDownLatch(1);
        successfulJoins = new AtomicInteger();

        server = new Server();
        Network.register(server);

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof JoinRequest request) {
                    PlayerAdmission.Result result = admission.register(
                        connection.getID(),
                        request.playerName
                    );

                    if (result.status()
                        == PlayerAdmission.Status.INVALID_NAME) {

                        connection.sendTCP(
                            new JoinRejected("Invalid player name")
                        );
                        return;
                    }

                    if (result.status()
                        == PlayerAdmission.Status.ACCEPTED) {

                        successfulJoins.incrementAndGet();

                        Welcome welcome = new Welcome();
                        welcome.playerId = connection.getID();
                        welcome.worldSeed = 123L;
                        welcome.generatorVersion = 1;

                        connection.sendTCP(welcome);
                    }

                    return;
                }

                if (!admission.isRegistered(connection.getID())) {
                    return;
                }

                if (object instanceof Ping) {
                    Ping response = new Ping();
                    response.message = "pong";
                    connection.sendTCP(response);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                admission.removePlayer(connection.getID());
                disconnected.countDown();
            }
        });

        int port;

        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        server.start();
        server.bind(port);

        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                receivedMessages.offer(object);
            }
        });

        client.start();
        client.connect(3000, "127.0.0.1", port);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.stop();
        }

        if (server != null) {
            server.stop();
        }
    }

    @Test
    void doesNotSendWelcomeBeforeJoinRequest()
        throws InterruptedException {

        Object response = receivedMessages.poll(
            300, TimeUnit.MILLISECONDS
        );

        assertNull(response);
        assertEquals(0, successfulJoins.get());
    }

    @Test
    void rejectsInvalidNameWithoutSendingWelcome()
        throws InterruptedException {

        client.sendTCP(new JoinRequest("Invalid!"));

        Object response = nextMessage();

        assertInstanceOf(JoinRejected.class, response);

        assertNull(receivedMessages.poll(
            300, TimeUnit.MILLISECONDS
        ));

        assertEquals(0, successfulJoins.get());
    }

    @Test
    void acceptsValidNameAndSendsWelcome()
        throws InterruptedException {

        client.sendTCP(new JoinRequest("Alex"));

        Welcome welcome = assertInstanceOf(
            Welcome.class,
            nextMessage()
        );

        assertTrue(welcome.playerId > 0);
        assertEquals("Alex", admission.getName(welcome.playerId));
        assertEquals(1, successfulJoins.get());
    }

    @Test
    void doesNotRegisterSameConnectionTwice()
        throws InterruptedException {

        client.sendTCP(new JoinRequest("Alex"));

        Welcome welcome = assertInstanceOf(
            Welcome.class,
            nextMessage()
        );

        client.sendTCP(new JoinRequest("Maria"));

        assertNull(receivedMessages.poll(
            300, TimeUnit.MILLISECONDS
        ));

        assertEquals(1, successfulJoins.get());
        assertEquals("Alex", admission.getName(welcome.playerId));
    }

    @Test
    void allowsValidJoinAfterInvalidAttempt()
        throws InterruptedException {

        client.sendTCP(new JoinRequest("Invalid!"));

        assertInstanceOf(
            JoinRejected.class,
            nextMessage()
        );

        client.sendTCP(new JoinRequest("Maria"));

        Welcome welcome = assertInstanceOf(
            Welcome.class,
            nextMessage()
        );

        assertEquals("Maria", admission.getName(welcome.playerId));
        assertEquals(1, successfulJoins.get());
    }

    @Test
    void ignoresGameplayMessagesBeforeRegistration()
        throws InterruptedException {

        Ping ping = new Ping();
        ping.message = "hello";

        client.sendTCP(ping);

        assertNull(receivedMessages.poll(
            300, TimeUnit.MILLISECONDS
        ));

        client.sendTCP(new JoinRequest("Alex"));

        assertInstanceOf(
            Welcome.class,
            nextMessage()
        );

        client.sendTCP(ping);

        Ping response = assertInstanceOf(
            Ping.class,
            nextMessage()
        );

        assertEquals("pong", response.message);
    }

    @Test
    void removesRegistrationWhenClientDisconnects()
        throws InterruptedException {

        client.sendTCP(new JoinRequest("Alex"));

        Welcome welcome = assertInstanceOf(
            Welcome.class,
            nextMessage()
        );

        client.stop();

        assertTrue(disconnected.await(
            3, TimeUnit.SECONDS
        ));

        assertFalse(admission.isRegistered(welcome.playerId));
        assertTrue(admission.isEmpty());
    }

    private Object nextMessage() throws InterruptedException {
        Object response = receivedMessages.poll(
            3, TimeUnit.SECONDS
        );

        assertNotNull(response, "Expected a network response");
        return response;
    }
}
