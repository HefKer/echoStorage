package dev.hefker.echostorage.block;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.CarriedStorage;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Quick-stack: moves everything from the player that a chest wants into that chest — what is
 * in its Category or what it already holds (ADR-0010) — unless the chest refuses it (ADR-0009).
 */
public final class QuickStack {
	/** How many slots a shulker box item is taken to have, as vanilla's and the Echo Shulker Box do. */
	private static final int SHULKER_BOX_SLOTS = 27;

	private QuickStack() {
	}

	/**
	 * Moves every stack in {@code source}'s slots {@code [from, to)} that {@code wants} matches
	 * into {@code chest}, unless {@code refuses} keeps it out.
	 */
	public static void run(Container chest, Predicate<ItemStack> wants, Predicate<ItemStack> refuses, Container source, int from, int to) {
		for (int slot = from; slot < to; slot++) {
			ItemStack stack = source.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack moving = stack.copy();
			put(chest, wants, refuses, moving);
			if (moving.getCount() != stack.getCount()) {
				source.setItem(slot, moving);
			}
		}
	}

	/**
	 * Moves as much of {@code moving} into {@code chest} as fits, if {@code wants} matches it and
	 * {@code refuses} does not keep it out, shrinking {@code moving} by what went in: first topping
	 * up the bundles and shulker boxes inside that already hold it, then into slots. Also how a carried Echo Shulker
	 * Box vacuums a picked-up stack, the one other nested write (ADR-0007).
	 */
	public static void put(Container chest, Predicate<ItemStack> wants, Predicate<ItemStack> refuses, ItemStack moving) {
		// A bundle or shulker box is the player's carried storage, never something to put away: Vacuum
		// relies on this too, so a carried box never takes a picked-up one.
		if (CarriedStorage.is(moving) || !wants.test(moving) || refuses.test(moving)) {
			return;
		}
		intoNested(chest, moving);
		intoSlots(chest, moving);
	}

	/** What a chest with {@code category} wants: anything in the Category or that it already holds. */
	public static Predicate<ItemStack> wanted(Container chest, Optional<Category> category) {
		return holds(chest).or(inCategory(category));
	}

	/**
	 * Whether {@code chest} already holds an item, reading through the bundles and shulker boxes
	 * inside it, one level deep. Taken once, when called, so what this Quick-stack moves in does not
	 * widen it.
	 */
	public static Predicate<ItemStack> holds(Container chest) {
		Set<Item> held = heldBy(chest);
		return stack -> held.contains(stack.getItem());
	}

	/** Whether an item is in {@code category}, by the chest's own rule. With no Category nothing is. */
	public static Predicate<ItemStack> inCategory(Optional<Category> category) {
		return stack -> category.isPresent() && !EchoChestBlockEntity.isStray(category, stack);
	}

	private static Set<Item> heldBy(Container chest) {
		Set<Item> held = new HashSet<>();
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			Optional<Iterable<ItemStack>> contents = CarriedStorage.contents(stack);
			if (contents.isPresent()) {
				contents.get().forEach(inside -> held.add(inside.getItem()));
			} else if (!stack.isEmpty()) {
				held.add(stack.getItem());
			}
		}
		return held;
	}

	/** Tops up each bundle and shulker box in the chest that already holds this item, in slot order. */
	private static void intoNested(Container chest, ItemStack moving) {
		for (int slot = 0; slot < chest.getContainerSize() && !moving.isEmpty(); slot++) {
			ItemStack nested = chest.getItem(slot);
			// Writing to one component would give every item in a stack of several the new contents.
			if (nested.getCount() != 1 || !CarriedStorage.holds(nested, moving.getItem())) {
				continue;
			}
			int before = moving.getCount();
			ItemStack written = nested.copy();
			EchoBundleContents bundled = nested.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
			if (bundled != null) {
				EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(bundled);
				mutable.tryInsert(moving);
				written.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
			} else {
				intoShulkerBox(written, moving);
			}
			if (moving.getCount() != before) {
				chest.setItem(slot, written);
			}
		}
	}

	/** Puts as much of {@code moving} as fits into the shulker box item {@code box}, as into a chest's slots. */
	private static void intoShulkerBox(ItemStack box, ItemStack moving) {
		ItemContainerContents contents = box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		// Another mod's bigger box: slots that would not be read back must not be written away.
		if (contents.stream().count() > SHULKER_BOX_SLOTS) {
			return;
		}
		SimpleContainer slots = new SimpleContainer(SHULKER_BOX_SLOTS);
		contents.copyInto(slots.getItems());
		intoSlots(slots, moving);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots.getItems()));
	}

	/** Merges into matching stacks first, then fills empty slots, as a shift-click would. */
	private static void intoSlots(Container chest, ItemStack moving) {
		for (int slot = 0; slot < chest.getContainerSize() && !moving.isEmpty(); slot++) {
			ItemStack there = chest.getItem(slot);
			if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, moving)) {
				int room = Math.min(chest.getMaxStackSize(there), there.getMaxStackSize()) - there.getCount();
				int added = Math.min(room, moving.getCount());
				if (added > 0) {
					chest.setItem(slot, there.copyWithCount(there.getCount() + added));
					moving.shrink(added);
				}
			}
		}
		for (int slot = 0; slot < chest.getContainerSize() && !moving.isEmpty(); slot++) {
			if (chest.getItem(slot).isEmpty()) {
				chest.setItem(slot, moving.split(Math.min(chest.getMaxStackSize(moving), moving.getMaxStackSize())));
			}
		}
	}
}
