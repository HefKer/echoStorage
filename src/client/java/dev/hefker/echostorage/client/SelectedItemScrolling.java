package dev.hefker.echostorage.client;

import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.network.NetClient;
import dev.hefker.echostorage.network.StepSelectedItemPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Sneak + scroll with an Echo Bundle in the main hand steps its Selected item, one stop per
 * scroll: down to the next, up to the previous, as the hotbar would move. The server does the
 * stepping and shows the result; the client only asks.
 */
public final class SelectedItemScrolling {
	private SelectedItemScrolling() {
	}

	/**
	 * Asks the server to step the Selected item if this scroll is one that should; returns whether
	 * it did, so the hotbar is left alone. An empty bundle, or one in the off hand, lets the hotbar
	 * scroll as usual.
	 *
	 * @param direction vanilla's hotbar scroll amount: positive for up
	 */
	public static boolean onScroll(Player player, double direction) {
		ItemStack held = player.getMainHandItem();
		if (!player.isShiftKeyDown() || !(held.getItem() instanceof EchoBundleItem)
				|| EchoBundleItem.selectedItemOf(held).isEmpty()) {
			return false;
		}
		NetClient.sendToServer(new StepSelectedItemPayload(direction > 0 ? -1 : 1));
		return true;
	}
}
