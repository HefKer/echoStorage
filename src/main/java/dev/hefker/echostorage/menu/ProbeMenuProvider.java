package dev.hefker.echostorage.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Opens a {@link ProbeMenu} over a container the caller supplies, handing the client one
 * {@link ProbeMenuData} record as open-data.
 *
 * <p>The record is the only thing crossing the wire at open time — the shape every future
 * Echo Chest and Echo Interface screen will reuse.
 */
public record ProbeMenuProvider(Container container, ProbeMenuData data)
		implements ExtendedScreenHandlerFactory<ProbeMenuData> {

	@Override
	public ProbeMenuData getScreenOpeningData(ServerPlayer player) {
		return data;
	}

	@Override
	public Component getDisplayName() {
		return Component.literal(data.label());
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ProbeMenu(containerId, playerInventory, container, data);
	}
}
