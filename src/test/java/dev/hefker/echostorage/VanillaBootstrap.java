package dev.hefker.echostorage;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Brings up vanilla's registries so tests can make real {@code ItemStack}s. Idempotent. */
public final class VanillaBootstrap {
	private VanillaBootstrap() {
	}

	public static void run() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
}
