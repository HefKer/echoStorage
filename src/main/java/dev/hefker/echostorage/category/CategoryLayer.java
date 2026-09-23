package dev.hefker.echostorage.category;

import net.minecraft.world.item.ItemStack;

/**
 * One way of deciding that an item belongs to a Category. See ADR-0001: a Category is an
 * ordered list of these — its tag, then predicates, curated unions and mod id as they arrive.
 */
@FunctionalInterface
public interface CategoryLayer {
	boolean matches(ItemStack stack);
}
