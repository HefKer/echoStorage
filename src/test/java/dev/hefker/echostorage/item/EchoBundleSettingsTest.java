package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.category.Categories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EchoBundleSettingsTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	@Test
	void aCategoryAndTheVacuumToggleSurviveASave() {
		EchoBundleSettings settings = new EchoBundleSettings(Optional.of(Categories.ORES), true);

		assertEquals(settings, load(save(settings)));
	}

	@Test
	void aCategoryAndTheVacuumToggleSurviveTheNetwork() {
		EchoBundleSettings settings = new EchoBundleSettings(Optional.of(Categories.WOOL), true);

		assertEquals(settings, CodecRoundTrip.of(EchoBundleSettings.STREAM_CODEC, settings));
		assertEquals(EchoBundleSettings.DEFAULT, CodecRoundTrip.of(EchoBundleSettings.STREAM_CODEC, EchoBundleSettings.DEFAULT));
	}

	@Test
	void theDefaultIsNoCategoryAndNoVacuum() {
		assertEquals(new EchoBundleSettings(Optional.empty(), false), EchoBundleSettings.DEFAULT);
		assertEquals(EchoBundleSettings.DEFAULT, load("{}"));
	}

	@Test
	void aCategoryThatNoLongerShipsLoadsAsNoneAndKeepsTheToggle() {
		assertEquals(new EchoBundleSettings(Optional.empty(), true), load("{\"category\": \"lighting\", \"vacuum\": true}"));
	}

	private static String save(EchoBundleSettings settings) {
		return EchoBundleSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().toString();
	}

	private static EchoBundleSettings load(String json) {
		return EchoBundleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}
}
