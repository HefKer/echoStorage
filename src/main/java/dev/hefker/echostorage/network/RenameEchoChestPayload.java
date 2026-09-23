package dev.hefker.echostorage.network;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.block.EchoChestName;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player typed a new name into the Echo Chest screen they have open.
 *
 * <p>An intent, not an instruction. It names the menu rather than a block position, so the
 * server renames only the chest this player actually has open, and it sends the text as typed
 * — {@link EchoChestName} decides what it becomes.
 *
 * @param containerId the menu the player typed into; stale ids are ignored
 * @param name the typed text, empty to clear the name
 */
public record RenameEchoChestPayload(int containerId, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RenameEchoChestPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("rename_echo_chest"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RenameEchoChestPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, RenameEchoChestPayload::containerId,
					ByteBufCodecs.stringUtf8(EchoChestName.MAX_LENGTH), RenameEchoChestPayload::name,
					RenameEchoChestPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
