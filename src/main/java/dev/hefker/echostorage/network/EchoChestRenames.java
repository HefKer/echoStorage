package dev.hefker.echostorage.network;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.menu.EchoChestMenu;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server's side of in-place renaming. The client only ever says what was typed into which
 * screen; whether that renames anything, and to what, is decided here.
 */
public final class EchoChestRenames {
	private EchoChestRenames() {
	}

	/**
	 * Renames the chest behind the player's open Echo Chest screen. Runs on the server thread.
	 *
	 * <p>Ignored unless the payload names the menu the player has open right now and the player
	 * could still use it — so a late packet from a closed screen, or one sent from across the
	 * world, renames nothing.
	 */
	public static void onRename(ServerPlayer player, RenameEchoChestPayload rename) {
		if (player.containerMenu instanceof EchoChestMenu menu
				&& menu.containerId == rename.containerId()
				&& menu.stillValid(player)
				&& menu.container() instanceof EchoChestBlockEntity chest) {
			chest.rename(rename.name());
		}
	}
}
