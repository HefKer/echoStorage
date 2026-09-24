package dev.hefker.echostorage.item;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.block.EchoChestAssignment;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mod's data components. Built eagerly but registered only from {@link #register()}, so
 * tests can put them on stacks without touching a frozen registry.
 */
public final class EchoComponents {
	public static final DataComponentType<EchoBundleContents> ECHO_BUNDLE_CONTENTS =
			DataComponentType.<EchoBundleContents>builder()
					.persistent(EchoBundleContents.CODEC)
					.networkSynchronized(EchoBundleContents.STREAM_CODEC)
					.cacheEncoding()
					.build();

	/** Absent on a bundle nobody has set anything on; read it through {@link EchoBundleItem#settingsOf}. */
	public static final DataComponentType<EchoBundleSettings> ECHO_BUNDLE_SETTINGS =
			DataComponentType.<EchoBundleSettings>builder()
					.persistent(EchoBundleSettings.CODEC)
					.networkSynchronized(EchoBundleSettings.STREAM_CODEC)
					.build();

	/** Absent on an Echo Chest item with nothing set, so a blank chest stacks with a crafted one (ADR-0008). */
	public static final DataComponentType<EchoChestAssignment> ECHO_CHEST_ASSIGNMENT =
			DataComponentType.<EchoChestAssignment>builder()
					.persistent(EchoChestAssignment.CODEC)
					.networkSynchronized(EchoChestAssignment.STREAM_CODEC)
					.build();

	private EchoComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_bundle_contents"), ECHO_BUNDLE_CONTENTS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_bundle_settings"), ECHO_BUNDLE_SETTINGS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_chest_assignment"), ECHO_CHEST_ASSIGNMENT);
	}
}
