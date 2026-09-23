package dev.hefker.echostorage.platform.fabric;

import dev.hefker.echostorage.platform.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Fabric implementation of {@link PlatformHelper}. This class and the
 * {@code META-INF/services} file next to it are the whole loader-specific surface of the
 * platform seam — a NeoForge port replaces both and touches nothing above.
 */
public final class FabricPlatformHelper implements PlatformHelper {
	@Override
	public String platformName() {
		return "Fabric";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
}
