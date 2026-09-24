package dev.hefker.echostorage.network;

import java.util.List;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.link.LinkedChests;
import dev.hefker.echostorage.link.LinkedChests.Row;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: an open Echo Interface's rows changed.
 *
 * @param containerId the interface menu the rows belong to; a screen showing another ignores them
 * @param rows        every row, in order
 */
public record EchoInterfaceRowsPayload(int containerId, List<Row> rows) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<EchoInterfaceRowsPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("echo_interface_rows"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoInterfaceRowsPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, EchoInterfaceRowsPayload::containerId,
					Row.STREAM_CODEC.apply(ByteBufCodecs.list(LinkedChests.MAX_ROWS)), EchoInterfaceRowsPayload::rows,
					EchoInterfaceRowsPayload::new);

	public EchoInterfaceRowsPayload {
		rows = List.copyOf(rows);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
