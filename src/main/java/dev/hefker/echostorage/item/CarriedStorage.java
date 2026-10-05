package dev.hefker.echostorage.item;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/**
 * Bundles and shulker boxes: the storage a player carries. Quick-stack and Vacuum never move one,
 * and inside an Echo Chest an Echo Bundle or a shulker box is See-through, exactly one level deep
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
	 * Whether {@code stack} is a See-through shulker box: one with the vanilla {@code container}
	 * component and no unrolled loot.
	 */
	private static boolean isSeeThroughShulkerBox(ItemStack stack) {
		return stack.is(SHULKER_BOXES) && stack.has(DataComponents.CONTAINER) && !stack.has(DataComponents.CONTAINER_LOOT);
	}

	/**
	 * What is inside {@code stack}, if it is See-through: an Echo Bundle's or a shulker box's items.
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
	 * and the Echo Shulker Box, each by its own class's count. Empty for another mod's, since the
	 * {@code container} component does not say, so only the stacks it already lists may be written.
	 */
	public static OptionalInt shulkerBoxSlots(ItemStack stack) {
		if (stack.getItem() instanceof EchoShulkerBoxItem) {
			return OptionalInt.of(EchoShulkerBoxBlockEntity.SLOTS);
		}
		// Another mod's box may extend vanilla's block and still have a size of its own.
		boolean vanilla = stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock
				&& ResourceLocation.DEFAULT_NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
		return vanilla ? OptionalInt.of(ShulkerBoxBlockEntity.CONTAINER_SIZE) : OptionalInt.empty();
	}

	/** Whether {@code stack} is See-through and holds {@code item}, whatever its components. */
	public static boolean holds(ItemStack stack, Item item) {
		return contents(stack).filter(inside -> inside.stream().anyMatch(held -> held.is(item))).isPresent();
	}
}
