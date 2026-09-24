package dev.hefker.echostorage.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Opens an {@link EchoBundleMenu} on the bundle in one inventory slot. Plain vanilla: the screen
 * needs no open-data, since its whole state arrives through data slots.
 *
 * @param slot  the player-inventory slot holding the bundle
 * @param title the bundle's name, for the screen
 */
public record EchoBundleMenuProvider(int slot, Component title) implements MenuProvider {
	@Override
	public Component getDisplayName() {
		return title;
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new EchoBundleMenu(containerId, playerInventory, slot);
	}
}
