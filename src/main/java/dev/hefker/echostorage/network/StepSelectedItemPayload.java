package dev.hefker.echostorage.network;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player sneak-scrolled with an Echo Bundle in the main hand.
 *
 * <p>An intent, not an instruction. It names no bundle and no item: the server steps the
 * Selected item of whatever Echo Bundle is in the player's main hand when it arrives.
 *
 * @param steps how many stops to move, forward for positive; one per scroll
 */
public record StepSelectedItemPayload(int steps) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StepSelectedItemPayload> TYPE =
			new CustomPacketPayload.Type<>(EchoStorage.id("step_selected_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, StepSelectedItemPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, StepSelectedItemPayload::steps,
					StepSelectedItemPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
