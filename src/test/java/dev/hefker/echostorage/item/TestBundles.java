package dev.hefker.echostorage.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Echo Bundles as unit tests can make them. The item is not registered outside a game, so a
 * bundle here is a stick carrying the components, which is all the code under test reads.
 */
final class TestBundles {
	private TestBundles() {
	}

	static ItemStack of(EchoBundleSettings settings, ItemStack... contents) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (ItemStack stack : contents) {
			mutable.tryInsert(stack.copy());
		}
		ItemStack bundle = new ItemStack(Items.STICK);
		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		bundle.set(EchoComponents.ECHO_BUNDLE_SETTINGS, settings);
		return bundle;
	}

	/** How many of {@code item} the bundle holds, however its entries are split. */
	static int count(ItemStack bundle, Item item) {
		int held = 0;
		for (ItemStack inside : bundle.getOrDefault(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY).items()) {
			if (inside.is(item)) {
				held += inside.getCount();
			}
		}
		return held;
	}
}
