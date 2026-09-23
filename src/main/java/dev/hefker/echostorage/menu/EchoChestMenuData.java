package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.block.EchoChestName;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Open-data for {@link EchoChestMenu}, shaped like {@link ProbeMenuData} (ADR-0003 rule 4).
 *
 * <p>The screen title already says what the chest is called; this carries the name as the
 * player typed it, so the label area opens holding editable text rather than a display name.
 *
 * @param name the chest's name, empty if it has none
 */
public record EchoChestMenuData(String name) {
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChestMenuData> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.stringUtf8(EchoChestName.MAX_LENGTH), EchoChestMenuData::name,
					EchoChestMenuData::new);

	public EchoChestMenuData {
		if (name.length() > EchoChestName.MAX_LENGTH) {
			throw new IllegalArgumentException("name is longer than " + EchoChestName.MAX_LENGTH + " characters");
		}
	}
}
