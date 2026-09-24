package dev.hefker.echostorage.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.minecraft.network.chat.Component;

/** What a Category cycle button offers, on any screen that assigns one: none, then each preset. */
final class CategoryChoices {
	static final Component BUTTON_LABEL = Component.translatable("container.echostorage.category");

	private CategoryChoices() {
	}

	static List<Optional<Category>> all() {
		List<Optional<Category>> choices = new ArrayList<>();
		choices.add(Optional.empty());
		Categories.ALL.forEach(category -> choices.add(Optional.of(category)));
		return choices;
	}

	static Component label(Optional<Category> category) {
		return category.map(Category::displayName)
				.orElseGet(() -> Component.translatable("container.echostorage.category.none"));
	}
}
