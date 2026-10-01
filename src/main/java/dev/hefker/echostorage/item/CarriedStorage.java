package dev.hefker.echostorage.item;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Bundles and shulker boxes: the storage a player carries. Quick-stack and Vacuum never move one,
 * and inside an Echo Chest an Echo Bundle or a shulker box is see-through, exactly one level deep
 * (ADR-0007): the one place that says which stacks those are and what is in them.
 */
public final class CarriedStorage {
	/** Every shulker box, vanilla's, ours and other mods'. */
	public static final TagKey<Item> SHULKER_BOXES =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "shulker_boxes"));

	private CarriedStorage() {
	}

	/** Whether {@code stack} is a bundle or a shulker box of any kind, full or empty. */
	public static boolean isCarriedStorage(ItemStack stack) {
		return EchoBundleContents.isBundle(stack) || stack.is(SHULKER_BOXES);
	}

	/**
	 * Whether {@code stack} is a shulker box that keeps its items where they can be read and
	 * written. One whose loot has not been rolled yet is a plain item.
	 */
	private static boolean isSeeThroughShulkerBox(ItemStack stack) {
		return stack.is(SHULKER_BOXES) && stack.has(DataComponents.CONTAINER) && !stack.has(DataComponents.CONTAINER_LOOT);
	}

	/**
	 * What is inside {@code stack}, if it is see-through: an Echo Bundle's or a shulker box's items.
	 * Empty for any other stack, which is then judged as the item it is. The items are not looked
	 * into in turn, so a bundle inside a shulker box is one of them and what it holds is not.
	 */
	public static Optional<List<ItemStack>> contents(ItemStack stack) {
		EchoBundleContents bundled = stack.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (bundled != null) {
			return Optional.of(bundled.items);
		}
		if (isSeeThroughShulkerBox(stack)) {
			return Optional.of(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList());
		}
		return Optional.empty();
	}

	/**
	 * How many slots the shulker box {@code stack} has, where that is known: vanilla's shulker boxes
	 * and the Echo Shulker Box, which has as many. Empty for another mod's, since the
	 * {@code container} component does not say, so only the stacks it already lists may be written.
	 */
	public static OptionalInt shulkerBoxSlots(ItemStack stack) {
		boolean known = stack.getItem() instanceof EchoShulkerBoxItem
				|| (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock);
		return known ? OptionalInt.of(EchoShulkerBoxBlockEntity.SLOTS) : OptionalInt.empty();
	}

	/** Whether {@code stack} is see-through and holds {@code item}, whatever its components. */
	public static boolean holds(ItemStack stack, Item item) {
		return contents(stack).filter(inside -> inside.stream().anyMatch(held -> held.is(item))).isPresent();
	}
}
