package dev.hefker.echostorage.item;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.block.EchoBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Item registration, and the only place that names the loader's creative-tab and server-tick
 * events. On NeoForge these become {@code BuildCreativeModeTabContentsEvent} and
 * {@code ServerTickEvent.Post}; nothing else in the package changes.
 */
public final class EchoItems {
	public static final Item ECHO_BUNDLE = new EchoBundleItem(new Item.Properties()
			.stacksTo(1)
			.component(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY));

	public static final Item ECHO_CHEST = new BlockItem(EchoBlocks.ECHO_CHEST, new Item.Properties());

	public static final Item ECHO_INTERFACE = new BlockItem(EchoBlocks.ECHO_INTERFACE, new Item.Properties());

	/** Contents of destroyed Echo Bundles, released at vanilla's worst-case bundle burst per tick. */
	static final StaggeredSpill SPILL = new StaggeredSpill(EchoBundleItem.MAX_ENTRIES_DROPPED_AT_ONCE);

	private EchoItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, EchoStorage.id("echo_bundle"), ECHO_BUNDLE);
		Registry.register(BuiltInRegistries.ITEM, EchoStorage.id("echo_chest"), ECHO_CHEST);
		Registry.register(BuiltInRegistries.ITEM, EchoStorage.id("echo_interface"), ECHO_INTERFACE);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(entries -> entries.addAfter(Items.BUNDLE, ECHO_BUNDLE));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(entries -> entries.addAfter(Items.CHEST, ECHO_CHEST, ECHO_INTERFACE));
		ServerTickEvents.END_SERVER_TICK.register(server -> SPILL.tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SPILL.flush());
	}
}
