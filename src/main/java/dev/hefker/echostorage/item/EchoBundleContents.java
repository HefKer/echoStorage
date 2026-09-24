package dev.hefker.echostorage.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.math.Fraction;

/**
 * What an Echo Bundle holds: vanilla's {@code BundleContents} with every item weighing a
 * quarter of what it would there, so the same {@link Fraction#ONE} budget fits four bundles'
 * worth.
 *
 * <p>Weight is derived from the items, never stored, so the codecs carry only the item list.
 *
 * <p>Unlike vanilla, an entry never grows past its item's max stack size. Vanilla can merge
 * freely because its whole budget is one stack; ours is four, and a merged entry of 256 would
 * exceed the 1-99 count that {@code ItemStack.CODEC} accepts and fail the next save (ADR-0005).
 */
public final class EchoBundleContents {
	public static final EchoBundleContents EMPTY = new EchoBundleContents(List.of());
	public static final Codec<EchoBundleContents> CODEC =
			ItemStack.CODEC.listOf().xmap(EchoBundleContents::new, contents -> contents.items);
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoBundleContents> STREAM_CODEC =
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).map(EchoBundleContents::new, contents -> contents.items);

	/** A vanilla bundle's capacity is one full stack; ours is this many. */
	public static final int CAPACITY_IN_STACKS = 4;

	final List<ItemStack> items;
	final Fraction weight;

	EchoBundleContents(List<ItemStack> items, Fraction weight) {
		this.items = items;
		this.weight = weight;
	}

	public EchoBundleContents(List<ItemStack> items) {
		this(items, weightOf(items));
	}

	private static Fraction weightOf(List<ItemStack> items) {
		Fraction total = Fraction.ZERO;
		for (ItemStack stack : items) {
			total = total.add(weightOf(stack, stack.getCount()));
		}
		return total;
	}

	/**
	 * Whether a stack may go in at all. No bundle of any kind does: vanilla charges a nested
	 * bundle 1/16 of a budget sized for one stack, which against our budget of four would allow
	 * 64 bundles per bundle and a component that grows geometrically. Refusing outright also
	 * makes the one-level recursion in ADR-0007 exhaustive.
	 */
	public static boolean canHold(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem().canFitInsideContainerItems() && !isBundle(stack);
	}

	/** Whether {@code stack} is a bundle of either kind, full or empty. */
	public static boolean isBundle(ItemStack stack) {
		return stack.getItem() instanceof BundleItem
				|| stack.getItem() instanceof EchoBundleItem
				|| stack.has(DataComponents.BUNDLE_CONTENTS)
				|| stack.has(EchoComponents.ECHO_BUNDLE_CONTENTS);
	}

	static Fraction weightOf(ItemStack stack, int count) {
		return unitWeight(stack).multiplyBy(Fraction.getFraction(count, 1));
	}

	/** Vanilla's weight for one of this stack, over {@link #CAPACITY_IN_STACKS}. */
	static Fraction unitWeight(ItemStack stack) {
		boolean carriesBees = !stack.getOrDefault(DataComponents.BEES, List.of()).isEmpty();
		int perStack = carriesBees ? 1 : stack.getMaxStackSize();
		return Fraction.getFraction(1, CAPACITY_IN_STACKS * perStack);
	}

	public Fraction weight() {
		return weight;
	}

	public int size() {
		return items.size();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	public ItemStack getItemUnsafe(int index) {
		return items.get(index);
	}

	public Iterable<ItemStack> items() {
		return items;
	}

	/** Whether any entry is {@code item}, whatever its components. */
	public boolean contains(Item item) {
		return items.stream().anyMatch(entry -> entry.is(item));
	}

	/** Whether every entry passes {@code test}; true of an empty bundle. */
	public boolean allMatch(Predicate<ItemStack> test) {
		return items.stream().allMatch(test);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EchoBundleContents contents
				&& weight.equals(contents.weight)
				&& ItemStack.listMatches(items, contents.items);
	}

	@Override
	public int hashCode() {
		return ItemStack.hashStackList(items);
	}

	@Override
	public String toString() {
		return "EchoBundleContents" + items;
	}

	public static final class Mutable {
		private final List<ItemStack> items;
		private Fraction weight;

		public Mutable(EchoBundleContents contents) {
			this.items = new ArrayList<>(contents.items);
			this.weight = contents.weight;
		}

		private int maxAmountToAdd(ItemStack stack) {
			Fraction room = Fraction.ONE.subtract(weight);
			return Math.max(room.divideBy(unitWeight(stack)).intValue(), 0);
		}

		public int tryInsert(ItemStack stack) {
			if (!canHold(stack)) {
				return 0;
			}
			int amount = Math.min(stack.getCount(), maxAmountToAdd(stack));
			if (amount == 0) {
				return 0;
			}

			weight = weight.add(weightOf(stack, amount));
			int remaining = amount;

			int partial = findPartialEntry(stack);
			if (partial != -1) {
				ItemStack entry = items.remove(partial);
				int added = Math.min(remaining, entry.getMaxStackSize() - entry.getCount());
				items.add(0, entry.copyWithCount(entry.getCount() + added));
				remaining -= added;
			}
			while (remaining > 0) {
				int entrySize = Math.min(remaining, stack.getMaxStackSize());
				items.add(0, stack.copyWithCount(entrySize));
				remaining -= entrySize;
			}

			stack.shrink(amount);
			return amount;
		}

		/** Moves as much of a slot's stack in as fits, taking it the way a player would. */
		public int tryTransfer(Slot slot, Player player) {
			ItemStack stack = slot.getItem();
			if (!canHold(stack)) {
				return 0;
			}
			return tryInsert(slot.safeTake(stack.getCount(), maxAmountToAdd(stack), player));
		}

		/** An entry of the same item with room left in its stack, or -1. */
		private int findPartialEntry(ItemStack stack) {
			if (!stack.isStackable()) {
				return -1;
			}
			for (int i = 0; i < items.size(); i++) {
				ItemStack entry = items.get(i);
				if (entry.getCount() < entry.getMaxStackSize() && ItemStack.isSameItemSameComponents(entry, stack)) {
					return i;
				}
			}
			return -1;
		}

		/** Takes out the most recently added entry, or {@link ItemStack#EMPTY} if there is none. */
		public ItemStack removeOne() {
			if (items.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack removed = items.remove(0).copy();
			weight = weight.subtract(weightOf(removed, removed.getCount()));
			return removed;
		}

		/**
		 * Takes out up to {@code max} of exactly {@code like} — same item, same components —
		 * gathering from as many entries as it needs, most recent first.
		 */
		public ItemStack take(ItemStack like, int max) {
			int taken = 0;
			for (int i = 0; i < items.size() && taken < max; ) {
				ItemStack entry = items.get(i);
				if (!ItemStack.isSameItemSameComponents(entry, like)) {
					i++;
					continue;
				}
				int amount = Math.min(entry.getCount(), max - taken);
				taken += amount;
				if (amount == entry.getCount()) {
					items.remove(i);
				} else {
					items.set(i, entry.copyWithCount(entry.getCount() - amount));
					i++;
				}
			}
			if (taken == 0) {
				return ItemStack.EMPTY;
			}
			weight = weight.subtract(weightOf(like, taken));
			return like.copyWithCount(taken);
		}

		public Fraction weight() {
			return weight;
		}

		public EchoBundleContents toImmutable() {
			return new EchoBundleContents(List.copyOf(items), weight);
		}
	}
}
