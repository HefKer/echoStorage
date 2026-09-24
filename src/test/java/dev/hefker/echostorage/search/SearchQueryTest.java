package dev.hefker.echostorage.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * What a typed search matches. The same query runs over the item names in an open container
 * and over the Echo Chest names in an Echo Interface, so both behave alike.
 */
class SearchQueryTest {
	@Test
	void aNameContainingTheQueryMatches() {
		assertTrue(SearchQuery.of("ore").matches("Iron Ore"));
		assertTrue(SearchQuery.of("ore").matches("Ores 2"));
	}

	@Test
	void aNameWithoutTheQueryDoesNotMatch() {
		assertFalse(SearchQuery.of("ore").matches("Cobblestone"));
	}

	@Test
	void caseIsIgnoredOnBothSides() {
		assertTrue(SearchQuery.of("ORE").matches("iron ore"));
		assertTrue(SearchQuery.of("ore").matches("IRON ORE"));
	}

	@Test
	void surroundingWhitespaceInTheQueryIsIgnored() {
		assertTrue(SearchQuery.of("  ore ").matches("Iron Ore"));
	}

	@Test
	void aBlankQueryIsNotASearch() {
		assertTrue(SearchQuery.of("   ").isBlank());
		assertFalse(SearchQuery.of("ore").isBlank());
	}
}
