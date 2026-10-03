package dev.hefker.echostorage.item;

import java.util.List;

import dev.hefker.echostorage.block.EchoChestAssignment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Shulker boxes as unit tests can make them. Only vanilla's is registered outside a game, so the
 * others are stand-in items, which {@code VanillaBootstrap} puts in {@code c:shulker_boxes}: what
 * makes a stack a shulker box is the tag and the component, which is all the code under test reads.
 */
public final class TestShulkerBoxes {
	private TestShulkerBoxes() {
	}

	/**
	 * The Echo Shulker Box stands in as an ender chest. That does not make it one known to have 27
	 * slots: a game test fills the real one's empty slots.
	 */
	public static Item echoShulkerBox() {
		// Not a constant: Items cannot be touched until bootstrap has run.
		return Items.ENDER_CHEST;
	}

	/** Stands in for another mod's shulker box: in the tag, with nothing to say how many slots it has. */
	public static Item otherModsShulkerBox() {
		return Items.BARREL;
	}

	/** A shulker box item of the given kind, its slots filled in order from the first. */
	public static ItemStack of(Item kind, ItemStack... slots) {
		ItemStack box = new ItemStack(kind);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(slots)));
		return box;
	}

	/**
	 * Gives an Echo Shulker Box its Category and Strictness, returning the same stack. Like a
	 * broken Echo Shulker Box, it carries them only when something is set.
	 */
	public static ItemStack withAssignment(ItemStack box, EchoChestAssignment assignment) {
		if (!assignment.isBlank()) {
			box.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, assignment);
		}
		return box;
	}

	/** Turns an Echo Shulker Box's Vacuum on, returning the same stack. */
	public static ItemStack withVacuum(ItemStack box) {
		box.set(EchoComponents.ECHO_SHULKER_BOX_VACUUM, Unit.INSTANCE);
		return box;
	}

	/** The box's slots, from the first to the last one filled; none if it carries no contents. */
	public static List<ItemStack> slots(ItemStack box) {
		return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
	}

	/** One of the box's slots, empty past the last one filled. */
	public static ItemStack slot(ItemStack box, int index) {
		List<ItemStack> slots = slots(box);
		return index < slots.size() ? slots.get(index) : ItemStack.EMPTY;
	}
}
