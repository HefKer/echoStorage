package dev.hefker.echostorage.menu;

import java.util.Optional;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;

/**
 * A Category as a menu data-slot value, and as an offset from a menu's clear-Category button.
 * 0 is none, so a client menu that has not heard from the server yet reads as unassigned rather
 * than as the first preset; each preset follows in {@link Categories#ALL} order.
 */
final class CategoryData {
	private CategoryData() {
	}

	static int encode(Optional<Category> category) {
		return category.map(assigned -> Categories.ALL.indexOf(assigned) + 1).orElse(0);
	}

	/** The inverse of {@link #encode}; a value no preset has reads as none. */
	static Optional<Category> decode(int value) {
		return value >= 1 && isValid(value) ? Optional.of(Categories.ALL.get(value - 1)) : Optional.empty();
	}

	/** Whether {@code value} is none or a preset, and so a button offset worth acting on. */
	static boolean isValid(int value) {
		return value >= 0 && value <= Categories.ALL.size();
	}

	/** The button that assigns {@code category}, counting from a menu's clear-Category button. */
	static int assignButton(int clearButton, Category category) {
		if (!Categories.ALL.contains(category)) {
			throw new IllegalArgumentException("not a preset: " + category.name());
		}
		return clearButton + encode(Optional.of(category));
	}
}
