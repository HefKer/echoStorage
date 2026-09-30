package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.block.EchoChestName;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Open-data for {@link EchoChestMenu}, shaped like {@link ProbeMenuData} (ADR-0003 rule 4).
 *
 * <p>The screen title already says what the chest is called; this carries the name as the
 * player typed it, so the label area opens holding editable text rather than a display name.
 * It also says how many rows the chest has, since one menu type serves every size, and which
 * kind of chest it is, which names the chest while its name field is empty.
 *
 * @param name the chest's name, empty if it has none
 * @param rows the chest's rows of nine slots, as many as vanilla's chest texture draws at most
 * @param kind the registry id of the chest's item
 */
public record EchoChestMenuData(String name, int rows, ResourceLocation kind) {
	/** The most rows vanilla's {@code generic_54} texture has. */
	public static final int MAX_ROWS = 6;

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChestMenuData> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.stringUtf8(EchoChestName.MAX_LENGTH), EchoChestMenuData::name,
					ByteBufCodecs.VAR_INT, EchoChestMenuData::rows,
					ResourceLocation.STREAM_CODEC, EchoChestMenuData::kind,
					EchoChestMenuData::new);

	public EchoChestMenuData {
		if (name.length() > EchoChestName.MAX_LENGTH) {
			throw new IllegalArgumentException("name is longer than " + EchoChestName.MAX_LENGTH + " characters");
		}
		if (rows < 1 || rows > MAX_ROWS) {
			throw new IllegalArgumentException("a chest screen lays out 1 to " + MAX_ROWS + " rows, not " + rows);
		}
	}
}
