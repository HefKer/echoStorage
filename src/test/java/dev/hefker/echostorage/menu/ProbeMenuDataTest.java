package dev.hefker.echostorage.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hefker.echostorage.CodecRoundTrip;
import org.junit.jupiter.api.Test;

class ProbeMenuDataTest {
	@Test
	void openDataSurvivesTheWire() {
		ProbeMenuData sent = new ProbeMenuData("Echo Chest", 3);
		assertEquals(sent, CodecRoundTrip.of(ProbeMenuData.STREAM_CODEC, sent));
	}

	@Test
	void rowsOutsideAChestsRangeAreRejectedBeforeTheyReachTheWire() {
		assertThrows(IllegalArgumentException.class, () -> new ProbeMenuData("too tall", 7));
		assertThrows(IllegalArgumentException.class, () -> new ProbeMenuData("too short", 0));
	}

	@Test
	void slotCountFollowsFromRows() {
		assertEquals(27, new ProbeMenuData("three rows", 3).slotCount());
	}
}
