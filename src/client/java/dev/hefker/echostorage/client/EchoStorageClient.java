package dev.hefker.echostorage.client;

import dev.hefker.echostorage.client.render.EchoRenderers;
import dev.hefker.echostorage.client.screen.EchoScreens;
import dev.hefker.echostorage.client.tooltip.EchoTooltips;
import dev.hefker.echostorage.network.NetClient;
import net.fabricmc.api.ClientModInitializer;

public class EchoStorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NetClient.registerClientReceivers();
		EchoScreens.register();
		EchoTooltips.register();
		EchoItemProperties.register();
		EchoRenderers.register();
	}
}
