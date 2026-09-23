package dev.hefker.echostorage.gametest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The tag layer against real datapacks: vanilla's tags, Fabric's convention tags and the
 * shipped {@code echostorage:category/*} tags, plus this source set's own datapack standing in
 * for a pack author's (see {@code src/gametest/resources/data}).
 */
public class CategoryGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPresetHoldsTheVanillaItemsItIsNamedFor(GameTestHelper helper) {
		Map<Category, List<Item>> expected = Map.of(
				Categories.FOOD, List.of(Items.BREAD, Items.COOKED_BEEF, Items.APPLE),
				Categories.ORES, List.of(Items.IRON_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.NETHER_QUARTZ_ORE),
				Categories.MATERIALS, List.of(Items.IRON_INGOT, Items.RAW_GOLD, Items.DIAMOND, Items.REDSTONE, Items.GOLD_NUGGET),
				Categories.STORAGE_BLOCKS, List.of(Items.IRON_BLOCK, Items.RAW_COPPER_BLOCK),
				Categories.CROPS, List.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.CARROT),
				Categories.DYES, List.of(Items.RED_DYE, Items.BLACK_DYE),
				Categories.WOOL, List.of(Items.WHITE_WOOL, Items.PINK_WOOL),
				Categories.TOOLS, List.of(Items.IRON_PICKAXE, Items.DIAMOND_SWORD, Items.SHEARS),
				Categories.ARMOR, List.of(Items.IRON_CHESTPLATE, Items.LEATHER_BOOTS));

		helper.assertValueEqual(expected.keySet(), Set.copyOf(Categories.ALL), "presets covered");
		expected.forEach((category, items) -> items.forEach(item ->
				helper.assertTrue(category.matches(new ItemStack(item)), category.name() + " should hold " + item)));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatTagsCannotDescribeFallsIntoNoPreset(GameTestHelper helper) {
		// Wood, decorative stone, light sources and redstone wait for the predicate layer.
		for (Item item : List.of(Items.OAK_PLANKS, Items.POLISHED_ANDESITE, Items.TORCH, Items.REPEATER)) {
			for (Category category : Categories.ALL) {
				helper.assertFalse(category.matches(new ItemStack(item)), category.name() + " should not hold " + item);
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDatapackCanAddToACategory(GameTestHelper helper) {
		// Added by this source set's data/echostorage/tags/item/category/food.json.
		helper.assertTrue(Categories.FOOD.matches(new ItemStack(Items.FLINT)), "food should hold what a datapack added");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anOldModsFlatConventionTagStillReachesItsCategory(GameTestHelper helper) {
		// Stands in for a mod that only tags c:tin_ingots, the v1 name, and never c:ingots.
		helper.assertTrue(Categories.MATERIALS.matches(new ItemStack(Items.STICK)), "materials should hold c:tin_ingots");
		helper.succeed();
	}
}
