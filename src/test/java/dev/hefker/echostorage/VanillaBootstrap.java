package dev.hefker.echostorage;

import java.util.List;
import java.util.Map;

import dev.hefker.echostorage.item.CarriedStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Brings up vanilla's registries so tests can make real {@code ItemStack}s, and binds the one tag
 * the code under test reads, since no datapack does here. Idempotent.
 */
public final class VanillaBootstrap {
	private static boolean tagsBound;

	private VanillaBootstrap() {
	}

	/**
	 * The Echo Shulker Box is not registered outside a game, so here it is an ender chest: what
	 * makes a stack a shulker box is the tag, which {@link #run} gives it, and the component. That
	 * does not make it one known to have 27 slots: a game test fills the real one's empty slots.
	 */
	public static Item echoShulkerBox() {
		// Not a constant: Items cannot be touched until bootstrap has run.
		return Items.ENDER_CHEST;
	}

	/** Stands in for another mod's shulker box: in the tag, with nothing to say how many slots it has. */
	public static Item otherModsShulkerBox() {
		return Items.BARREL;
	}

	public static synchronized void run() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		if (!tagsBound) {
			// The binding is global to the item registry, so every test class sees the same tag.
			BuiltInRegistries.ITEM.bindTags(Map.of(CarriedStorage.SHULKER_BOXES,
					List.of(Items.SHULKER_BOX.builtInRegistryHolder(), echoShulkerBox().builtInRegistryHolder(),
							otherModsShulkerBox().builtInRegistryHolder())));
			tagsBound = true;
		}
	}
}
