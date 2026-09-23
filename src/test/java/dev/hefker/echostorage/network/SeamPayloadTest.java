package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hefker.echostorage.CodecRoundTrip;
import org.junit.jupiter.api.Test;

class SeamPayloadTest {
	@Test
	void pingSurvivesTheWire() {
		SeamPingPayload sent = new SeamPingPayload(1234567890L);
		assertEquals(sent, CodecRoundTrip.of(SeamPingPayload.STREAM_CODEC, sent));
	}

	@Test
	void pongSurvivesTheWire() {
		SeamPongPayload sent = new SeamPongPayload(1234567890L, "Fabric");
		assertEquals(sent, CodecRoundTrip.of(SeamPongPayload.STREAM_CODEC, sent));
	}

	@Test
	void payloadTypesAreNamespacedToTheMod() {
		assertEquals("echostorage:seam_ping", SeamPingPayload.TYPE.id().toString());
		assertEquals("echostorage:seam_pong", SeamPongPayload.TYPE.id().toString());
	}
}
