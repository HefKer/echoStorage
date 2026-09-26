package dev.hefker.echostorage.block;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An Echo Chest's Category and strictness: the placed chest's state, and what travels on its item
 * so a broken chest keeps what the player set on it (ADR-0008). Only put on the item when something is set, so a blank
 * chest still stacks with a freshly crafted one.
 *
 * <p>The Category is saved by name and read leniently: a name no preset has any more, or a
 * value of the wrong type, loads as none, because a failing component would lose the item,
 * and the chest's name with it. It is dropped without a warning: codecs run on the client and for
 * network sync, often many times per item, and have no position to report, so a warning here
 * would be noise. The placed chest's own load does warn, once, naming where it stands.
 *
 * @param category what the chest is assigned to hold, if anything
 * @param strict   whether the chest refuses strays from everything but the player's hand (ADR-0009)
 */
public record EchoChestAssignment(Optional<Category> category, boolean strict) {
	public static final EchoChestAssignment DEFAULT = new EchoChestAssignment(Optional.empty(), false);

	public static final Codec<EchoChestAssignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Categories.OPTIONAL_FIELD.forGetter(EchoChestAssignment::category),
			Codec.BOOL.lenientOptionalFieldOf("strict", false).forGetter(EchoChestAssignment::strict)
	).apply(instance, EchoChestAssignment::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoChestAssignment> STREAM_CODEC = StreamCodec.composite(
			Categories.OPTIONAL_STREAM_CODEC, EchoChestAssignment::category,
			ByteBufCodecs.BOOL, EchoChestAssignment::strict,
			EchoChestAssignment::new);

	public EchoChestAssignment withCategory(Optional<Category> category) {
		return new EchoChestAssignment(category, strict);
	}

	public EchoChestAssignment withStrict(boolean strict) {
		return new EchoChestAssignment(category, strict);
	}

	/** Whether nothing is set, in which case the item carries no assignment at all. */
	public boolean isBlank() {
		return category.isEmpty() && !strict;
	}
}
