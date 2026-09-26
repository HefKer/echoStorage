package dev.hefker.echostorage.menu;

import java.util.function.Predicate;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoRelayBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Opens an {@link EchoChestMenu} on a chest, sending its typed name as open-data. The loader
 * adapter for this menu, as {@link ProbeMenuProvider} is for the probe.
 *
 * @param remoteReach set when the chest is opened from an Echo Interface; see {@link EchoChestMenu}
 */
public record EchoChestMenuProvider(EchoChestBlockEntity chest, @Nullable Predicate<Player> remoteReach)
		implements ExtendedScreenHandlerFactory<EchoChestMenuData> {

	/** Opens the chest for a player standing at it. */
	public EchoChestMenuProvider(EchoChestBlockEntity chest) {
		this(chest, null);
	}

	@Override
	public EchoChestMenuData getScreenOpeningData(ServerPlayer player) {
		return openingData();
	}

	@Override
	public Component getDisplayName() {
		return chest.getDisplayName();
	}

	/** Also where an Echo Relay hears the chest opened, whether the player is at it or at an interface. */
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		if (player.level() instanceof ServerLevel level && !player.isSpectator()) {
			EchoRelayBlockEntity.hearOpening(level, chest);
		}
		return new EchoChestMenu(containerId, playerInventory, chest, openingData(), remoteReach);
	}

	private EchoChestMenuData openingData() {
		return new EchoChestMenuData(chest.name());
	}
}
