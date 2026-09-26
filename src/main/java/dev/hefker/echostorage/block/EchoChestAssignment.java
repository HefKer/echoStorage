package dev.hefker.echostorage.block;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An Echo Chest's Category and strictness as they travel on its item, so a broken chest keeps
 * what the player set on it (ADR-0008). Only put on the item when something is set, so a blank
 * chest still stacks with a freshly crafted one.
 *
 * <p>The Category is saved by name and read leniently: a name no preset has any more, or a
 * value of the wrong type, loads as none, because a failing component would lose the item,
 * and the chest's name with it.
 *
 * @param category what the chest is assigned to hold, if anything
 * @param strict   whether the chest refuses strays from everything but the player's hand (ADR-0009)
 */
public record EchoChestAssignment(Optional<Category> category, boolean strict) {
	public static final EchoChestAssignment NONE = new EchoChestAssignment(Optional.empty(), false);

	private static final MapCodec<Optional<Category>> CATEGORY_FIELD = Codec.STRING.lenientOptionalFieldOf("category")
			.xmap(name -> name.flatMap(Categories::byName), category -> category.map(Category::name));

	public static final Codec<EchoChestAssignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			CATEGORY_FIELD.forGetter(EchoChestAssignment::category),
			Codec.BOOL.lenientOptionalFieldOf("strict", false).forGetter(EchoChestAssignment::strict)
	).apply(instance, EchoChestAssignment::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChestAssignment> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
					.map(name -> name.flatMap(Categories::byName), category -> category.map(Category::name)),
			EchoChestAssignment::category,
			ByteBufCodecs.BOOL, EchoChestAssignment::strict,
			EchoChestAssignment::new);

	/** Whether nothing is set, in which case the item carries no assignment at all. */
	public boolean isBlank() {
		return category.isEmpty() && !strict;
	}
}
