package com.projectoverseer.shared.world.generation;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.projectoverseer.shared.Network;
import com.projectoverseer.shared.world.Chunk;
import org.junit.Test;

import static org.junit.Assert.*;

public class WelcomeWorldTest {
    @Test
    public void serializedWelcomeRecreatesServerTerrainAndResources() {
        // Separate Kryo instances use the same registrations as the real endpoints.
        Client sender = new Client();
        Client receiver = new Client();
        try {
            Network.register(sender);
            Network.register(receiver);
            for (long seed : new long[]{123L, -123L, Long.MIN_VALUE, Long.MAX_VALUE}) {
                Network.Welcome sent = new Network.Welcome();
                sent.playerId = 17;
                sent.worldSeed = seed;
                sent.generatorVersion = WorldGenerator.VERSION;
                Network.Welcome received;
                try (Output output = new Output(256, -1)) {
                    sender.getKryo().writeClassAndObject(output, sent);
                    try (Input input = new Input(output.toBytes())) {
                        received = (Network.Welcome) receiver.getKryo().readClassAndObject(input);
                    }
                }
                assertEquals(sent.playerId, received.playerId);
                assertEquals(seed, received.worldSeed);
                assertEquals(WorldGenerator.VERSION, received.generatorVersion);
                var serverChunks = new WorldGenerator(seed).generateInitialChunks();
                var clientChunks = WorldGenerator.fromServer(received.worldSeed,
                    received.generatorVersion).generateInitialChunks();
                assertEquals(9, serverChunks.size());
                assertEquals(serverChunks.size(), clientChunks.size());
                for (int i = 0; i < serverChunks.size(); i++) {
                    Chunk a = serverChunks.get(i);
                    Chunk b = clientChunks.get(i);
                    assertEquals(a.getChunkX(), b.getChunkX());
                    assertEquals(a.getChunkY(), b.getChunkY());
                    assertEquals(a.getResources(), b.getResources());
                    for (int y = 0; y < Chunk.SIZE; y++) {
                        for (int x = 0; x < Chunk.SIZE; x++) {
                            assertEquals(a.getTile(x, y), b.getTile(x, y));
                        }
                    }
                }
            }
        } finally {
            sender.stop();
            receiver.stop();
        }
    }

    @Test
    public void rejectsUnknownOrMissingGeneratorVersion() {
        assertThrows(IllegalArgumentException.class, () -> WorldGenerator.fromServer(123L, 0));
        assertThrows(IllegalArgumentException.class,
            () -> WorldGenerator.fromServer(123L, WorldGenerator.VERSION + 1));
    }
}
