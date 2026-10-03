package dev.hefker.echostorage.network;

import dev.hefker.echostorage.item.EchoBundleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The server's side of sneak-scrolling an Echo Bundle. The client only says that the player
 * scrolled; which bundle that steps, and whether it steps at all, is decided here.
 */
public final class SelectedItemSteps {
	private SelectedItemSteps() {
	}

	/**
	 * Steps the Selected item of the Echo Bundle in the player's main hand. Runs on the server
	 * thread. Does nothing unless the main hand holds an Echo Bundle with something in it, so a
	 * late packet after the player switched items steps nothing.
	 */
	public static void onStep(ServerPlayer player, StepSelectedItemPayload step) {
		ItemStack bundle = player.getMainHandItem();
		if (bundle.getItem() instanceof EchoBundleItem) {
			EchoBundleItem.stepSelectedItem(bundle, step.steps(), player);
		}
	}
}
