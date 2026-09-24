package dev.hefker.echostorage.network;

import java.util.UUID;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player picked a row on the Echo Interface screen they have open, to
 * open that chest.
 *
 * <p>Names the chest by id, not by its place in the list, so a list that changed under the
 * click cannot turn it into a click on another chest.
 *
 * @param containerId the interface menu clicked in; stale ids are ignored
 * @param chest       the chest's id
 */
public record OpenLinkedChestPayload(int containerId, UUID chest) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenLinkedChestPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("open_linked_chest"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenLinkedChestPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, OpenLinkedChestPayload::containerId,
					UUIDUtil.STREAM_CODEC, OpenLinkedChestPayload::chest,
					OpenLinkedChestPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
