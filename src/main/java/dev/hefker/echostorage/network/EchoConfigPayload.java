package dev.hefker.echostorage.network;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.config.EchoConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, once on join: the server's config, which the client uses for the rest of
 * the session. Without it, switches read on the client — the search boxes, the bundle's place
 * prediction — would follow the client's own file instead of the server's.
 *
 * @param config every switch, as the server has it
 */
public record EchoConfigPayload(EchoConfig config) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EchoConfigPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("echo_config"));

	private static final StreamCodec<ByteBuf, EchoConfig> CONFIG_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, EchoConfig::searchInOpenContainer,
			ByteBufCodecs.BOOL, EchoConfig::searchByChestName,
			ByteBufCodecs.BOOL, EchoConfig::bundleVacuum,
			ByteBufCodecs.BOOL, EchoConfig::bundleRefill,
			ByteBufCodecs.BOOL, EchoConfig::bundlePlace,
			EchoConfig::new);

	public static final StreamCodec<ByteBuf, EchoConfigPayload> STREAM_CODEC =
			CONFIG_CODEC.map(EchoConfigPayload::new, EchoConfigPayload::config);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
