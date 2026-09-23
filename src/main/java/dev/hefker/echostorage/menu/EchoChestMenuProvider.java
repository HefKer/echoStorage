package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Opens an {@link EchoChestMenu} on a chest, sending its typed name as open-data. The loader
 * adapter for this menu, as {@link ProbeMenuProvider} is for the probe.
 */
public record EchoChestMenuProvider(EchoChestBlockEntity chest)
		implements ExtendedScreenHandlerFactory<EchoChestMenuData> {

	@Override
	public EchoChestMenuData getScreenOpeningData(ServerPlayer player) {
		return new EchoChestMenuData(chest.name());
	}

	@Override
	public Component getDisplayName() {
		return chest.getDisplayName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new EchoChestMenu(containerId, playerInventory, chest, new EchoChestMenuData(chest.name()));
	}
}
