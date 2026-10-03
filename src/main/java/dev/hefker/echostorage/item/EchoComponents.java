package dev.hefker.echostorage.item;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.block.EchoChestAssignment;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

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

	/**
	 * Absent on an empty bundle and on one from before the Selected item existed; read it through
	 * {@link EchoBundleItem#selectedItemOf}, and write contents through {@link EchoBundleItem#setContents}
	 * so it stays in step with them.
	 */
	public static final DataComponentType<SelectedItem> ECHO_BUNDLE_SELECTED_ITEM =
			DataComponentType.<SelectedItem>builder()
					.persistent(SelectedItem.CODEC)
					.networkSynchronized(SelectedItem.STREAM_CODEC)
					.build();

	/** Absent on an Echo Chest item with nothing set, so a blank chest stacks with a crafted one (ADR-0008). */
	public static final DataComponentType<EchoChestAssignment> ECHO_CHEST_ASSIGNMENT =
			DataComponentType.<EchoChestAssignment>builder()
					.persistent(EchoChestAssignment.CODEC)
					.networkSynchronized(EchoChestAssignment.STREAM_CODEC)
					.build();

	/**
	 * Present on an Echo Shulker Box whose Vacuum toggle is on, and absent while it is off, so a box
	 * nobody has turned on carries nothing extra. Kept on the item when the box is broken, as its
	 * Category and strictness are.
	 */
	public static final DataComponentType<Unit> ECHO_SHULKER_BOX_VACUUM =
			DataComponentType.<Unit>builder()
					.persistent(Unit.CODEC)
					.networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
					.build();

	private EchoComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_bundle_contents"), ECHO_BUNDLE_CONTENTS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_bundle_settings"), ECHO_BUNDLE_SETTINGS);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_bundle_selected_item"), ECHO_BUNDLE_SELECTED_ITEM);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_chest_assignment"), ECHO_CHEST_ASSIGNMENT);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoStorage.id("echo_shulker_box_vacuum"), ECHO_SHULKER_BOX_VACUUM);
	}
}
