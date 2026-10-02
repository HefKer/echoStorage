package dev.hefker.echostorage.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import dev.hefker.echostorage.category.Category;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Vacuuming a picked-up stack into the player's bundles and Echo Shulker Boxes, over a plain 41-slot container. */
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
	void aBundleWithACategoryAlsoVacuumsWhatItHoldsOutsideIt() {
		player.setItem(3, TestBundles.of(vacuuming(Optional.of(ORES)), new ItemStack(Items.BREAD, 1)));
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, bread);

		assertTrue(bread.isEmpty(), "left over: " + bread);
		assertEquals(6, TestBundles.count(player.getItem(3), Items.BREAD));
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

	// --- a carried Echo Shulker Box ---------------------------------------------------------

	@Test
	void aBoxVacuumsWhatItsCategoryMatchesEvenIfEmpty() {
		player.setItem(3, box(permissive(ORES), true));
		ItemStack ore = new ItemStack(Items.COAL_ORE, 5);
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, ore);
		Vacuum.run(player, bread);

		assertTrue(ore.isEmpty(), "left over: " + ore);
		assertEquals(5, bread.getCount());
		assertStack(new ItemStack(Items.COAL_ORE, 5), boxSlot(player.getItem(3), 0));
	}

	@Test
	void aBoxVacuumsWhatItAlreadyHoldsOutsideItsCategoryOntoItsOwnStack() {
		player.setItem(3, box(permissive(ORES), true, new ItemStack(Items.DIRT), new ItemStack(Items.BREAD, 10)));
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, bread);

		assertTrue(bread.isEmpty(), "left over: " + bread);
		assertStack(new ItemStack(Items.BREAD, 15), boxSlot(player.getItem(3), 1));
	}

	@Test
	void anEmptyBoxWithNoCategoryVacuumsNothing() {
		player.setItem(3, box(EchoChestAssignment.DEFAULT, true));
		ItemStack pickedUp = new ItemStack(Items.DIAMOND, 2);

		Vacuum.run(player, pickedUp);

		assertEquals(2, pickedUp.getCount());
		assertTrue(boxSlot(player.getItem(3), 0).isEmpty(), "the box took a diamond it neither holds nor matches");
	}

	@Test
	void aBoxVacuumsWhatItHoldsOnlyInsideOneOfItsBundlesIntoThatBundle() {
		player.setItem(3, box(EchoChestAssignment.DEFAULT, true, TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.FLINT, 1))));
		ItemStack flint = new ItemStack(Items.FLINT, 4);

		Vacuum.run(player, flint);

		assertTrue(flint.isEmpty(), "left over: " + flint);
		assertEquals(5, TestBundles.count(boxSlot(player.getItem(3), 0), Items.FLINT));
		assertTrue(boxSlot(player.getItem(3), 1).isEmpty(), "the flint went to a slot of its own");
	}

	@Test
	void aBoxTopsUpTheBundlesInsideThatHoldTheItemBeforeItsOwnSlotsWhateverTheirSettings() {
		EchoBundleSettings foodNotVacuuming = new EchoBundleSettings(Optional.of(Category.of("food", stack -> stack.is(Items.BREAD))), false);
		ItemStack holdsNoOre = TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.STICK, 1));
		ItemStack holdsOre = TestBundles.of(foodNotVacuuming, new ItemStack(Items.IRON_ORE, 1));
		player.setItem(3, box(permissive(ORES), true, holdsNoOre, new ItemStack(Items.IRON_ORE, 1), holdsOre));
		ItemStack ore = new ItemStack(Items.IRON_ORE, 6);

		Vacuum.run(player, ore);

		ItemStack box = player.getItem(3);
		assertTrue(ore.isEmpty(), "left over: " + ore);
		assertEquals(7, TestBundles.count(boxSlot(box, 2), Items.IRON_ORE), "the bundle that holds ore is topped up");
		assertStack(new ItemStack(Items.IRON_ORE, 1), boxSlot(box, 1));
		assertEquals(0, TestBundles.count(boxSlot(box, 0), Items.IRON_ORE), "a bundle that held no ore was given some");
	}

	@Test
	void whatTheBundlesInsideCannotTakeGoesToTheBoxsOwnSlots() {
		ItemStack fullOfCobble = TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 60));
		player.setItem(3, box(EchoChestAssignment.DEFAULT, true, fullOfCobble));
		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 10);

		Vacuum.run(player, cobble);

		assertTrue(cobble.isEmpty(), "left over: " + cobble);
		assertEquals(256, TestBundles.count(boxSlot(player.getItem(3), 0), Items.COBBLESTONE));
		assertStack(new ItemStack(Items.COBBLESTONE, 6), boxSlot(player.getItem(3), 1));
	}

	@Test
	void aStrictBoxRefusesAStrayItHoldsInItsSlotsOrItsBundlesAndStillTakesItsCategory() {
		ItemStack flintBundle = TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.FLINT, 1));
		player.setItem(3, box(new EchoChestAssignment(Optional.of(ORES), true), true, new ItemStack(Items.BREAD, 1), flintBundle));
		ItemStack bread = new ItemStack(Items.BREAD, 5);
		ItemStack flint = new ItemStack(Items.FLINT, 5);
		ItemStack ore = new ItemStack(Items.IRON_ORE, 5);

		Vacuum.run(player, bread);
		Vacuum.run(player, flint);
		Vacuum.run(player, ore);

		ItemStack box = player.getItem(3);
		assertEquals(5, bread.getCount(), "the strict box took bread onto the Stray it holds");
		assertEquals(5, flint.getCount(), "the strict box took flint into its bundle");
		assertTrue(ore.isEmpty(), "left over: " + ore);
		assertStack(new ItemStack(Items.BREAD, 1), boxSlot(box, 0));
		assertEquals(1, TestBundles.count(boxSlot(box, 1), Items.FLINT));
		assertStack(new ItemStack(Items.IRON_ORE, 5), boxSlot(box, 2));
	}

	@Test
	void aStrictBoxWithNoCategoryHasNoStrays() {
		player.setItem(3, box(new EchoChestAssignment(Optional.empty(), true), true, new ItemStack(Items.BREAD, 1)));
		ItemStack bread = new ItemStack(Items.BREAD, 5);

		Vacuum.run(player, bread);

		assertTrue(bread.isEmpty(), "left over: " + bread);
		assertStack(new ItemStack(Items.BREAD, 6), boxSlot(player.getItem(3), 0));
	}

	@Test
	void aBoxWithItsToggleOffVacuumsNothing() {
		ItemStack box = box(permissive(ORES), false, new ItemStack(Items.IRON_ORE, 1));
		player.setItem(3, box.copy());
		ItemStack ore = new ItemStack(Items.IRON_ORE, 5);

		Vacuum.run(player, ore);

		assertEquals(5, ore.getCount());
		assertTrue(ItemStack.matches(box, player.getItem(3)), "the box changed: " + player.getItem(3));
	}

	@Test
	void carriedItemsAreVisitedInSlotOrderAndWhatOneCannotTakeFallsToTheNext() {
		ItemStack[] nearlyFull = new ItemStack[EchoShulkerBoxBlockEntity.SLOTS];
		Arrays.fill(nearlyFull, new ItemStack(Items.DIRT, 64));
		nearlyFull[0] = new ItemStack(Items.COBBLESTONE, 60);
		player.setItem(1, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 62)));
		player.setItem(4, box(EchoChestAssignment.DEFAULT, true, nearlyFull));
		player.setItem(7, TestBundles.of(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 1)));
		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 10);

		Vacuum.run(player, cobble);

		assertEquals(256, TestBundles.count(player.getItem(1), Items.COBBLESTONE));
		assertStack(new ItemStack(Items.COBBLESTONE, 64), boxSlot(player.getItem(4), 0));
		assertEquals(5, TestBundles.count(player.getItem(7), Items.COBBLESTONE));
		assertTrue(cobble.isEmpty(), "left over: " + cobble);
	}

	@Test
	void aShulkerBoxIsNeverVacuumedIntoABox() {
		player.setItem(3, box(EchoChestAssignment.DEFAULT, true, new ItemStack(Items.SHULKER_BOX)));
		ItemStack shulkerBox = new ItemStack(Items.SHULKER_BOX);

		Vacuum.run(player, shulkerBox);

		assertEquals(1, shulkerBox.getCount());
	}

	@Test
	void aShulkerBoxIsCarriedStorageWithoutAnotherTestClassBindingTheTag() {
		assertTrue(CarriedStorage.isCarriedStorage(new ItemStack(Items.SHULKER_BOX)), "c:shulker_boxes is not bound");
	}

	@Test
	void anotherModsShulkerBoxIsNeverVacuumedEvenByABundleOrABoxWhoseCategoryMatchesIt() {
		// It fits inside container items, so neither the bundle nor the box would refuse it itself.
		Category boxes = Category.of("boxes", stack -> stack.is(VanillaBootstrap.otherModsShulkerBox()));
		player.setItem(3, TestBundles.of(vacuuming(Optional.of(boxes))));
		player.setItem(4, box(permissive(boxes), true));
		ItemStack bundleBefore = player.getItem(3).copy();
		ItemStack boxBefore = player.getItem(4).copy();
		ItemStack pickedUp = new ItemStack(VanillaBootstrap.otherModsShulkerBox());

		Vacuum.run(player, pickedUp);

		assertEquals(1, pickedUp.getCount(), "the shulker box was Vacuumed");
		assertTrue(ItemStack.matches(bundleBefore, player.getItem(3)), "the bundle changed: " + player.getItem(3));
		assertTrue(ItemStack.matches(boxBefore, player.getItem(4)), "the box changed: " + player.getItem(4));
	}

	@Test
	void aBundleOfEitherKindIsNeverVacuumedIntoABoxWhoseCategoryMatchesBundles() {
		Category bundles = Category.of("bundles", EchoBundleContents::isBundle);
		player.setItem(3, box(permissive(bundles), true));
		ItemStack before = player.getItem(3).copy();
		ItemStack echoBundle = TestBundles.of(EchoBundleSettings.DEFAULT, new ItemStack(Items.FLINT, 1));
		ItemStack vanillaBundle = new ItemStack(Items.BUNDLE);

		Vacuum.run(player, echoBundle);
		Vacuum.run(player, vanillaBundle);

		assertEquals(1, echoBundle.getCount(), "the Echo Bundle went into the box");
		assertEquals(1, vanillaBundle.getCount(), "the vanilla bundle went into the box");
		assertTrue(ItemStack.matches(before, player.getItem(3)), "the box changed: " + player.getItem(3));
	}

	@Test
	void aBundleOfEitherKindIsNeverVacuumedIntoABoxThatAlreadyHoldsThatKind() {
		player.setItem(3, box(EchoChestAssignment.DEFAULT, true, TestBundles.of(EchoBundleSettings.DEFAULT), new ItemStack(Items.BUNDLE)));
		ItemStack before = player.getItem(3).copy();
		ItemStack echoBundle = TestBundles.of(EchoBundleSettings.DEFAULT);
		ItemStack vanillaBundle = new ItemStack(Items.BUNDLE);

		Vacuum.run(player, echoBundle);
		Vacuum.run(player, vanillaBundle);

		assertEquals(1, echoBundle.getCount(), "the Echo Bundle went into the box");
		assertEquals(1, vanillaBundle.getCount(), "the vanilla bundle went into the box");
		assertTrue(ItemStack.matches(before, player.getItem(3)), "the box changed: " + player.getItem(3));
	}

	private static EchoBundleSettings vacuuming(Optional<Category> category) {
		return new EchoBundleSettings(category, true);
	}

	private static EchoChestAssignment permissive(Category category) {
		return new EchoChestAssignment(Optional.of(category), false);
	}

	/**
	 * An Echo Shulker Box as a unit test can make one: the item standing in for it, carrying the
	 * box's components, since the real one is not registered outside a game.
	 */
	private static ItemStack box(EchoChestAssignment assignment, boolean vacuum, ItemStack... slots) {
		ItemStack box = new ItemStack(VanillaBootstrap.echoShulkerBox());
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(slots)));
		if (!assignment.isBlank()) {
			box.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, assignment);
		}
		if (vacuum) {
			box.set(EchoComponents.ECHO_SHULKER_BOX_VACUUM, Unit.INSTANCE);
		}
		return box;
	}

	private static ItemStack boxSlot(ItemStack box, int slot) {
		NonNullList<ItemStack> slots = NonNullList.withSize(EchoShulkerBoxBlockEntity.SLOTS, ItemStack.EMPTY);
		box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);
		return slots.get(slot);
	}

	private static void assertStack(ItemStack expected, ItemStack actual) {
		assertTrue(ItemStack.matches(expected, actual), "expected " + expected + ", got " + actual);
	}
}
