package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.config.EchoConfig;
import org.junit.jupiter.api.Test;

class EchoConfigPayloadTest {
	@Test
	void everySwitchSurvivesTheWire() {
		EchoConfigPayload sent = new EchoConfigPayload(new EchoConfig(true, false, false, false, false));
		assertEquals(sent, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, sent));
	}

	@Test
	void eachSwitchKeepsItsOwnPlaceOnTheWire() {
		EchoConfigPayload odd = new EchoConfigPayload(new EchoConfig(true, false, true, false, true));
		EchoConfigPayload even = new EchoConfigPayload(new EchoConfig(false, true, false, true, false));
		assertEquals(odd, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, odd));
		assertEquals(even, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, even));
	}

	@Test
	void payloadTypeIsNamespacedToTheMod() {
		assertEquals("echostorage:echo_config", EchoConfigPayload.TYPE.id().toString());
	}
}
