package dev.hefker.echostorage;

import java.util.List;
import java.util.Map;

import dev.hefker.echostorage.item.CarriedStorage;
import dev.hefker.echostorage.item.TestShulkerBoxes;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;

/**
 * Brings up vanilla's registries so tests can make real {@code ItemStack}s, and binds the one tag
 * the code under test reads, since no datapack does here. Idempotent. The tag takes its shulker
 * boxes from {@link TestShulkerBoxes}, so the tag and the tests agree on them.
 */
public final class VanillaBootstrap {
	private static boolean tagBound;

	private VanillaBootstrap() {
	}

	public static synchronized void run() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		if (!tagBound) {
			// The binding is global to the item registry, so every test class sees the same tag.
			// It replaces every item tag at once: another tag goes in this map, not in a second call.
			BuiltInRegistries.ITEM.bindTags(Map.of(CarriedStorage.SHULKER_BOXES,
					List.of(Items.SHULKER_BOX.builtInRegistryHolder(), TestShulkerBoxes.echoShulkerBox().builtInRegistryHolder(),
							TestShulkerBoxes.otherModsShulkerBox().builtInRegistryHolder())));
			tagBound = true;
		}
	}
}
