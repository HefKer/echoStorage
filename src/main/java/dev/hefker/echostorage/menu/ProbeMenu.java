package dev.hefker.echostorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A plain chest-shaped menu opened from {@link ProbeMenuData}.
 *
 * <p>It holds a vanilla {@link Container} rather than a {@code Storage<ItemVariant>}
 * (ADR-0003 rule 5) and knows nothing about how its open-data reached the client.
 */
public class ProbeMenu extends AbstractContainerMenu {
	private final Container container;
	private final ProbeMenuData data;

	/** Client-side constructor: the container is a stand-in the server syncs contents into. */
	public ProbeMenu(int containerId, Inventory playerInventory, ProbeMenuData data) {
		this(containerId, playerInventory, new SimpleContainer(data.slotCount()), data);
	}

	public ProbeMenu(int containerId, Inventory playerInventory, Container container, ProbeMenuData data) {
		super(EchoMenus.PROBE, containerId);
		checkContainerSize(container, data.slotCount());
		this.container = container;
		this.data = data;
		container.startOpen(playerInventory.player);

		int rows = data.rows();
		int playerInventoryTop = 103 + (rows - 4) * 18;

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < ProbeMenuData.SLOTS_PER_ROW; column++) {
				addSlot(new Slot(container, column + row * ProbeMenuData.SLOTS_PER_ROW,
						8 + column * 18, 18 + row * 18));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(playerInventory, column + row * 9 + 9,
						8 + column * 18, playerInventoryTop + row * 18));
			}
		}

		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(playerInventory, column, 8 + column * 18, playerInventoryTop + 58));
		}
	}

	public ProbeMenuData data() {
		return data;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		int containerSlots = data.slotCount();
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
