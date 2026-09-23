package dev.hefker.echostorage.item;

import dev.hefker.echostorage.EchoStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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

	/** Contents of destroyed Echo Bundles, released at vanilla's worst-case bundle burst per tick. */
	static final StaggeredSpill SPILL = new StaggeredSpill(EchoBundleItem.MAX_ENTRIES_DROPPED_AT_ONCE);

	private EchoItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, EchoStorage.id("echo_bundle"), ECHO_BUNDLE);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(entries -> entries.addAfter(Items.BUNDLE, ECHO_BUNDLE));
		ServerTickEvents.END_SERVER_TICK.register(server -> SPILL.tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SPILL.flush());
	}
}
