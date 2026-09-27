package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.config.ConfigSwitch;
import dev.hefker.echostorage.config.EchoConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class EchoConfigPayloadTest {
	@Test
	void everySwitchSurvivesTheWire() {
		EchoConfigPayload sent = new EchoConfigPayload(new EchoConfig(true, false, false, false, false, false, false));
		assertEquals(sent, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, sent));
	}

	@Test
	void eachSwitchKeepsItsOwnPlaceOnTheWire() {
		for (ConfigSwitch on : ConfigSwitch.ALL) {
			EchoConfigPayload sent = new EchoConfigPayload(ConfigSwitch.build(option -> option == on));

			assertEquals(sent, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, sent), "only " + on.path() + " on");
		}
	}

	@Test
	void theWireCarriesOneBooleanPerSwitch() {
		ByteBuf buf = Unpooled.buffer();
		EchoConfigPayload.STREAM_CODEC.encode(buf, new EchoConfigPayload(EchoConfig.DEFAULTS));
		assertEquals(ConfigSwitch.ALL.size(), buf.readableBytes());
	}

	@Test
	void payloadTypeIsNamespacedToTheMod() {
		assertEquals("echostorage:echo_config", EchoConfigPayload.TYPE.id().toString());
	}
}
