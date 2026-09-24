package dev.hefker.echostorage;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.command.SeamCommand;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.config.EchoConfigFile;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoMenus;
import dev.hefker.echostorage.network.Net;
import dev.hefker.echostorage.platform.Services;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoStorage implements ModInitializer {
	public static final String MOD_ID = "echostorage";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Echo Storage starting on {}", Services.PLATFORM.platformName());

		EchoConfig.set(EchoConfigFile.load(Services.PLATFORM.configDir().resolve(MOD_ID + ".toml")));

		Net.registerPayloads();
		Net.registerServerReceivers();
		EchoComponents.register();
		EchoBlocks.register();
		EchoItems.register();
		EchoMenus.register();
		SeamCommand.register();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
