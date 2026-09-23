package dev.hefker.echostorage.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The client half of the {@link Net} facade (ADR-0003 rule 3). Same package, client source
 * set — so the "only the network package touches Fabric networking" rule still holds.
 */
public final class NetClient {
	private NetClient() {
	}

	/**
	 * Wires the client-bound handlers. Handlers run on the netty thread, so anything touching
	 * game state is scheduled onto the client thread here rather than in the handler.
	 */
	public static void registerClientReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(SeamPingPayload.TYPE,
				(payload, context) -> context.client().execute(() -> ClientSeamProbe.onPing(payload)));
	}

	/** Sends a payload to the server. The only client-to-server send in the mod. */
	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
