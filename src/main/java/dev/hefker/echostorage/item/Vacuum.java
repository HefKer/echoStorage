package dev.hefker.echostorage.item;

import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import dev.hefker.echostorage.block.QuickStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/**
 * Auto-vacuum: a stack the player picks up goes into the Echo Bundles and Echo Shulker Boxes they
 * carry before it reaches their inventory. Legal under ADR-0002 because it moves items between the
 * world and the player's own containers, never between two containers.
 *
 * <p>Each carried item wants what is in its Category or what it already holds (ADR-0010). A box
 * decides exactly as Quick-stack into it would, strictness included (ADR-0009), and so tops up the
 * bundles inside it before its own slots (ADR-0007).
 */
public final class Vacuum {
	private Vacuum() {
	}

	/**
	 * Moves as much of {@code pickedUp} as fits into each bundle or box in {@code inventory} that
	 * wants it, in slot order, shrinking {@code pickedUp} by what went in.
	 */
	public static void run(Container inventory, ItemStack pickedUp) {
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
		if (!box.has(EchoComponents.ECHO_SHULKER_BOX_VACUUM)) {
			return null;
		}
		SimpleContainer slots = new SimpleContainer(EchoShulkerBoxBlockEntity.SLOTS);
		box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots.getItems());
		EchoChestAssignment assignment = box.getOrDefault(EchoComponents.ECHO_CHEST_ASSIGNMENT, EchoChestAssignment.DEFAULT);
		int before = pickedUp.getCount();
		// A box refuses every shulker box, and a Strict one its strays, as it does when placed.
		QuickStack.put(slots, QuickStack.wanted(slots, assignment.category()),
				stack -> EchoChestBlockEntity.refuses(true, assignment.category(), assignment.strict(), stack), pickedUp);
		if (pickedUp.getCount() == before) {
			return null;
		}
		ItemStack written = box.copy();
		written.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots.getItems()));
		return written;
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
