package dev.hefker.echostorage.item;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Makes an Echo Shulker Box from a vanilla shulker box, keeping what is inside it, its colour and
 * its name. It starts with no Category and Permissive, since a vanilla box has neither to give.
 *
 * <p>A vanilla box whose loot table has not rolled yet, such as one picked up from an End city
 * unopened, does not craft: keeping the table would mean the Echo Shulker Box rolls loot itself,
 * and dropping it would lose the loot without warning. Refusing costs nothing, since placing the
 * box and opening it rolls the loot, and breaking it again gives a box that crafts.
 */
public class EchoShulkerBoxRecipe extends KeepingShapedRecipe {
	public static final RecipeSerializer<EchoShulkerBoxRecipe> SERIALIZER = serializer(EchoShulkerBoxRecipe::new);

	public EchoShulkerBoxRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
			boolean showNotification) {
		super(group, category, pattern, result, showNotification);
	}

	/** Refuses a vanilla box that still has a loot table to roll. */
	@Override
	public boolean matches(CraftingInput input, Level level) {
		return super.matches(input, level) && vanillaBox(input).filter(box -> box.has(DataComponents.CONTAINER_LOOT)).isEmpty();
	}

	/** A vanilla box's colour is which block it is; an Echo Shulker Box's is a component. */
	@Override
	protected void keep(CraftingInput input, ItemStack made) {
		vanillaBox(input).ifPresent(box -> {
			copy(DataComponents.CUSTOM_NAME, box, made);
			copy(DataComponents.CONTAINER, box, made);
			DyeColor color = ShulkerBoxBlock.getColorFromItem(box.getItem());
			if (color != null) {
				made.set(DataComponents.BASE_COLOR, color);
			}
		});
	}

	private static Optional<ItemStack> vanillaBox(CraftingInput input) {
		return input.items().stream()
				.filter(stack -> Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock)
				.findFirst();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
