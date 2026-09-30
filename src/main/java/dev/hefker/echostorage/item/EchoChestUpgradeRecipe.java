package dev.hefker.echostorage.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.block.EchoChestBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that makes one kind of Echo Chest from another and keeps what the player set
 * on it: the name and the Category and strictness. Vanilla's shaped recipe drops every
 * component of its inputs, so an upgraded chest would otherwise come out blank.
 *
 * <p>Only the settings travel. The chest's id is never on the item, so the result is a new chest
 * once placed: Interfaces list it as a new row and Echo Relays have to hear it again.
 *
 * <p>It is a {@link ShapedRecipe} rather than a recipe of its own shape so the recipe book shows
 * it and places its ingredients like any shaped recipe. The JSON is vanilla's
 * {@code crafting_shaped}, under this recipe's own type.
 */
public class EchoChestUpgradeRecipe extends ShapedRecipe {
	public static final RecipeSerializer<EchoChestUpgradeRecipe> SERIALIZER = new Serializer();

	// ShapedRecipe keeps these package-private, and the serializer has to write them back out.
	private final ShapedRecipePattern pattern;
	private final ItemStack result;

	public EchoChestUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
			boolean showNotification) {
		super(group, category, pattern, result, showNotification);
		this.pattern = pattern;
		this.result = result;
	}

	/** The result, carrying the name and assignment of the Echo Chest it was made from. */
	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack upgraded = super.assemble(input, registries);
		input.items().stream()
				.filter(EchoChestUpgradeRecipe::isEchoChest)
				.findFirst()
				.ifPresent(chest -> {
					copy(DataComponents.CUSTOM_NAME, chest, upgraded);
					copy(EchoComponents.ECHO_CHEST_ASSIGNMENT, chest, upgraded);
				});
		return upgraded;
	}

	private static boolean isEchoChest(ItemStack stack) {
		return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EchoChestBlock;
	}

	private static <T> void copy(DataComponentType<T> type, ItemStack from, ItemStack to) {
		T value = from.get(type);
		if (value != null) {
			to.set(type, value);
		}
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	/** Vanilla's shaped-recipe format, field for field, building this recipe instead. */
	private static final class Serializer implements RecipeSerializer<EchoChestUpgradeRecipe> {
		private static final MapCodec<EchoChestUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(recipe -> recipe.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
				CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
				ShapedRecipePattern.MAP_CODEC.forGetter(upgrade -> upgrade.pattern),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(upgrade -> upgrade.result),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
		).apply(recipe, EchoChestUpgradeRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, EchoChestUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
				CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
				ShapedRecipePattern.STREAM_CODEC, upgrade -> upgrade.pattern,
				ItemStack.STREAM_CODEC, upgrade -> upgrade.result,
				ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
				EchoChestUpgradeRecipe::new);

		@Override
		public MapCodec<EchoChestUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, EchoChestUpgradeRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
