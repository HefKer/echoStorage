package dev.hefker.echostorage.category;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** A preset without its tag file matches nothing, and one without a lang entry shows a raw key. */
class CategoryResourcesTest {
	static List<Category> presets() {
		return Categories.ALL;
	}

	@ParameterizedTest
	@MethodSource("presets")
	void everyPresetShipsTheTagThatBacksIt(Category category) throws IOException {
		ResourceLocation location = category.tag().location();
		String path = "/data/%s/tags/item/%s.json".formatted(location.getNamespace(), location.getPath());
		JsonObject tag = read(path);

		assertTrue(tag.getAsJsonArray("values").size() > 0, path + " is empty");
	}

	@ParameterizedTest
	@MethodSource("presets")
	void everyPresetHasADisplayName(Category category) throws IOException {
		assertTrue(read("/assets/echostorage/lang/en_us.json").has(category.translationKey()),
				"no en_us entry for " + category.translationKey());
	}

	private static JsonObject read(String path) throws IOException {
		try (InputStream in = CategoryResourcesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " is missing");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
