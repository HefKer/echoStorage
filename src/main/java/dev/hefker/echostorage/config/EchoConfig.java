package dev.hefker.echostorage.config;

/**
 * World-preference switches: what a pack author may turn off for everyone. Per-object intent —
 * a chest's strictness, a bundle's or box's Vacuum toggle — is never here; it lives on the object. Nor
 * is the Echo Chest's slot count: container size is world data, and shrinking it would strand
 * whatever sat in the removed slots.
 *
 * <p>{@link EchoConfigFile} loads it at startup; everything reads {@link #get()}. On a client
 * connected to a server, {@link #get()} answers with the server's switches (see
 * {@link SessionConfig}), so a switch read on the client follows the server too.
 *
 * @param searchInOpenContainer whether an open container's screen offers a search box
 * @param searchByChestName     whether an Echo Interface can search its chests by name
 * @param vacuumEnabled         whether any carried Echo Bundle or Echo Shulker Box may Vacuum on pickup;
 *                              each one still starts with its own toggle off
 * @param bundleRefill          whether placing the last block in hand pulls the next from a bundle
 * @param bundlePlace           whether using an Echo Bundle on a block places a block out of it
 * @param chestQuickStack       whether an Echo Chest's screen offers Quick-stack into that chest
 * @param interfaceQuickStack   whether an Echo Interface offers Quick-stack into every linked chest;
 *                              off by default, so a pack opts in (ADR-0010)
 */
public record EchoConfig(
		boolean searchInOpenContainer,
		boolean searchByChestName,
		boolean vacuumEnabled,
		boolean bundleRefill,
		boolean bundlePlace,
		boolean chestQuickStack,
		boolean interfaceQuickStack) {
	public static final EchoConfig DEFAULTS = new EchoConfig(true, true, true, true, true, true, false);

	private static volatile EchoConfig current = DEFAULTS;

	public static EchoConfig get() {
		return current;
	}

	public static void set(EchoConfig config) {
		current = config;
	}

	public EchoConfig withVacuumEnabled(boolean vacuumEnabled) {
		return new EchoConfig(searchInOpenContainer, searchByChestName, vacuumEnabled, bundleRefill, bundlePlace,
				chestQuickStack, interfaceQuickStack);
	}

	public EchoConfig withBundleRefill(boolean bundleRefill) {
		return new EchoConfig(searchInOpenContainer, searchByChestName, vacuumEnabled, bundleRefill, bundlePlace,
				chestQuickStack, interfaceQuickStack);
	}

	public EchoConfig withBundlePlace(boolean bundlePlace) {
		return new EchoConfig(searchInOpenContainer, searchByChestName, vacuumEnabled, bundleRefill, bundlePlace,
				chestQuickStack, interfaceQuickStack);
	}

	public EchoConfig withChestQuickStack(boolean chestQuickStack) {
		return new EchoConfig(searchInOpenContainer, searchByChestName, vacuumEnabled, bundleRefill, bundlePlace,
				chestQuickStack, interfaceQuickStack);
	}

	public EchoConfig withInterfaceQuickStack(boolean interfaceQuickStack) {
		return new EchoConfig(searchInOpenContainer, searchByChestName, vacuumEnabled, bundleRefill, bundlePlace,
				chestQuickStack, interfaceQuickStack);
	}
}
