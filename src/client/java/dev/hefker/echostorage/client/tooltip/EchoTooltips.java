package dev.hefker.echostorage.client.tooltip;

import dev.hefker.echostorage.item.EchoBundleTooltip;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;

/**
 * Maps tooltip data to its renderer, and the only place that names the loader's tooltip hook.
 * On NeoForge this is {@code RegisterClientTooltipComponentFactoriesEvent}.
 */
public final class EchoTooltips {
	private EchoTooltips() {
	}

	public static void register() {
		TooltipComponentCallback.EVENT.register(data ->
				data instanceof EchoBundleTooltip tooltip ? new ClientEchoBundleTooltip(tooltip.contents()) : null);
	}
}
