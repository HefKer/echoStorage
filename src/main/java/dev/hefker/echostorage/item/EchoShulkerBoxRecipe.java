package dev.hefker.echostorage.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Makes an Echo Shulker Box from a vanilla shulker box, keeping what is inside it, its colour and
 * its name. It starts with no Category and permissive, since a vanilla box has neither to give.
 */
public class EchoShulkerBoxRecipe extends KeepingShapedRecipe {
	public static final RecipeSerializer<EchoShulkerBoxRecipe> SERIALIZER = serializer(EchoShulkerBoxRecipe::new);

	public EchoShulkerBoxRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
			boolean showNotification) {
		super(group, category, pattern, result, showNotification);
	}

	/** A vanilla box's colour is which block it is; an Echo Shulker Box's is a component. */
	@Override
	protected void keep(CraftingInput input, ItemStack made) {
		input.items().stream()
				.filter(stack -> Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock)
				.findFirst()
				.ifPresent(box -> {
					copy(DataComponents.CUSTOM_NAME, box, made);
					copy(DataComponents.CONTAINER, box, made);
					DyeColor color = ShulkerBoxBlock.getColorFromItem(box.getItem());
					if (color != null) {
						made.set(DataComponents.BASE_COLOR, color);
					}
				});
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
