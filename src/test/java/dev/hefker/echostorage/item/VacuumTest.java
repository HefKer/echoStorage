package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.category.Category;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Vacuuming a picked-up stack into the player's bundles, over a plain 41-slot container. */
class VacuumTest {
	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	/** Tag layers are empty without datapacks, so tests give their Category a later layer. */
	private static final Category ORES = Category.of("ores", stack -> stack.is(Items.IRON_ORE) || stack.is(Items.COAL_ORE));

	private final SimpleContainer player = new SimpleContainer(41);

	@Test
	void aBundleWithNoCategoryVacuumsWhatItAlreadyHolds() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 10)));
		ItemStack pickedUp = new ItemStack(Items.COBBLESTONE, 20);

		Vacuum.run(player, pickedUp);

		assertTrue(pickedUp.isEmpty(), "left over: " + pickedUp);
		assertEquals(30, TestBundles.count(player.getItem(3), Items.COBBLESTONE));
	}

	@Test
	void aBundleWithNoCategoryLeavesWhatItDoesNotHold() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 10)));
		ItemStack pickedUp = new ItemStack(Items.DIAMOND, 2);

		Vacuum.run(player, pickedUp);

		assertEquals(2, pickedUp.getCount());
		assertEquals(0, TestBundles.count(player.getItem(3), Items.DIAMOND));
	}

	@Test
	void anEmptyBundleWithNoCategoryVacuumsNothing() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.empty())));
		ItemStack pickedUp = new ItemStack(Items.DIAMOND, 2);

		Vacuum.run(player, pickedUp);

		assertEquals(2, pickedUp.getCount());
	}

	@Test
	void aBundleWithACategoryVacuumsWhatTheCategoryMatchesEvenIfEmpty() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.of(ORES))));
		ItemStack ore = new ItemStack(Items.COAL_ORE, 5);
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, ore);
		Vacuum.run(player, bread);

		assertTrue(ore.isEmpty(), "left over: " + ore);
		assertEquals(5, bread.getCount());
		assertEquals(5, TestBundles.count(player.getItem(3), Items.COAL_ORE));
	}

	@Test
	void aBundleWithACategoryIgnoresWhatItHoldsOutsideIt() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.of(ORES)), new ItemStack(Items.BREAD, 1)));
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, bread);

		assertEquals(5, bread.getCount());
	}

	@Test
	void aBundleWithTheToggleOffVacuumsNothing() {
		player.setItem(3, TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.COBBLESTONE, 10)));
		ItemStack pickedUp = new ItemStack(Items.COBBLESTONE, 20);

		Vacuum.run(player, pickedUp);

		assertEquals(20, pickedUp.getCount());
		assertEquals(10, TestBundles.count(player.getItem(3), Items.COBBLESTONE));
	}

	@Test
	void whatAFullBundleCannotTakeFallsToTheNextThenStaysPickedUp() {
		ItemStack full = TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 60));
		player.setItem(1, full);
		player.setItem(7, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 1),
				new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 64), new ItemStack(Items.DIRT, 60)));
		ItemStack pickedUp = new ItemStack(Items.COBBLESTONE, 10);

		Vacuum.run(player, pickedUp);

		assertEquals(256, TestBundles.count(player.getItem(1), Items.COBBLESTONE));
		assertEquals(4, TestBundles.count(player.getItem(7), Items.COBBLESTONE));
		assertEquals(3, pickedUp.getCount(), "what no bundle had room for is left to the inventory");
	}

	@Test
	void aBundleIsNeverVacuumedIntoABundle() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.STICK, 1)));
		ItemStack otherBundle = TestBundles.of(EchoBundleSettings.DEFAULT);

		Vacuum.run(player, otherBundle);

		assertEquals(1, otherBundle.getCount());
	}

	private static EchoBundleSettings vacuuming(Optional<Category> category) {
		return new EchoBundleSettings(category, true);
	}
}
