package dev.hefker.echostorage.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.CarriedStorage;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoBundleSettings;
import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Quick-stack's transfer, over plain containers. The chest is 27 slots; the player is the 36
 * slots of a vanilla inventory, of which the first nine are the hotbar.
 */
class QuickStackTest {
	private static final int HOTBAR = 9;
	/** Tag layers are empty without datapacks, so tests give their Category a later layer. */
	private static final Category ORES = Category.of("ores", stack -> stack.is(Items.IRON_ORE) || stack.is(Items.COAL_ORE));

	/**
	 * The Echo Shulker Box is not registered outside a game, so here it is an ender chest: what
	 * makes a stack a shulker box is the tag and the component, which {@link #bootstrap} and
	 * {@link #boxOf} give it. That does not make it one known to have 27 slots: a game test fills
	 * the real one's empty slots.
	 */
	private static Item echoShulkerBox() {
		// Not a constant: Items cannot be touched until bootstrap has run.
		return Items.ENDER_CHEST;
	}

	/** Stands in for another mod's shulker box: in the tag, with nothing to say how many slots it has. */
	private static Item otherModsShulkerBox() {
		return Items.BARREL;
	}

	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
		// No datapack binds tags here, so the one the code under test reads is bound by hand.
		BuiltInRegistries.ITEM.bindTags(Map.of(CarriedStorage.SHULKER_BOXES,
				List.of(Items.SHULKER_BOX.builtInRegistryHolder(), echoShulkerBox().builtInRegistryHolder(),
						otherModsShulkerBox().builtInRegistryHolder())));
	}

	private final SimpleContainer chest = new SimpleContainer(27);
	private final SimpleContainer player = new SimpleContainer(36);

	@Test
	void movesWhatTheChestAlreadyHoldsAndLeavesTheRest() {
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		player.setItem(HOTBAR, new ItemStack(Items.COBBLESTONE, 20));
		player.setItem(HOTBAR + 1, new ItemStack(Items.BREAD, 5));

		quickStack();

		assertStack(new ItemStack(Items.COBBLESTONE, 30), chest.getItem(0));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the cobblestone stayed with the player");
		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR + 1));
	}

	@Test
	void whatDoesNotFitOnItsOwnStacksFillsEmptySlots() {
		chest.setItem(0, new ItemStack(Items.STONE));
		chest.setItem(3, new ItemStack(Items.COBBLESTONE, 60));
		player.setItem(HOTBAR, new ItemStack(Items.COBBLESTONE, 64));

		quickStack();

		assertStack(new ItemStack(Items.COBBLESTONE, 64), chest.getItem(3));
		assertStack(new ItemStack(Items.COBBLESTONE, 60), chest.getItem(1));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the cobblestone stayed with the player");
	}

	@Test
	void whatFindsNoRoomStaysWithThePlayer() {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 62));
		player.setItem(HOTBAR, new ItemStack(Items.COBBLESTONE, 10));

		quickStack();

		assertStack(new ItemStack(Items.COBBLESTONE, 64), chest.getItem(0));
		assertStack(new ItemStack(Items.COBBLESTONE, 8), player.getItem(HOTBAR));
	}

	@Test
	void anItemMatchesWhateverItsComponentsButOnlyStacksWithAnExactMatch() {
		ItemStack named = new ItemStack(Items.PAPER, 3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Deed"));
		chest.setItem(0, named);
		player.setItem(HOTBAR, new ItemStack(Items.PAPER, 5));

		quickStack();

		assertStack(named, chest.getItem(0));
		assertStack(new ItemStack(Items.PAPER, 5), chest.getItem(1));
	}

	// --- through bundles -----------------------------------------------------------------

	@Test
	void anItemHeldOnlyInsideABundleIsMatchedAndTopsThatBundleUp() {
		chest.setItem(5, bundleOf(new ItemStack(Items.IRON_ORE, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 30));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
		assertOnlySlotFilled(5);
	}

	@Test
	void aBundleIsPreferredUntilFullThenTheRestFallsThroughToSlots() {
		chest.setItem(0, new ItemStack(Items.IRON_ORE, 1));
		chest.setItem(5, bundleOf(new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 64),
				new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 60)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 256));
		assertStack(new ItemStack(Items.IRON_ORE, 7), chest.getItem(0));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
	}

	@Test
	void aBundleThatDoesNotHoldTheItemIsLeftAlone() {
		chest.setItem(0, new ItemStack(Items.COAL, 1));
		chest.setItem(5, bundleOf(new ItemStack(Items.IRON_ORE, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.COAL, 10));

		quickStack();

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 10));
		assertStack(new ItemStack(Items.COAL, 11), chest.getItem(0));
	}

	@Test
	void thePlayersOwnBundleNeverMoves() {
		// Tests make bundles out of sticks, so a chest of sticks is a chest that "holds" them.
		chest.setItem(0, new ItemStack(Items.STICK, 1));
		chest.setItem(1, bundleOf());
		player.setItem(HOTBAR, bundleOf(new ItemStack(Items.IRON_ORE, 10)));

		quickStack();

		assertBundleHolds(player.getItem(HOTBAR), new ItemStack(Items.IRON_ORE, 10));
		assertStack(new ItemStack(Items.STICK, 1), chest.getItem(0));
	}

	@Test
	void aStackOfSeveralBundlesIsNeverWrittenInto() {
		// Writing to one component would give every bundle in the stack the new contents.
		ItemStack pair = bundleOf(new ItemStack(Items.IRON_ORE, 10));
		pair.setCount(2);
		chest.setItem(5, pair);
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertEquals(2, chest.getItem(5).getCount());
		assertBundleHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 10));
		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
	}

	// --- through shulker boxes -----------------------------------------------------------

	@Test
	void anItemHeldOnlyInsideAShulkerBoxIsMatchedAndTopsThatBoxUp() {
		chest.setItem(5, boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 30));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
		assertOnlySlotFilled(5);
	}

	@Test
	void anEchoShulkerBoxIsReadAndToppedUpAsAVanillaOneIs() {
		chest.setItem(5, boxOf(echoShulkerBox(), new ItemStack(Items.IRON_ORE, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 30));
		assertOnlySlotFilled(5);
	}

	@Test
	void aShulkerBoxThatDoesNotHoldTheItemIsLeftAloneWhateverItsOwnCategory() {
		ItemStack box = boxOf(echoShulkerBox(), new ItemStack(Items.COAL_ORE, 10));
		box.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, new EchoChestAssignment(Optional.of(ORES), false));
		chest.setItem(5, box);
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack(Optional.of(ORES));

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.COAL_ORE, 10));
		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
	}

	@Test
	void aShulkerBoxIsToppedUpWithAStrayItHoldsWhateverItsOwnStrictness() {
		ItemStack box = boxOf(echoShulkerBox(), new ItemStack(Items.BREAD, 10));
		box.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, new EchoChestAssignment(Optional.of(ORES), true));
		chest.setItem(5, box);
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 20));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.BREAD, 30));
		assertOnlySlotFilled(5);
	}

	@Test
	void aShulkerBoxFillsItsMatchingStacksThenItsEmptySlots() {
		chest.setItem(5, boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 60), new ItemStack(Items.BREAD, 1)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.BREAD, 1), new ItemStack(Items.IRON_ORE, 6));
		assertOnlySlotFilled(5);
	}

	@Test
	void aFullShulkerBoxLetsTheRestFallThroughToChestSlots() {
		ItemStack[] full = new ItemStack[27];
		Arrays.fill(full, new ItemStack(Items.IRON_ORE, 64));
		full[26] = new ItemStack(Items.IRON_ORE, 60);
		chest.setItem(5, boxOf(Items.SHULKER_BOX, full));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		full[26] = new ItemStack(Items.IRON_ORE, 64);
		assertBoxHolds(chest.getItem(5), full);
		assertStack(new ItemStack(Items.IRON_ORE, 6), chest.getItem(0));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
	}

	@Test
	void aBundleAndAShulkerBoxThatBothHoldTheItemAreToppedUpInSlotOrder() {
		// Each has room for 4 more; the chest's own slot 0 comes after both.
		ItemStack[] nearlyFull = new ItemStack[27];
		Arrays.fill(nearlyFull, new ItemStack(Items.IRON_ORE, 64));
		nearlyFull[26] = new ItemStack(Items.IRON_ORE, 60);
		chest.setItem(3, boxOf(Items.SHULKER_BOX, nearlyFull));
		chest.setItem(5, bundleOf(new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 64),
				new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 60)));
		chest.setItem(7, boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 1)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		nearlyFull[26] = new ItemStack(Items.IRON_ORE, 64);
		assertBoxHolds(chest.getItem(3), nearlyFull);
		assertBundleHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 256));
		assertBoxHolds(chest.getItem(7), new ItemStack(Items.IRON_ORE, 3));
		assertTrue(chest.getItem(0).isEmpty(), "a chest slot took " + chest.getItem(0));
	}

	@Test
	void aBundleInsideAShulkerBoxIsNeverWrittenInto() {
		ItemStack inner = bundleOf(new ItemStack(Items.IRON_ORE, 10));
		chest.setItem(5, boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 1), inner));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 21), inner);
		assertOnlySlotFilled(5);
	}

	@Test
	void anItemHeldOnlyInABundleInsideAShulkerBoxIsNotMatched() {
		ItemStack box = boxOf(Items.SHULKER_BOX, bundleOf(new ItemStack(Items.IRON_ORE, 10)));
		chest.setItem(5, box.copy());
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertStack(new ItemStack(Items.IRON_ORE, 20), player.getItem(HOTBAR));
		assertStack(box, chest.getItem(5));
		assertOnlySlotFilled(5);
	}

	@Test
	void thePlayersOwnShulkerBoxNeverMovesEvenIntoAChestWhoseCategoryItsContentsMatch() {
		ItemStack vanilla = boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 10));
		ItemStack echo = boxOf(echoShulkerBox(), new ItemStack(Items.IRON_ORE, 10));
		// An ender chest carries no contents, so the chest "holds" that item as well as wanting the ore.
		chest.setItem(0, new ItemStack(echoShulkerBox()));
		player.setItem(HOTBAR, vanilla.copy());
		player.setItem(HOTBAR + 1, echo.copy());

		quickStack(Optional.of(ORES));

		assertStack(vanilla, player.getItem(HOTBAR));
		assertStack(echo, player.getItem(HOTBAR + 1));
		assertTrue(chest.getItem(1).isEmpty(), "the chest took " + chest.getItem(1));
	}

	@Test
	void aStackOfSeveralShulkerBoxesIsNeverWrittenInto() {
		ItemStack pair = boxOf(echoShulkerBox(), new ItemStack(Items.IRON_ORE, 10));
		pair.setCount(2);
		chest.setItem(5, pair.copy());
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();

		assertStack(pair, chest.getItem(5));
		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
	}

	@Test
	void aShulkerBoxWhoseLootIsUnrolledIsNeitherReadNorWritten() {
		ItemStack unrolled = boxOf(Items.SHULKER_BOX, new ItemStack(Items.IRON_ORE, 10));
		unrolled.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(BuiltInLootTables.SIMPLE_DUNGEON, 0));
		chest.setItem(5, unrolled.copy());
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack();
		assertStack(new ItemStack(Items.IRON_ORE, 20), player.getItem(HOTBAR));

		chest.setItem(0, new ItemStack(Items.IRON_ORE, 1));
		quickStack();

		assertStack(unrolled, chest.getItem(5));
		assertStack(new ItemStack(Items.IRON_ORE, 21), chest.getItem(0));
	}

	@Test
	void aShulkerBoxListingMoreSlotsThanItHasIsToppedUpButGainsNoSlot() {
		ItemStack[] big = new ItemStack[29];
		Arrays.fill(big, new ItemStack(Items.BREAD, 1));
		big[0] = new ItemStack(Items.IRON_ORE, 60);
		big[1] = ItemStack.EMPTY;
		chest.setItem(5, boxOf(Items.SHULKER_BOX, big));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		big[0] = new ItemStack(Items.IRON_ORE, 64);
		assertBoxHolds(chest.getItem(5), big);
		assertStack(new ItemStack(Items.IRON_ORE, 6), chest.getItem(0));
	}

	// --- through shulker boxes of unknown size ---------------------------------------------

	@Test
	void aShulkerBoxOfUnknownSizeHasItsStackToppedUpAndTheRestGoesToChestSlots() {
		chest.setItem(5, boxOf(otherModsShulkerBox(), new ItemStack(Items.IRON_ORE, 60)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		assertBoxHolds(chest.getItem(5), new ItemStack(Items.IRON_ORE, 64));
		assertStack(new ItemStack(Items.IRON_ORE, 6), chest.getItem(0));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
	}

	@Test
	void aShulkerBoxOfUnknownSizeKeepsItsSlotCountAndEveryStackWhereItWas() {
		// Slots 0 and 2 are empty and stay so; nothing is written past slot 3.
		ItemStack box = boxOf(otherModsShulkerBox(), ItemStack.EMPTY, new ItemStack(Items.IRON_ORE, 60),
				ItemStack.EMPTY, new ItemStack(Items.BREAD, 1));
		chest.setItem(5, box);
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		assertBoxHolds(chest.getItem(5), ItemStack.EMPTY, new ItemStack(Items.IRON_ORE, 64),
				ItemStack.EMPTY, new ItemStack(Items.BREAD, 1));
		assertStack(new ItemStack(Items.IRON_ORE, 6), chest.getItem(0));
	}

	@Test
	void aShulkerBoxOfUnknownSizeOnlyTopsUpAStackWithTheSameComponents() {
		ItemStack named = new ItemStack(Items.PAPER, 3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Deed"));
		ItemStack box = boxOf(otherModsShulkerBox(), named);
		chest.setItem(5, box.copy());
		player.setItem(HOTBAR, new ItemStack(Items.PAPER, 5));

		quickStack();

		assertStack(box, chest.getItem(5));
		assertStack(new ItemStack(Items.PAPER, 5), chest.getItem(0));
	}

	@Test
	void aShulkerBoxOfUnknownSizeHoldingOnlyFullStacksIsNotRewritten() {
		ItemStack box = boxOf(otherModsShulkerBox(), new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 64));
		// Told apart from a box written back with the same contents by being the very same stack.
		chest.setItem(5, box);
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		assertSame(box, chest.getItem(5));
		assertBoxHolds(box, new ItemStack(Items.IRON_ORE, 64), new ItemStack(Items.IRON_ORE, 64));
		assertStack(new ItemStack(Items.IRON_ORE, 10), chest.getItem(0));
	}

	@Test
	void aShulkerBoxOfUnknownSizeIsToppedUpPastSlot27WithNothingLostOrMoved() {
		ItemStack[] big = new ItemStack[30];
		Arrays.fill(big, new ItemStack(Items.BREAD, 1));
		big[27] = ItemStack.EMPTY;
		big[28] = new ItemStack(Items.IRON_ORE, 60);
		chest.setItem(5, boxOf(otherModsShulkerBox(), big));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 10));

		quickStack();

		big[28] = new ItemStack(Items.IRON_ORE, 64);
		assertBoxHolds(chest.getItem(5), big);
		assertStack(new ItemStack(Items.IRON_ORE, 6), chest.getItem(0));
	}

	// --- what is taken -------------------------------------------------------------------

	@Test
	void onlyTheGivenSlotsAreTakenFrom() {
		chest.setItem(0, new ItemStack(Items.TORCH, 1));
		player.setItem(0, new ItemStack(Items.TORCH, 20));

		quickStack();

		assertStack(new ItemStack(Items.TORCH, 20), player.getItem(0));
		assertStack(new ItemStack(Items.TORCH, 1), chest.getItem(0));
	}

	@Test
	void whatTheChestRefusesStaysWithThePlayerEvenIfItHoldsSome() {
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 5));

		QuickStack.run(chest, QuickStack.holds(chest), stack -> stack.is(Items.BREAD), player, HOTBAR, player.getContainerSize());

		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR));
		assertStack(new ItemStack(Items.BREAD, 1), chest.getItem(0));
	}

	// --- the chest's Category -------------------------------------------------------------

	@Test
	void anEmptyChestWithACategoryTakesWhatIsInItAndNothingElse() {
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));
		player.setItem(HOTBAR + 1, new ItemStack(Items.BREAD, 5));

		quickStack(Optional.of(ORES));

		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
		assertTrue(player.getItem(HOTBAR).isEmpty(), "the ore stayed with the player");
		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR + 1));
		assertOnlySlotFilled(0);
	}

	@Test
	void aChestWithACategoryStillTakesWhatItHoldsOutsideIt() {
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 5));

		quickStack(Optional.of(ORES));

		assertStack(new ItemStack(Items.BREAD, 6), chest.getItem(0));
	}

	@Test
	void aStrayTheChestHoldsIsLeftWithThePlayerWhenTheChestRefusesIt() {
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 5));
		player.setItem(HOTBAR + 1, new ItemStack(Items.IRON_ORE, 5));

		QuickStack.run(chest, QuickStack.wanted(chest, Optional.of(ORES)), stack -> !ORES.matches(stack),
				player, HOTBAR, player.getContainerSize());

		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR));
		assertStack(new ItemStack(Items.IRON_ORE, 5), chest.getItem(1));
	}

	@Test
	void anItemInTheCategoryThatNoBundleHoldsGoesToSlotsNotIntoABundle() {
		chest.setItem(5, bundleOf(new ItemStack(Items.COAL_ORE, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack(Optional.of(ORES));

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.COAL_ORE, 10));
		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
	}

	@Test
	void aBundleIsToppedUpWithWhatItHoldsWhateverTheChestsCategory() {
		chest.setItem(5, bundleOf(new ItemStack(Items.BREAD, 10)));
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 20));

		quickStack(Optional.of(ORES));

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.BREAD, 30));
		assertOnlySlotFilled(5);
	}

	@Test
	void aBundleIsNeverStartedOnAnItemItDoesNotHoldWhateverItsOwnCategory() {
		ItemStack bundle = bundleOf(new ItemStack(Items.COAL_ORE, 10));
		bundle.set(EchoComponents.ECHO_BUNDLE_SETTINGS, new EchoBundleSettings(Optional.of(ORES), false));
		chest.setItem(5, bundle);
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		quickStack(Optional.of(ORES));

		assertBundleHolds(chest.getItem(5), new ItemStack(Items.COAL_ORE, 10));
		assertStack(new ItemStack(Items.IRON_ORE, 20), chest.getItem(0));
	}

	// --- one match at a time, for global Quick-stack's passes -----------------------------

	@Test
	void matchingOnlyWhatTheChestHoldsIgnoresItsCategory() {
		player.setItem(HOTBAR, new ItemStack(Items.IRON_ORE, 20));

		QuickStack.run(chest, QuickStack.holds(chest), stack -> false, player, HOTBAR, player.getContainerSize());

		assertStack(new ItemStack(Items.IRON_ORE, 20), player.getItem(HOTBAR));
		assertTrue(chest.isEmpty(), "the chest took " + chest.getItem(0));
	}

	@Test
	void matchingOnlyTheCategoryIgnoresWhatTheChestHolds() {
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		player.setItem(HOTBAR, new ItemStack(Items.BREAD, 5));
		player.setItem(HOTBAR + 1, new ItemStack(Items.IRON_ORE, 5));

		QuickStack.run(chest, QuickStack.inCategory(Optional.of(ORES)), stack -> false, player, HOTBAR, player.getContainerSize());

		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR));
		assertStack(new ItemStack(Items.IRON_ORE, 5), chest.getItem(1));
	}

	private void assertOnlySlotFilled(int filled) {
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			assertEquals(slot == filled, !chest.getItem(slot).isEmpty(), "slot " + slot + " holds " + chest.getItem(slot));
		}
	}

	/** An Echo Bundle as tests can make one: the component is what makes a stack a bundle. */
	private static ItemStack bundleOf(ItemStack... contents) {
		EchoBundleContents.Mutable mutable = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);
		for (ItemStack stack : contents) {
			mutable.tryInsert(stack.copy());
		}
		ItemStack bundle = new ItemStack(Items.STICK);
		bundle.set(EchoComponents.ECHO_BUNDLE_CONTENTS, mutable.toImmutable());
		return bundle;
	}

	/** A shulker box item of the given kind, its slots filled in order from the first. */
	private static ItemStack boxOf(Item kind, ItemStack... contents) {
		ItemStack box = new ItemStack(kind);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
		return box;
	}

	/** The box's slots, exactly: stack by stack from the first, with nothing after them. */
	private static void assertBoxHolds(ItemStack box, ItemStack... expected) {
		ItemContainerContents contents = box.get(DataComponents.CONTAINER);
		assertNotNull(contents, box + " carries no contents");
		assertEquals(ItemContainerContents.fromItems(List.of(expected)), contents);
	}

	/** Totals per item, so the assertion does not care how the bundle split its entries. */
	private static void assertBundleHolds(ItemStack bundle, ItemStack... expected) {
		EchoBundleContents contents = bundle.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		assertNotNull(contents, bundle + " is not a bundle");
		Map<Item, Integer> actualTotals = new HashMap<>();
		contents.items().forEach(stack -> actualTotals.merge(stack.getItem(), stack.getCount(), Integer::sum));
		Map<Item, Integer> expectedTotals = new HashMap<>();
		for (ItemStack stack : expected) {
			expectedTotals.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		assertEquals(expectedTotals, actualTotals);
	}

	private void quickStack() {
		quickStack(Optional.empty());
	}

	private void quickStack(Optional<Category> category) {
		QuickStack.run(chest, QuickStack.wanted(chest, category), stack -> false, player, HOTBAR, player.getContainerSize());
	}

	private static void assertStack(ItemStack expected, ItemStack actual) {
		assertTrue(ItemStack.matches(expected, actual), "expected " + expected + ", got " + actual);
	}
}
