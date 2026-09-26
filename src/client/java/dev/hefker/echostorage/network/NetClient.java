package dev.hefker.echostorage.network;

import dev.hefker.echostorage.config.SessionConfig;
import dev.hefker.echostorage.menu.EchoInterfaceMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
	 * Wires the client-bound handlers. Fabric calls these on the render thread, so they may
	 * touch client state directly — do not defer them, or every timing they report is really
	 * a tick boundary.
	 */
	public static void registerClientReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(SeamPingPayload.TYPE,
				(payload, context) -> ClientSeamProbe.onPing(payload));
		ClientPlayNetworking.registerGlobalReceiver(EchoInterfaceRowsPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof EchoInterfaceMenu menu && menu.containerId == payload.containerId()) {
				menu.setRows(payload.rows());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(EchoConfigPayload.TYPE,
				(payload, context) -> SessionConfig.applyRemote(payload.config()));
	}

	/** Puts the client's own config back on leaving a server, so the next world starts clean. */
	public static void registerConnectionEvents() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SessionConfig.restoreLocal());
	}

	/** Sends a payload to the server. The only client-to-server send in the mod. */
	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
