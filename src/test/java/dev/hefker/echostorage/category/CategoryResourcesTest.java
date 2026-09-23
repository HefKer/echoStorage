package dev.hefker.echostorage.category;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.network.chat.contents.TranslatableContents;
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
		String path = "/data/%s/tags/item/%s.json".formatted(category.tag().location().getNamespace(), category.tag().location().getPath());
		JsonObject tag = read(path);

		assertTrue(tag.getAsJsonArray("values").size() > 0, path + " is empty");
	}

	@ParameterizedTest
	@MethodSource("presets")
	void everyPresetHasADisplayName(Category category) throws IOException {
		String key = ((TranslatableContents) category.displayName().getContents()).getKey();

		assertTrue(read("/assets/echostorage/lang/en_us.json").has(key), "no en_us entry for " + key);
	}

	private static JsonObject read(String path) throws IOException {
		try (InputStream in = CategoryResourcesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " is missing");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
