package dev.hefker.echostorage.item;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Auto-refill: when a player places the last block in their hand, the next stack of it comes
 * out of an Echo Bundle they carry. Legal under ADR-0002: a bundle to the hand, both the player's.
 */
public final class Refill {
	private Refill() {
	}

	/**
	 * Takes up to one stack of exactly {@code like} out of the bundles in {@code inventory}, in
	 * slot order, whatever their vacuum setting. Empty if no bundle holds any.
	 */
	public static ItemStack take(Container inventory, ItemStack like) {
		int wanted = like.getMaxStackSize();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize() && taken < wanted; slot++) {
			ItemStack bundle = inventory.getItem(slot);
			EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
			if (contents == null || bundle.getCount() != 1) {
				continue;
			}
			EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
			int pulled = mutable.take(like, wanted - taken).getCount();
			if (pulled > 0) {
				taken += pulled;
				ItemStack written = bundle.copy();
				written.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
				inventory.setItem(slot, written);
			}
		}
		return taken == 0 ? ItemStack.EMPTY : like.copyWithCount(taken);
	}
}
