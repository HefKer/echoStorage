package dev.hefker.echostorage.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import dev.hefker.echostorage.VanillaBootstrap;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Quick-stack's transfer, over plain containers. The chest is 27 slots; the player is the 36
 * slots of a vanilla inventory, of which the first nine are the hotbar.
 */
class QuickStackTest {
	private static final int HOTBAR = 9;

	@BeforeAll
	static void bootstrap() {
		VanillaBootstrap.run();
	}

	private final SimpleContainer chest = new SimpleContainer(EchoChestBlockEntity.SLOTS);
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

		QuickStack.run(chest, stack -> stack.is(Items.BREAD), player, HOTBAR, player.getContainerSize());

		assertStack(new ItemStack(Items.BREAD, 5), player.getItem(HOTBAR));
		assertStack(new ItemStack(Items.BREAD, 1), chest.getItem(0));
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
		QuickStack.run(chest, stack -> false, player, HOTBAR, player.getContainerSize());
	}

	private static void assertStack(ItemStack expected, ItemStack actual) {
		assertTrue(ItemStack.matches(expected, actual), "expected " + expected + ", got " + actual);
	}
}
