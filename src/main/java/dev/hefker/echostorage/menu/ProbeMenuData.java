package dev.hefker.echostorage.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Open-data for {@link ProbeMenu}: what the server tells the client at the moment the screen
 * opens, and the reference shape for every menu the mod adds (ADR-0003 rule 4).
 *
 * <p>A record and a {@link StreamCodec} — never ad-hoc {@code buf.writeUtf} calls. Fabric's
 * typed {@code ExtendedScreenHandlerType} and NeoForge's raw {@code FriendlyByteBuf} model
 * both accept this shape, which is what keeps the port a five-line adapter in
 * {@link EchoMenus} rather than a rewrite of every screen.
 *
 * @param label the name the player gave this container
 * @param rows how tall it is, in rows of nine
 */
public record ProbeMenuData(String label, int rows) {
	public static final int SLOTS_PER_ROW = 9;
	private static final int MAX_ROWS = 6;

	public static final StreamCodec<RegistryFriendlyByteBuf, ProbeMenuData> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, ProbeMenuData::label,
					ByteBufCodecs.VAR_INT, ProbeMenuData::rows,
					ProbeMenuData::new);

	public ProbeMenuData {
		if (rows < 1 || rows > MAX_ROWS) {
			throw new IllegalArgumentException("rows must be between 1 and " + MAX_ROWS + ", got " + rows);
		}
	}

	public int slotCount() {
		return rows * SLOTS_PER_ROW;
	}
}
