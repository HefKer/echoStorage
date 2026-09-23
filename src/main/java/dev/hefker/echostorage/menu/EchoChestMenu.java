package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An open Echo Chest: its 27 slots over the player's inventory, laid out like a vanilla chest.
 *
 * <p>Shift-clicking moves stacks between slots and never looks inside bundles; writing into a
 * nested bundle is quick-stack's job alone (ADR-0007).
 */
public class EchoChestMenu extends AbstractContainerMenu {
	private static final int SLOTS_PER_ROW = 9;
	private static final int ROWS = EchoChestBlockEntity.SLOTS / SLOTS_PER_ROW;

	private final Container container;
	private final EchoChestMenuData data;

	/** Client-side constructor: the container is a stand-in the server syncs contents into. */
	public EchoChestMenu(int containerId, Inventory playerInventory, EchoChestMenuData data) {
		this(containerId, playerInventory, new SimpleContainer(EchoChestBlockEntity.SLOTS), data);
	}

	public EchoChestMenu(int containerId, Inventory playerInventory, Container container, EchoChestMenuData data) {
		super(EchoMenus.ECHO_CHEST, containerId);
		checkContainerSize(container, EchoChestBlockEntity.SLOTS);
		this.container = container;
		this.data = data;
		container.startOpen(playerInventory.player);

		int playerInventoryTop = 103 + (ROWS - 4) * 18;

		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < SLOTS_PER_ROW; column++) {
				addSlot(new Slot(container, column + row * SLOTS_PER_ROW, 8 + column * 18, 18 + row * 18));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, playerInventoryTop + row * 18));
			}
		}

		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(playerInventory, column, 8 + column * 18, playerInventoryTop + 58));
		}
	}

	/** What this menu shows: on the server the chest itself, on the client a stand-in. */
	public Container container() {
		return container;
	}

	public EchoChestMenuData data() {
		return data;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		int containerSlots = EchoChestBlockEntity.SLOTS;
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack inSlot = slot.getItem();
		ItemStack original = inSlot.copy();

		if (index < containerSlots) {
			if (!moveItemStackTo(inSlot, containerSlots, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(inSlot, 0, containerSlots, false)) {
			return ItemStack.EMPTY;
		}

		if (inSlot.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
