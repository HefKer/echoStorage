package dev.hefker.echostorage.network;

import dev.hefker.echostorage.menu.EchoInterfaceMenu;
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
	}

	/** Sends a payload to the server. The only client-to-server send in the mod. */
	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
