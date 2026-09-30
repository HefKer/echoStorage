package dev.hefker.echostorage.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Dyes an Echo Shulker Box: one box and one dye anywhere in the grid, as vanilla dyes its
 * shulker boxes. The box keeps everything else, and can be dyed again.
 */
public class EchoShulkerBoxColoring extends CustomRecipe {
	public static final RecipeSerializer<EchoShulkerBoxColoring> SERIALIZER = new SimpleCraftingRecipeSerializer<>(EchoShulkerBoxColoring::new);

	public EchoShulkerBoxColoring(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		int boxes = 0;
		int dyes = 0;
		for (ItemStack stack : input.items()) {
			if (stack.is(EchoItems.ECHO_SHULKER_BOX)) {
				boxes++;
			} else if (stack.getItem() instanceof DyeItem) {
				dyes++;
			} else if (!stack.isEmpty()) {
				return false;
			}
		}
		return boxes == 1 && dyes == 1;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack box = ItemStack.EMPTY;
		DyeItem dye = null;
		for (ItemStack stack : input.items()) {
			if (stack.is(EchoItems.ECHO_SHULKER_BOX)) {
				box = stack;
			} else if (stack.getItem() instanceof DyeItem item) {
				dye = item;
			}
		}
		if (box.isEmpty() || dye == null) {
			return ItemStack.EMPTY;
		}
		ItemStack dyed = box.copyWithCount(1);
		dyed.set(DataComponents.BASE_COLOR, dye.getDyeColor());
		return dyed;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
