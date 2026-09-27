package dev.hefker.echostorage.gametest;

import dev.hefker.echostorage.config.EchoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Starts every game-test run from {@link EchoConfig#DEFAULTS}, whatever the run's own
 * {@code echostorage.toml} says, so a test that relies on a switch being at its default can't
 * fail because someone edited that file. A test that needs a switch changed uses
 * {@link EchoChestTests#withConfig}.
 *
 * <p>The reset waits for the server to start: the main mod loads the config file in its own
 * initializer, and initializers of different mods run in no guaranteed order.
 */
public class GameTestDefaults implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> EchoConfig.set(EchoConfig.DEFAULTS));
	}
}
