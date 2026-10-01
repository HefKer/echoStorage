package dev.hefker.echostorage.item;

import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Auto-vacuum: a stack the player picks up goes into the Echo Bundles and Echo Shulker Boxes they
 * carry before it reaches their inventory. Legal under ADR-0002 because it moves items between the
 * world and the player's own containers, never between two containers.
 *
 * <p>Each carried item wants what is in its Category or what it already holds (ADR-0010). A box
 * decides exactly as Quick-stack into it would, strictness included (ADR-0009), and so tops up the
 * bundles inside it before its own slots (ADR-0007).
 *
 * <p>A picked-up bundle or shulker box is carried storage, and no carried item takes it.
 */
public final class Vacuum {
	private Vacuum() {
	}

	/**
	 * Moves as much of {@code pickedUp} as fits into each bundle or box in {@code inventory} that
	 * wants it, in slot order, shrinking {@code pickedUp} by what went in. Carried storage is left
	 * whole, whatever would have wanted it.
	 */
	public static void run(Container inventory, ItemStack pickedUp) {
		if (CarriedStorage.isCarriedStorage(pickedUp)) {
			return;
		}
		for (int slot = 0; slot < inventory.getContainerSize() && !pickedUp.isEmpty(); slot++) {
			ItemStack carried = inventory.getItem(slot);
			if (carried.getCount() != 1) {
				continue;
			}
			ItemStack written = carried.has(EchoComponents.ECHO_BUNDLE_CONTENTS)
					? intoBundle(carried, pickedUp)
					: intoBox(carried, pickedUp);
			if (written != null) {
				inventory.setItem(slot, written);
			}
		}
	}

	/** The bundle with what it took of {@code pickedUp} added, or null if it took nothing. */
	@Nullable
	private static ItemStack intoBundle(ItemStack bundle, ItemStack pickedUp) {
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (!wants(EchoBundleItem.settingsOf(bundle), contents, pickedUp)) {
			return null;
		}
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
		if (mutable.tryInsert(pickedUp) == 0) {
			return null;
		}
		ItemStack written = bundle.copy();
		written.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		return written;
	}

	/**
	 * An Echo Shulker Box with its toggle on, with what it took of {@code pickedUp} added: the same
	 * transfer as Quick-stacking that one stack into it. Null if it took nothing, or is no such box.
	 */
	@Nullable
	private static ItemStack intoBox(ItemStack box, ItemStack pickedUp) {
		return box.has(EchoComponents.ECHO_SHULKER_BOX_VACUUM) ? EchoShulkerBoxBlockEntity.putIntoItem(box, pickedUp) : null;
	}

	/**
	 * Whether a bundle vacuums {@code stack}: never with the toggle off; otherwise whatever its
	 * Category matches or it already holds — so an empty bundle with no Category takes nothing, and
	 * a new player's diamonds do not vanish into it.
	 */
	static boolean wants(EchoBundleSettings settings, EchoBundleContents contents, ItemStack stack) {
		return settings.vacuum()
				&& (settings.category().filter(category -> category.matches(stack)).isPresent()
						|| contents.contains(stack.getItem()));
	}
}
