package dev.hefker.echostorage.network;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server half of the seam probe: echoes the nonce and reports the loader the client
 * is running on, as {@code PlatformHelper} names it.
 */
public record SeamPongPayload(long nonce, String platformName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SeamPongPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("seam_pong"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SeamPongPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_LONG, SeamPongPayload::nonce,
					ByteBufCodecs.STRING_UTF8, SeamPongPayload::platformName,
					SeamPongPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
