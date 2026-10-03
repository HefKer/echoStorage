package dev.hefker.echostorage.client;

import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.network.NetClient;
import dev.hefker.echostorage.network.StepSelectedItemPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Sneak + scroll with an Echo Bundle in the main hand steps its Selected item, one stop per
 * notch: down to the next, up to the previous, the way the hotbar moves. The server does the
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
	 * @param notches the whole notches vanilla scrolled the hotbar by: positive for up
	 */
	public static boolean onScroll(Player player, double notches) {
		ItemStack held = player.getMainHandItem();
		if (!player.isShiftKeyDown() || !(held.getItem() instanceof EchoBundleItem)
				|| EchoBundleItem.selectedItemOf(held).isEmpty()) {
			return false;
		}
		NetClient.sendToServer(new StepSelectedItemPayload(-(int) notches));
		return true;
	}
}
