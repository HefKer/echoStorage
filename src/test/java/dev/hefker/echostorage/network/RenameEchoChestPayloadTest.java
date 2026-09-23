package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.block.EchoChestName;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.Test;

class RenameEchoChestPayloadTest {
	@Test
	void aRenameSurvivesTheWire() {
		RenameEchoChestPayload sent = new RenameEchoChestPayload(7, "Ores");
		assertEquals(sent, CodecRoundTrip.of(RenameEchoChestPayload.STREAM_CODEC, sent));
	}

	@Test
	void clearingTheNameIsAnEmptyString() {
		RenameEchoChestPayload sent = new RenameEchoChestPayload(7, "");
		assertEquals(sent, CodecRoundTrip.of(RenameEchoChestPayload.STREAM_CODEC, sent));
	}

	@Test
	void theWireCarriesNoMoreThanANameCanHold() {
		RenameEchoChestPayload tooLong = new RenameEchoChestPayload(7, "x".repeat(EchoChestName.MAX_LENGTH + 1));
		assertThrows(EncoderException.class, () -> CodecRoundTrip.of(RenameEchoChestPayload.STREAM_CODEC, tooLong));
	}

	@Test
	void payloadTypeIsNamespacedToTheMod() {
		assertEquals("echostorage:rename_echo_chest", RenameEchoChestPayload.TYPE.id().toString());
	}
}
