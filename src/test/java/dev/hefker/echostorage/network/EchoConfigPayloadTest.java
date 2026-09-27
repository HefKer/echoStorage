package dev.hefker.echostorage.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		for (int on = 0; on < ConfigSwitch.ALL.size(); on++) {
			List<Boolean> values = new ArrayList<>(Collections.nCopies(ConfigSwitch.ALL.size(), false));
			values.set(on, true);
			EchoConfigPayload sent = new EchoConfigPayload(ConfigSwitch.build(values));

			assertEquals(sent, CodecRoundTrip.of(EchoConfigPayload.STREAM_CODEC, sent),
					"only " + ConfigSwitch.ALL.get(on).path() + " on");
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
