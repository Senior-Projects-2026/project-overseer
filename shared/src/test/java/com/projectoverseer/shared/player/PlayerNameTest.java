
package com.projectoverseer.shared.player;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerNameTest {

    @Test
    public void nullNameIsNormalizedToEmptyString() {
        assertEquals("", PlayerName.normalize(null));
    }

    @Test
    public void leadingAndTrailingWhitespaceIsRemoved() {
        assertEquals(
            "Alex_21",
            PlayerName.normalize("  Alex_21  ")
        );
    }

    @Test
    public void emptyAndWhitespaceOnlyNamesAreRejected() {
        assertFalse(PlayerName.isValid(null));
        assertFalse(PlayerName.isValid(""));
        assertFalse(PlayerName.isValid("   "));
        assertFalse(PlayerName.isValid("\t\n"));
    }

    @Test
    public void validNamesAreAccepted() {
        assertTrue(PlayerName.isValid("Alex"));
        assertTrue(PlayerName.isValid("Player123"));
        assertTrue(PlayerName.isValid("Alex_21"));
        assertTrue(PlayerName.isValid("Alex-21"));
    }

    @Test
    public void unicodeLettersAreAccepted() {
        assertTrue(PlayerName.isValid("Ștefan"));
        assertTrue(PlayerName.isValid("Măriuța"));
        assertTrue(PlayerName.isValid("Țepeș"));
    }

    @Test
    public void nameAtMaximumLengthIsAccepted() {
        String name = "A".repeat(PlayerName.MAX_LENGTH);

        assertTrue(PlayerName.isValid(name));
    }

    @Test
    public void nameExceedingMaximumLengthIsRejected() {
        String name = "A".repeat(PlayerName.MAX_LENGTH + 1);

        assertFalse(PlayerName.isValid(name));
    }

    @Test
    public void invalidCharactersAreRejected() {
        assertFalse(PlayerName.isValid("Alex!"));
        assertFalse(PlayerName.isValid("Alex@123"));
        assertFalse(PlayerName.isValid("Alex.Test"));
        assertFalse(PlayerName.isValid("Alex/123"));
    }

    @Test
    public void internalWhitespaceIsRejected() {
        assertFalse(PlayerName.isValid("Alex Smith"));
        assertFalse(PlayerName.isValid("Alex\tSmith"));
        assertFalse(PlayerName.isValid("Alex\nSmith"));
    }

    @Test
    public void surroundingWhitespaceIsAcceptedAfterNormalization() {
        assertTrue(PlayerName.isValid("  Alex  "));
    }

    @Test
    public void allowedCharacterValidationIsConsistent() {
        assertTrue(PlayerName.isAllowedCharacter('A'));
        assertTrue(PlayerName.isAllowedCharacter('7'));
        assertTrue(PlayerName.isAllowedCharacter('_'));
        assertTrue(PlayerName.isAllowedCharacter('-'));
        assertTrue(PlayerName.isAllowedCharacter('ă'));

        assertFalse(PlayerName.isAllowedCharacter(' '));
        assertFalse(PlayerName.isAllowedCharacter('!'));
        assertFalse(PlayerName.isAllowedCharacter('/'));
    }
}
