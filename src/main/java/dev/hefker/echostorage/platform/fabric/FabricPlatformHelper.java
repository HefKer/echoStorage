package dev.hefker.echostorage.platform.fabric;

import dev.hefker.echostorage.platform.PlatformHelper;

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
}
