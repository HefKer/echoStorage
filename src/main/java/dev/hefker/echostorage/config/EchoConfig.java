package dev.hefker.echostorage.config;

/**
 * World-preference switches: what a pack author may turn off for everyone. Per-object intent —
 * a chest's strictness, a bundle's vacuum toggle — is never here; it lives on the object.
 *
 * <p>Held in code until the config file (#9) loads it; everything reads {@link #get()}, so the
 * file only has to {@link #set} what it read.
 *
 * @param bundleVacuum whether any Echo Bundle may vacuum on pickup; each bundle still starts off
 * @param bundleRefill whether placing the last block in hand pulls the next from a bundle
 * @param bundlePlace  whether using an Echo Bundle on a block places a block out of it
 */
public record EchoConfig(boolean bundleVacuum, boolean bundleRefill, boolean bundlePlace) {
	public static final EchoConfig DEFAULTS = new EchoConfig(true, true, true);

	private static volatile EchoConfig current = DEFAULTS;

	public static EchoConfig get() {
		return current;
	}

	public static void set(EchoConfig config) {
		current = config;
	}

	public EchoConfig withBundleVacuum(boolean bundleVacuum) {
		return new EchoConfig(bundleVacuum, bundleRefill, bundlePlace);
	}

	public EchoConfig withBundleRefill(boolean bundleRefill) {
		return new EchoConfig(bundleVacuum, bundleRefill, bundlePlace);
	}

	public EchoConfig withBundlePlace(boolean bundlePlace) {
		return new EchoConfig(bundleVacuum, bundleRefill, bundlePlace);
	}
}
