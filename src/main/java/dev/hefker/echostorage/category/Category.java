package dev.hefker.echostorage.category;

import java.util.List;

import dev.hefker.echostorage.EchoStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A named set of items an Echo Chest can be assigned to hold, resolved in layers (ADR-0001).
 *
 * <p>The first layer is always the Category's own {@code echostorage:category/<name>} item tag.
 * The shipped tag pulls in the convention tags that describe the Category well; a datapack
 * overrides it like any other tag, which is how pack authors fix categorization in their pack.
 * The {@code later} layers — predicates, curated unions — are consulted after it, in order.
 */
public record Category(String name, List<CategoryLayer> later) {
	public Category {
		later = List.copyOf(later);
	}

	/** A Category backed by its tag, then by {@code later} layers in order. */
	public static Category of(String name, CategoryLayer... later) {
		return new Category(name, List.of(later));
	}

	/** The item tag that backs this Category and that datapacks override. */
	public TagKey<Item> tag() {
		return TagKey.create(Registries.ITEM, EchoStorage.id("category/" + name));
	}

	public String translationKey() {
		return "category.echostorage." + name;
	}

	public Component displayName() {
		return Component.translatable(translationKey());
	}

	/** Whether any layer, the tag first, places {@code stack} in this Category. */
	public boolean matches(ItemStack stack) {
		if (stack.is(tag())) {
			return true;
		}
		for (CategoryLayer layer : later) {
			if (layer.matches(stack)) {
				return true;
			}
		}
		return false;
	}
}
