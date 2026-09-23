package dev.hefker.echostorage.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.block.EchoChestName;
import org.junit.jupiter.api.Test;

class EchoChestMenuDataTest {
	@Test
	void openDataSurvivesTheWire() {
		EchoChestMenuData sent = new EchoChestMenuData("Ores");
		assertEquals(sent, CodecRoundTrip.of(EchoChestMenuData.STREAM_CODEC, sent));
	}

	@Test
	void aBlankChestOpensWithAnEmptyName() {
		EchoChestMenuData sent = new EchoChestMenuData("");
		assertEquals(sent, CodecRoundTrip.of(EchoChestMenuData.STREAM_CODEC, sent));
	}

	@Test
	void aNameTheServerWouldNotHaveAcceptedIsRejectedBeforeItReachesTheWire() {
		assertThrows(IllegalArgumentException.class,
				() -> new EchoChestMenuData("x".repeat(EchoChestName.MAX_LENGTH + 1)));
	}
}
