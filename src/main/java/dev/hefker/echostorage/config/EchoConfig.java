package dev.hefker.echostorage.config;

/**
 * World-preference switches: what a pack author may turn off for everyone. Per-object intent —
 * a chest's strictness, a bundle's vacuum toggle — is never here; it lives on the object. Nor
 * is the Echo Chest's slot count: container size is world data, and shrinking it would strand
 * whatever sat in the removed slots.
 *
 * <p>{@link EchoConfigFile} loads it at startup; everything reads {@link #get()}.
 *
 * @param wirelessLinking       whether an Echo Interface reaches Echo Chests without a sculk
 *                              Link. Only that; see ADR-0004
 * @param searchInOpenContainer whether an open container's screen offers a search box
 * @param searchByChestName     whether an Echo Interface can search its chests by name
 * @param bundleVacuum          whether any Echo Bundle may vacuum on pickup; each bundle still starts off
 * @param bundleRefill          whether placing the last block in hand pulls the next from a bundle
 * @param bundlePlace           whether using an Echo Bundle on a block places a block out of it
 */
public record EchoConfig(
		boolean wirelessLinking,
		boolean searchInOpenContainer,
		boolean searchByChestName,
		boolean bundleVacuum,
		boolean bundleRefill,
		boolean bundlePlace) {
	public static final EchoConfig DEFAULTS = new EchoConfig(false, true, true, true, true, true);

	private static volatile EchoConfig current = DEFAULTS;

	public static EchoConfig get() {
		return current;
	}

	public static void set(EchoConfig config) {
		current = config;
	}

	public EchoConfig withBundleVacuum(boolean bundleVacuum) {
		return new EchoConfig(wirelessLinking, searchInOpenContainer, searchByChestName, bundleVacuum, bundleRefill, bundlePlace);
	}

	public EchoConfig withBundleRefill(boolean bundleRefill) {
		return new EchoConfig(wirelessLinking, searchInOpenContainer, searchByChestName, bundleVacuum, bundleRefill, bundlePlace);
	}

	public EchoConfig withBundlePlace(boolean bundlePlace) {
		return new EchoConfig(wirelessLinking, searchInOpenContainer, searchByChestName, bundleVacuum, bundleRefill, bundlePlace);
	}
}
