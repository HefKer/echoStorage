package dev.hefker.echostorage.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.hefker.echostorage.CodecRoundTrip;
import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.category.Categories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EchoChestAssignmentTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	@Test
	void aCategoryAndStrictnessSurviveASave() {
		EchoChestAssignment assignment = new EchoChestAssignment(Optional.of(Categories.ORES), true);

		assertEquals(assignment, load(save(assignment)));
	}

	@Test
	void aCategoryAndStrictnessSurviveTheNetwork() {
		EchoChestAssignment assignment = new EchoChestAssignment(Optional.of(Categories.WOOL), true);

		assertEquals(assignment, CodecRoundTrip.of(EchoChestAssignment.STREAM_CODEC, assignment));
		assertEquals(EchoChestAssignment.NONE, CodecRoundTrip.of(EchoChestAssignment.STREAM_CODEC, EchoChestAssignment.NONE));
	}

	@Test
	void theCategoryIsSavedByName() {
		assertEquals("{\"category\":\"ores\"}", save(new EchoChestAssignment(Optional.of(Categories.ORES), false)));
	}

	@Test
	void nothingSetIsNoCategoryAndPermissive() {
		assertEquals(new EchoChestAssignment(Optional.empty(), false), EchoChestAssignment.NONE);
		assertEquals(EchoChestAssignment.NONE, load("{}"));
		assertTrue(EchoChestAssignment.NONE.isBlank());
	}

	@Test
	void eitherACategoryOrStrictnessIsSomethingSet() {
		assertFalse(new EchoChestAssignment(Optional.of(Categories.ORES), false).isBlank());
		assertFalse(new EchoChestAssignment(Optional.empty(), true).isBlank());
	}

	@Test
	void aCategoryThatNoLongerShipsLoadsAsNoneAndKeepsStrictness() {
		assertEquals(new EchoChestAssignment(Optional.empty(), true), load("{\"category\": \"wood\", \"strict\": true}"));
	}

	private static String save(EchoChestAssignment assignment) {
		return EchoChestAssignment.CODEC.encodeStart(JsonOps.INSTANCE, assignment).getOrThrow().toString();
	}

	private static EchoChestAssignment load(String json) {
		return EchoChestAssignment.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}
}
