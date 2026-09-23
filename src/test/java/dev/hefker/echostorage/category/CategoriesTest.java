package dev.hefker.echostorage.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonPrimitive;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class CategoriesTest {
	@Test
	void v1ShipsOnlyTheCategoriesTagsDoWell() {
		assertEquals(
				List.of("food", "ores", "materials", "storage_blocks", "crops", "dyes", "wool", "tools", "armor"),
				Categories.ALL.stream().map(Category::name).toList());
	}

	@ParameterizedTest
	@ValueSource(strings = {"wood", "stone", "nature", "lighting", "redstone"})
	void categoriesThatWaitForTheirPredicateLayerDoNotShip(String name) {
		assertTrue(Categories.byName(name).isEmpty());
	}

	@Test
	void aCategoryIsFoundByItsName() {
		assertEquals(Categories.FOOD, Categories.byName("food").orElseThrow());
	}

	@Test
	void aCategoryIsSavedAsItsName() {
		assertEquals(new JsonPrimitive("ores"), Categories.CODEC.encodeStart(JsonOps.INSTANCE, Categories.ORES).getOrThrow());
		assertEquals(Categories.ORES, Categories.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("ores")).getOrThrow());
	}

	@Test
	void aSavedCategoryThatNoLongerShipsFailsToLoad() {
		assertTrue(Categories.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("wood")).isError());
	}
}
