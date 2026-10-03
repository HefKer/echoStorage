package dev.hefker.echostorage.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip data for an Echo Bundle; the client draws it with a capped grid.
 *
 * @param selected one of the Selected item, whose entries the grid highlights; empty if the bundle is
 */
public record EchoBundleTooltip(EchoBundleContents contents, ItemStack selected) implements TooltipComponent {
}
