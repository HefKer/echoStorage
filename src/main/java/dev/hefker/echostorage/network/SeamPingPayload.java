package dev.hefker.echostorage.network;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client half of the seam probe: carries a nonce the client is expected to echo.
 *
 * <p>This is the reference shape every payload in the mod follows (ADR-0003 rule 3): a record,
 * a vanilla {@link StreamCodec}, a {@link CustomPacketPayload.Type} — and no knowledge of how
 * it is sent. That lives in {@link Net}.
 */
public record SeamPingPayload(long nonce) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SeamPingPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("seam_ping"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SeamPingPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_LONG, SeamPingPayload::nonce,
					SeamPingPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
