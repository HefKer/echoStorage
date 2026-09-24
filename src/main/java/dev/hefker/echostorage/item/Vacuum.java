package dev.hefker.echostorage.item;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Auto-vacuum: a stack the player picks up goes into the Echo Bundles they carry before it
 * reaches their inventory. Legal under ADR-0002 because it moves items between the world and
 * the player's own containers, never between two containers.
 */
public final class Vacuum {
	private Vacuum() {
	}

	/**
	 * Moves as much of {@code pickedUp} as fits into each bundle in {@code inventory} that
	 * {@linkplain #wants wants} it, in slot order, shrinking {@code pickedUp} by what went in.
	 */
	public static void run(Container inventory, ItemStack pickedUp) {
		for (int slot = 0; slot < inventory.getContainerSize() && !pickedUp.isEmpty(); slot++) {
			ItemStack bundle = inventory.getItem(slot);
			EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
			if (contents == null || bundle.getCount() != 1 || !wants(EchoBundleItem.settingsOf(bundle), contents, pickedUp)) {
				continue;
			}
			EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
			if (mutable.tryInsert(pickedUp) > 0) {
				ItemStack written = bundle.copy();
				written.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
				inventory.setItem(slot, written);
			}
		}
	}

	/**
	 * Whether a bundle vacuums {@code stack}: never with the toggle off; with a Category, whatever
	 * the Category matches; without one, whatever it already holds — so an empty bundle with no
	 * Category takes nothing, and a new player's diamonds do not vanish into it.
	 */
	static boolean wants(EchoBundleSettings settings, EchoBundleContents contents, ItemStack stack) {
		if (!settings.vacuum()) {
			return false;
		}
		return settings.category()
				.map(category -> category.matches(stack))
				.orElseGet(() -> contents.contains(stack.getItem()));
	}
}
