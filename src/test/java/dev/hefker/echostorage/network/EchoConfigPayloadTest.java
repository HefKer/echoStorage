package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.config.EchoConfig;
import org.junit.jupiter.api.Test;

class EchoConfigPayloadTest {
	@Test
	void everySwitchSurvivesTheWire() {
		EchoConfigPayload sent = new EchoConfigPayload(new EchoConfig(true, false, false, false, false, false));
		assertEquals(sent, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, sent));
	}

	@Test
	void payloadTypeIsNamespacedToTheMod() {
		assertEquals("echostorage:echo_config", EchoConfigPayload.TYPE.id().toString());
	}
}
