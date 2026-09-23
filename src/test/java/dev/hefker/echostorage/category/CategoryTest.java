package dev.hefker.echostorage.category;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hefker.echostorage.VanillaBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CategoryTest {
	@BeforeAll
	static void bootstrap() {
		// No datapacks load here, so every tag is empty: only the layers after the tag can match.
		VanillaBootstrap.run();
	}

	@Test
	void aCategoryMatchesWhatALaterLayerMatches() {
		Category food = Category.of("food", stack -> stack.is(Items.BREAD));

		assertTrue(food.matches(new ItemStack(Items.BREAD)));
		assertFalse(food.matches(new ItemStack(Items.STONE)));
	}
}
