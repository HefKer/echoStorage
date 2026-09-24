package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.block.EchoInterfaceBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Opens an {@link EchoInterfaceMenu}, sending the interface's rows as open-data. */
public record EchoInterfaceMenuProvider(EchoInterfaceBlockEntity echoInterface)
		implements ExtendedScreenHandlerFactory<EchoInterfaceMenuData> {

	@Override
	public EchoInterfaceMenuData getScreenOpeningData(ServerPlayer player) {
		return new EchoInterfaceMenuData(echoInterface.rows());
	}

	@Override
	public Component getDisplayName() {
		return echoInterface.getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new EchoInterfaceMenu(containerId, playerInventory, echoInterface);
	}
}
