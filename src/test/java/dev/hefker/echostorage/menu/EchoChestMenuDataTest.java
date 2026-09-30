package dev.hefker.echostorage.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.block.EchoChestName;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EchoChestMenuDataTest {
	private static final ResourceLocation ECHO_CHEST = ResourceLocation.fromNamespaceAndPath("echostorage", "echo_chest");
	private static final ResourceLocation DEEP_ECHO_CHEST = ResourceLocation.fromNamespaceAndPath("echostorage", "deep_echo_chest");

	@Test
	void openDataSurvivesTheWire() {
		EchoChestMenuData sent = new EchoChestMenuData("Ores", 3, ECHO_CHEST);
		assertEquals(sent, CodecRoundTrip.of(EchoChestMenuData.STREAM_CODEC, sent));
	}

	@Test
	void aBlankChestOpensWithAnEmptyName() {
		EchoChestMenuData sent = new EchoChestMenuData("", 6, DEEP_ECHO_CHEST);
		assertEquals(sent, CodecRoundTrip.of(EchoChestMenuData.STREAM_CODEC, sent));
	}

	@Test
	void aNameTheServerWouldNotHaveAcceptedIsRejectedBeforeItReachesTheWire() {
		assertThrows(IllegalArgumentException.class,
				() -> new EchoChestMenuData("x".repeat(EchoChestName.MAX_LENGTH + 1), 3, ECHO_CHEST));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, 7})
	void aRowCountNoChestScreenCanLayOutIsRejectedBeforeItReachesTheWire(int rows) {
		assertThrows(IllegalArgumentException.class, () -> new EchoChestMenuData("", rows, ECHO_CHEST));
	}

	@Test
	void aRowCountNoChestScreenCanLayOutIsRefusedOffTheWire() {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
		buf.writeUtf("");
		buf.writeVarInt(7);
		ResourceLocation.STREAM_CODEC.encode(buf, ECHO_CHEST);

		assertThrows(RuntimeException.class, () -> EchoChestMenuData.STREAM_CODEC.decode(buf));
	}
}
