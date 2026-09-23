package dev.hefker.echostorage.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's whole networking surface (ADR-0003 rule 3).
 *
 * <p>Every payload is registered here, every server-bound handler is wired here, and every
 * server-to-client send goes through {@link #sendTo}. {@code NetClient}, in the client source
 * set, is the same facade's client half. No other class in the mod may import a Fabric
 * networking API — {@code PortabilityRulesTest} fails the build if one does.
 *
 * <p>A NeoForge port rewrites this class against {@code PayloadRegistrar} and leaves the
 * payload records and their handlers untouched.
 */
public final class Net {
	private Net() {
	}

	/**
	 * Registers payload types on both logical sides. Must run in common init, before any
	 * connection is established, and in the same order on client and server.
	 */
	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(SeamPingPayload.TYPE, SeamPingPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(SeamPongPayload.TYPE, SeamPongPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(RenameEchoChestPayload.TYPE, RenameEchoChestPayload.STREAM_CODEC);
	}

	/** Wires the server-bound handlers. Payload types must already be registered. */
	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SeamPongPayload.TYPE,
				(payload, context) -> SeamProbe.onPong(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(RenameEchoChestPayload.TYPE,
				(payload, context) -> EchoChestRenames.onRename(context.player(), payload));
	}

	/** Sends a payload to one player. The only server-to-client send in the mod. */
	public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
