package dev.hefker.echostorage.category;

import dev.hefker.echostorage.EchoStorage;
import java.util.ArrayList;
import java.util.List;

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
 */
public record Category(String name, TagKey<Item> tag, List<CategoryLayer> layers) {
	public Category {
		layers = List.copyOf(layers);
	}

	/** A Category backed by its tag, then by {@code later} layers in order. */
	public static Category of(String name, CategoryLayer... later) {
		TagKey<Item> tag = TagKey.create(Registries.ITEM, EchoStorage.id("category/" + name));
		List<CategoryLayer> layers = new ArrayList<>();
		layers.add(stack -> stack.is(tag));
		layers.addAll(List.of(later));
		return new Category(name, tag, layers);
	}

	public Component displayName() {
		return Component.translatable("category.echostorage." + name);
	}

	public boolean matches(ItemStack stack) {
		for (CategoryLayer layer : layers) {
			if (layer.matches(stack)) {
				return true;
			}
		}
		return false;
	}
}
