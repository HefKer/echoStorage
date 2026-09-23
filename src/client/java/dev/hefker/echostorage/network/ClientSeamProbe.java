package dev.hefker.echostorage.network;

import dev.hefker.echostorage.platform.Services;

/** Client half of {@link SeamProbe}: answer the ping, naming the loader we are running on. */
public final class ClientSeamProbe {
	private ClientSeamProbe() {
	}

	static void onPing(SeamPingPayload ping) {
		NetClient.sendToServer(new SeamPongPayload(ping.nonce(), Services.PLATFORM.platformName()));
	}
}
