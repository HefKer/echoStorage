package dev.hefker.echostorage.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip data for an Echo Bundle; the client draws it with a capped grid. */
public record EchoBundleTooltip(EchoBundleContents contents) implements TooltipComponent {
}
