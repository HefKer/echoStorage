package dev.hefker.echostorage.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Vanilla's bundle tooltip grows a cell per entry. Four bundles' worth can be 256 entries,
 * which vanilla would draw about 306x322px, so ours stops growing at vanilla's own worst case.
 */
class EchoBundleTooltipLayoutTest {
	@Test
	void anEmptyBundleLooksLikeVanillas() {
		assertEquals(new EchoBundleTooltipLayout(2, 1, 0, 0), EchoBundleTooltipLayout.forEntries(0));
	}

	@Test
	void aFewEntriesLookLikeVanillas() {
		assertEquals(new EchoBundleTooltipLayout(2, 2, 3, 0), EchoBundleTooltipLayout.forEntries(3));
	}

	@Test
	void vanillasLargestGridStillShowsEverything() {
		assertEquals(new EchoBundleTooltipLayout(9, 8, 64, 0), EchoBundleTooltipLayout.forEntries(64));
	}

	@Test
	void pastVanillasLargestGridTheLastCellCountsWhatIsHidden() {
		assertEquals(new EchoBundleTooltipLayout(8, 8, 63, 2), EchoBundleTooltipLayout.forEntries(65));
	}

	@Test
	void aBundleOfTwoHundredFiftySixDifferentThingsStaysOnScreen() {
		assertEquals(new EchoBundleTooltipLayout(8, 8, 63, 193), EchoBundleTooltipLayout.forEntries(256));
	}
}
