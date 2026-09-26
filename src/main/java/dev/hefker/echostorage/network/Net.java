package dev.hefker.echostorage.network;

import dev.hefker.echostorage.config.EchoConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
		PayloadTypeRegistry.playS2C().register(EchoInterfaceRowsPayload.TYPE, EchoInterfaceRowsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(OpenLinkedChestPayload.TYPE, OpenLinkedChestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(DismissLinkedChestPayload.TYPE, DismissLinkedChestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(EchoConfigPayload.TYPE, EchoConfigPayload.STREAM_CODEC);
	}

	/** Wires the server-bound handlers. Payload types must already be registered. */
	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(SeamPongPayload.TYPE,
				(payload, context) -> SeamProbe.onPong(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(RenameEchoChestPayload.TYPE,
				(payload, context) -> EchoChestRenames.onRename(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(OpenLinkedChestPayload.TYPE,
				(payload, context) -> EchoInterfaces.onOpen(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(DismissLinkedChestPayload.TYPE,
				(payload, context) -> EchoInterfaces.onDismiss(context.player(), payload));
	}

	/**
	 * Sends every joining player the server's config. Always, even to a singleplayer host,
	 * whose client already holds the same values.
	 */
	public static void registerConnectionEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				sendTo(handler.getPlayer(), new EchoConfigPayload(EchoConfig.get())));
	}

	/** Sends a payload to one player. The only server-to-client send in the mod. */
	public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}
}
