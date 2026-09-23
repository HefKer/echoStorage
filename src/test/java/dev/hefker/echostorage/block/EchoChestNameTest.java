package dev.hefker.echostorage.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A rename arrives from the client as raw text, so the server decides what it becomes. The
 * rules follow the anvil's, because a typed name is a vanilla custom name once it lands.
 */
class EchoChestNameTest {
	@Test
	void anOrdinaryNameIsKeptAsTyped() {
		assertEquals("Ores", EchoChestName.sanitize("Ores"));
	}

	@Test
	void surroundingWhitespaceIsTrimmed() {
		assertEquals("Ores 2", EchoChestName.sanitize("  Ores 2 "));
	}

	@Test
	void aBlankNameClearsTheName() {
		assertEquals("", EchoChestName.sanitize("   "));
		assertEquals("", EchoChestName.sanitize(""));
	}

	@Test
	void formattingCannotBeTypedInAndControlCharactersAreStripped() {
		// As in the anvil, only the section sign goes; the code letter after it is plain text.
		assertEquals("cOres", EchoChestName.sanitize("§cOr\u0007es\u007f"));
	}

	@Test
	void aNameLongerThanTheAnvilAllowsIsCutToFit() {
		String typed = "x".repeat(EchoChestName.MAX_LENGTH + 10);
		assertEquals("x".repeat(EchoChestName.MAX_LENGTH), EchoChestName.sanitize(typed));
	}

	@Test
	void cuttingToFitDoesNotLeaveTrailingWhitespace() {
		String typed = "x".repeat(EchoChestName.MAX_LENGTH - 1) + "  tail";
		assertEquals("x".repeat(EchoChestName.MAX_LENGTH - 1), EchoChestName.sanitize(typed));
	}
}
