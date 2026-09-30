package dev.hefker.echostorage.item;

import dev.hefker.echostorage.block.EchoChestBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that makes one kind of Echo Chest from another and keeps what the player set
 * on it: the name and the Category and strictness.
 *
 * <p>Only the settings travel. The chest's id is never on the item, so the result is a new chest
 * once placed: Interfaces list it as a new row and Echo Relays have to hear it again.
 */
public class EchoChestUpgradeRecipe extends KeepingShapedRecipe {
	public static final RecipeSerializer<EchoChestUpgradeRecipe> SERIALIZER = serializer(EchoChestUpgradeRecipe::new);

	public EchoChestUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
			boolean showNotification) {
		super(group, category, pattern, result, showNotification);
	}

	/** The name and assignment of the Echo Chest it was made from. */
	@Override
	protected void keep(CraftingInput input, ItemStack upgraded) {
		input.items().stream()
				.filter(EchoChestUpgradeRecipe::isEchoChest)
				.findFirst()
				.ifPresent(chest -> {
					copy(DataComponents.CUSTOM_NAME, chest, upgraded);
					copy(EchoComponents.ECHO_CHEST_ASSIGNMENT, chest, upgraded);
				});
	}

	private static boolean isEchoChest(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EchoChestBlock;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
