
package com.projectoverseer.server.player;

import com.projectoverseer.shared.player.PlayerName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAdmissionTest {

    private PlayerAdmission admission;

    @BeforeEach
    void setUp() {
        admission = new PlayerAdmission();
    }

    @Test
    void acceptsValidName() {
        PlayerAdmission.Result result =
            admission.register(1, "Alex");

        assertEquals(PlayerAdmission.Status.ACCEPTED, result.status());
        assertEquals("Alex", result.playerName());
        assertTrue(admission.isRegistered(1));
        assertEquals("Alex", admission.getName(1));
    }

    @Test
    void normalizesNameBeforeRegistration() {
        PlayerAdmission.Result result =
            admission.register(1, "  Maria  ");

        assertEquals(PlayerAdmission.Status.ACCEPTED, result.status());
        assertEquals("Maria", admission.getName(1));
    }

    @Test
    void rejectsNullAndBlankNames() {
        assertEquals(
            PlayerAdmission.Status.INVALID_NAME,
            admission.register(1, null).status()
        );

        assertEquals(
            PlayerAdmission.Status.INVALID_NAME,
            admission.register(1, "   ").status()
        );

        assertFalse(admission.isRegistered(1));
    }

    @Test
    void rejectsNamesExceedingMaximumLength() {
        String name = "A".repeat(PlayerName.MAX_LENGTH + 1);

        assertEquals(
            PlayerAdmission.Status.INVALID_NAME,
            admission.register(1, name).status()
        );

        assertTrue(admission.isEmpty());
    }

    @Test
    void rejectsInvalidCharacters() {
        PlayerAdmission.Result result =
            admission.register(1, "Alex!");

        assertEquals(PlayerAdmission.Status.INVALID_NAME, result.status());
        assertNull(admission.getName(1));
    }

    @Test
    void rejectsInvalidPlayerIds() {
        assertEquals(
            PlayerAdmission.Status.INVALID_NAME,
            admission.register(0, "Alex").status()
        );

        assertEquals(
            PlayerAdmission.Status.INVALID_NAME,
            admission.register(-1, "Alex").status()
        );

        assertTrue(admission.isEmpty());
    }

    @Test
    void rejectsDuplicateRegistration() {
        admission.register(1, "Alex");

        PlayerAdmission.Result second =
            admission.register(1, "Maria");

        assertEquals(
            PlayerAdmission.Status.ALREADY_REGISTERED,
            second.status()
        );

        assertEquals("Alex", admission.getName(1));
    }

    @Test
    void allowsDifferentPlayersToRegister() {
        admission.register(1, "Alex");
        admission.register(2, "Maria");

        assertEquals("Alex", admission.getName(1));
        assertEquals("Maria", admission.getName(2));
    }

    @Test
    void invalidRequestDoesNotReservePlayerId() {
        admission.register(1, "Invalid!");

        PlayerAdmission.Result result =
            admission.register(1, "Alex");

        assertEquals(PlayerAdmission.Status.ACCEPTED, result.status());
        assertEquals("Alex", admission.getName(1));
    }

    @Test
    void removesPlayerOnDisconnect() {
        admission.register(1, "Alex");

        assertTrue(admission.removePlayer(1));
        assertFalse(admission.isRegistered(1));
        assertNull(admission.getName(1));
        assertTrue(admission.isEmpty());
    }

    @Test
    void allowsRegistrationAfterDisconnect() {
        admission.register(1, "Alex");
        admission.removePlayer(1);

        PlayerAdmission.Result result =
            admission.register(1, "Maria");

        assertEquals(PlayerAdmission.Status.ACCEPTED, result.status());
        assertEquals("Maria", admission.getName(1));
    }

    @Test
    void removingUnknownPlayerDoesNotAffectOthers() {
        admission.register(1, "Alex");

        assertFalse(admission.removePlayer(999));
        assertTrue(admission.isRegistered(1));
    }
}
