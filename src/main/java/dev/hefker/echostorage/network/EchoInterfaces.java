package dev.hefker.echostorage.network;

import java.util.Optional;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoInterfaceBlockEntity;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import dev.hefker.echostorage.menu.EchoInterfaceMenu;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server's side of the Echo Interface screen. The client only ever names a chest by id;
 * whether that opens or dismisses anything is decided here.
 */
public final class EchoInterfaces {
	private EchoInterfaces() {
	}

	/**
	 * Opens the linked chest the player picked. Runs on the server thread.
	 *
	 * <p>The Links are followed out again first, so a chest cut off or broken since the list was
	 * sent is not opened. The chest then stays open while the player could still use the
	 * interface and the chest still stands — from wherever the chest is.
	 */
	public static void onOpen(ServerPlayer player, OpenLinkedChestPayload open) {
		interfaceFor(player, open.containerId()).ifPresent(echoInterface -> {
			echoInterface.resolve();
			echoInterface.linkedChest(open.chest()).ifPresent(chest -> player.openMenu(
					new EchoChestMenuProvider(chest, viewer -> echoInterface.stillValid(viewer) && stands(chest))));
		});
	}

	/** Takes a greyed row off the interface the player has open. Runs on the server thread. */
	public static void onDismiss(ServerPlayer player, DismissLinkedChestPayload dismiss) {
		interfaceFor(player, dismiss.containerId()).ifPresent(echoInterface -> echoInterface.dismiss(dismiss.chest()));
	}

	/** The interface behind the menu the payload names, if the player has it open and could still use it. */
	private static Optional<EchoInterfaceBlockEntity> interfaceFor(ServerPlayer player, int containerId) {
		return player.containerMenu instanceof EchoInterfaceMenu menu
				&& menu.containerId == containerId
				&& menu.stillValid(player)
				? Optional.ofNullable(menu.echoInterface())
				: Optional.empty();
	}

	private static boolean stands(EchoChestBlockEntity chest) {
		return !chest.isRemoved() && chest.getLevel() != null && chest.getLevel().getBlockEntity(chest.getBlockPos()) == chest;
	}
}
