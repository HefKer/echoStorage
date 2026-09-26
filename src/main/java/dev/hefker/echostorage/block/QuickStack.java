package dev.hefker.echostorage.block;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Quick-stack: moves everything from the player that a chest wants into that chest — what is
 * in its Category or what it already holds (ADR-0010) — unless the chest refuses it (ADR-0009).
 */
public final class QuickStack {
	private QuickStack() {
	}

	/**
	 * Moves every stack in {@code source}'s slots {@code [from, to)} that {@code wants} matches
	 * into {@code chest}, unless {@code refuses} keeps it out.
	 */
	public static void run(Container chest, Predicate<ItemStack> wants, Predicate<ItemStack> refuses, Container source, int from, int to) {
		for (int slot = from; slot < to; slot++) {
			ItemStack stack = source.getItem(slot);
			// A bundle on the player is their carried storage, never something to put away.
			if (stack.isEmpty() || EchoBundleContents.isBundle(stack) || !wants.test(stack) || refuses.test(stack)) {
				continue;
			}
			ItemStack moving = stack.copy();
			intoBundles(chest, moving);
			intoSlots(chest, moving);
			if (moving.getCount() != stack.getCount()) {
				source.setItem(slot, moving);
			}
		}
	}

	/** What a chest with {@code category} wants: anything in the Category or that it already holds. */
	public static Predicate<ItemStack> wanted(Container chest, Optional<Category> category) {
		return holds(chest).or(inCategory(category));
	}

	/**
	 * Whether {@code chest} already holds an item, reading through the bundles inside it. Taken
	 * once, when called, so what this quick-stack moves in does not widen it.
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
			EchoBundleContents contents = stack.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
			if (contents != null) {
				contents.items().forEach(inside -> held.add(inside.getItem()));
			} else if (!stack.isEmpty()) {
				held.add(stack.getItem());
			}
		}
		return held;
	}

	/** Tops up each bundle in the chest that already holds this item, in slot order. */
	private static void intoBundles(Container chest, ItemStack moving) {
		for (int slot = 0; slot < chest.getContainerSize() && !moving.isEmpty(); slot++) {
			ItemStack bundle = chest.getItem(slot);
			EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
			if (contents == null || bundle.getCount() != 1 || !contents.contains(moving.getItem())) {
				continue;
			}
			EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(contents);
			if (mutable.tryInsert(moving) > 0) {
				ItemStack written = bundle.copy();
				written.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
				chest.setItem(slot, written);
			}
		}
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
