package dev.hefker.echostorage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Encodes a value and reads it back, the way the network layer will. */
public final class CodecRoundTrip {
	private CodecRoundTrip() {
	}

	public static <T> T of(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
		codec.encode(buf, value);
		T decoded = codec.decode(buf);
		assertEquals(0, buf.readableBytes(), "codec left unread bytes in the buffer");
		return decoded;
	}
}
