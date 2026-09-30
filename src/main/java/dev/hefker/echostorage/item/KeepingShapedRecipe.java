package dev.hefker.echostorage.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe whose result keeps some of what was on one of its inputs. Vanilla's shaped
 * recipe drops every component of its inputs, so a container crafted into another kind would
 * otherwise come out blank.
 *
 * <p>It is a {@link ShapedRecipe} rather than a recipe of its own shape so the recipe book shows
 * it and places its ingredients like any shaped recipe. The JSON is vanilla's
 * {@code crafting_shaped}, under the recipe's own type.
 */
public abstract class KeepingShapedRecipe extends ShapedRecipe {
	// ShapedRecipe keeps these package-private, and the serializer has to write them back out.
	private final ShapedRecipePattern pattern;
	private final ItemStack result;

	protected KeepingShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
			boolean showNotification) {
		super(group, category, pattern, result, showNotification);
		this.pattern = pattern;
		this.result = result;
	}

	@Override
	public final ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack made = super.assemble(input, registries);
		keep(input, made);
		return made;
	}

	private ShapedRecipePattern pattern() {
		return pattern;
	}

	private ItemStack result() {
		return result;
	}

	/** Copies onto {@code made}, the stack just assembled, what it keeps from the inputs. */
	protected abstract void keep(CraftingInput input, ItemStack made);

	/** Sets {@code type} on {@code to} as it is on {@code from}, if it is there at all. */
	protected static <T> void copy(DataComponentType<T> type, ItemStack from, ItemStack to) {
		T value = from.get(type);
		if (value != null) {
			to.set(type, value);
		}
	}

	/** How a {@link KeepingShapedRecipe} is made from what its serializer reads. */
	@FunctionalInterface
	protected interface Factory<T extends KeepingShapedRecipe> {
		T create(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification);
	}

	/** Vanilla's shaped-recipe format, field for field, building the recipe {@code factory} makes. */
	protected static <T extends KeepingShapedRecipe> RecipeSerializer<T> serializer(Factory<T> factory) {
		MapCodec<T> codec = RecordCodecBuilder.mapCodec(recipe -> recipe.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
				CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
				ShapedRecipePattern.MAP_CODEC.forGetter(KeepingShapedRecipe::pattern),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(KeepingShapedRecipe::result),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
		).apply(recipe, factory::create));

		StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
				CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
				ShapedRecipePattern.STREAM_CODEC, KeepingShapedRecipe::pattern,
				ItemStack.STREAM_CODEC, KeepingShapedRecipe::result,
				ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
				factory::create);

		return new RecipeSerializer<>() {
			@Override
			public MapCodec<T> codec() {
				return codec;
			}

			@Override
			public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
				return streamCodec;
			}
		};
	}
}
