package dev.hefker.echostorage.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Tooltip data for an Echo Bundle; the client draws it with a capped grid.
 *
 * @param selected the Selected item, whose entries the grid highlights; none if the bundle is empty
 */
public record EchoBundleTooltip(EchoBundleContents contents, SelectedItem selected) implements TooltipComponent {
}
