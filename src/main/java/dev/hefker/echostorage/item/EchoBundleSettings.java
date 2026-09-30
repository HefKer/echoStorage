package dev.hefker.echostorage.item;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the player has set on one Echo Bundle from its own screen: a Category, and whether it
 * vacuums up matching items on pickup. Per-bundle intent, so it lives on the item, never in the
 * config file — which can only switch vacuuming off for every bundle.
 *
 * <p>Both fields are read leniently: a Category name no preset has any more loads as none, and a
 * value of the wrong type loads as the default, because a failing component would lose the whole
 * bundle, contents and all.
 *
 * @param category what the bundle vacuums, if assigned, besides what it already holds
 * @param vacuum   whether the bundle vacuums at all; off until the player turns it on
 */
public record EchoBundleSettings(Optional<Category> category, boolean vacuum) {
	public static final EchoBundleSettings DEFAULT = new EchoBundleSettings(Optional.empty(), false);

	public static final Codec<EchoBundleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Categories.OPTIONAL_FIELD.forGetter(EchoBundleSettings::category),
			Codec.BOOL.lenientOptionalFieldOf("vacuum", false).forGetter(EchoBundleSettings::vacuum)
	).apply(instance, EchoBundleSettings::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EchoBundleSettings> STREAM_CODEC = StreamCodec.composite(
			Categories.OPTIONAL_STREAM_CODEC, EchoBundleSettings::category,
			ByteBufCodecs.BOOL, EchoBundleSettings::vacuum,
			EchoBundleSettings::new);

	public EchoBundleSettings withCategory(Optional<Category> category) {
		return new EchoBundleSettings(category, vacuum);
	}

	public EchoBundleSettings withVacuum(boolean vacuum) {
		return new EchoBundleSettings(category, vacuum);
	}
}
