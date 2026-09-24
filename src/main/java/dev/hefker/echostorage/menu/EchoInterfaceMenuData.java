package dev.hefker.echostorage.menu;

import java.util.List;

import dev.hefker.echostorage.link.LinkedChests;
import dev.hefker.echostorage.link.LinkedChests.Row;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Open-data for {@link EchoInterfaceMenu} (ADR-0003 rule 4): the rows as the interface resolved
 * them for this open, so the screen is never empty on its first frame.
 */
public record EchoInterfaceMenuData(List<Row> rows) {
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoInterfaceMenuData> STREAM_CODEC =
			StreamCodec.composite(
					Row.STREAM_CODEC.apply(ByteBufCodecs.list(LinkedChests.MAX_ROWS)), EchoInterfaceMenuData::rows,
					EchoInterfaceMenuData::new);

	public EchoInterfaceMenuData {
		rows = List.copyOf(rows);
	}
}
