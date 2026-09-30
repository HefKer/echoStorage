package dev.hefker.echostorage.gametest;

import static dev.hefker.echostorage.gametest.EchoChestTests.CHEST;
import static dev.hefker.echostorage.gametest.EchoChestTests.FIRST_MAIN_INVENTORY_SLOT;
import static dev.hefker.echostorage.gametest.EchoChestTests.assertStack;
import static dev.hefker.echostorage.gametest.EchoChestTests.bundleOf;
import static dev.hefker.echostorage.gametest.EchoChestTests.menu;
import static dev.hefker.echostorage.gametest.EchoChestTests.openedBy;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.withConfig;

import java.util.Collection;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.gametest.EchoChestTests.ChestKind;
import dev.hefker.echostorage.gametest.EchoChestTests.EveryChestKind;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The quick-stack button on an open Echo Chest, against a real chest and player. The button is
 * the only thing that writes into a bundle inside a chest (ADR-0007); shift-click never does.
 */
public class EchoChestQuickStackGameTest implements FabricGameTest {
	@GameTestGenerator
	public Collection<TestFunction> everyChestKind() {
		return EchoChestTests.forEveryKind(EchoChestQuickStackGameTest.class);
	}

	private static final int HOTBAR_SLOT = 0;

	@EveryChestKind
	public void theButtonPutsAwayWhatTheChestHoldsFromTheMainInventoryNotTheHotbar(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 20));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.BREAD, 5));
		player.getInventory().setItem(HOTBAR_SLOT, new ItemStack(Items.COBBLESTONE, 7));

		helper.assertTrue(quickStack(player), "button handled");

		assertStack(helper, new ItemStack(Items.COBBLESTONE, 30), chest.getItem(0), "chest");
		helper.assertTrue(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT).isEmpty(), "the cobblestone stayed");
		assertStack(helper, new ItemStack(Items.BREAD, 5), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT + 1),
				"what the chest does not hold");
		assertStack(helper, new ItemStack(Items.COBBLESTONE, 7), player.getInventory().getItem(HOTBAR_SLOT), "the hotbar");
		helper.succeed();
	}

	@EveryChestKind
	public void theButtonTopsUpABundleWhereAShiftClickFillsASlot(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		chest.setItem(0, bundleOf(new ItemStack(Items.IRON_ORE, 10)));
		ServerPlayer player = openedBy(helper, chest);

		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.IRON_ORE, 20));
		menu(player).quickMoveStack(player, menu(player).chestSlots());
		assertBundleHolds(helper, chest.getItem(0), 10, "after a shift-click");
		assertStack(helper, new ItemStack(Items.IRON_ORE, 20), chest.getItem(1), "the shift-clicked slot");

		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.IRON_ORE, 20));
		quickStack(player);
		assertBundleHolds(helper, chest.getItem(0), 30, "after the button");
		assertStack(helper, new ItemStack(Items.IRON_ORE, 20), chest.getItem(1), "the slot after the button");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theButtonOnAStrictChestLeavesAStrayItAlreadyHoldsWithThePlayer(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.setStrict(true);
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		chest.setItem(1, new ItemStack(Items.IRON_ORE, 1));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.BREAD, 5));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.IRON_ORE, 5));

		quickStack(player);

		assertStack(helper, new ItemStack(Items.BREAD, 5), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT), "the stray");
		assertStack(helper, new ItemStack(Items.IRON_ORE, 6), chest.getItem(1), "the match");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theButtonOnAPermissiveChestTopsUpAStrayItAlreadyHolds(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.BREAD, 5));

		quickStack(player);

		assertStack(helper, new ItemStack(Items.BREAD, 6), chest.getItem(0), "the stray");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theButtonFillsAnEmptyChestWithWhatIsInItsCategoryAndNothingElse(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.IRON_ORE, 20));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.BREAD, 5));

		quickStack(player);

		assertStack(helper, new ItemStack(Items.IRON_ORE, 20), chest.getItem(0), "chest");
		helper.assertTrue(chest.getItem(1).isEmpty(), "the chest also took " + chest.getItem(1));
		assertStack(helper, new ItemStack(Items.BREAD, 5), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT + 1),
				"what is outside the Category");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theButtonWorksOnAChestWithNoCategory(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.setStrict(true);
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.BREAD, 5));

		quickStack(player);

		assertStack(helper, new ItemStack(Items.BREAD, 6), chest.getItem(0), "chest");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theButtonMovesNothingWithChestQuickStackSwitchedOff(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 20));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.IRON_ORE, 5));

		withConfig(EchoConfig.DEFAULTS.withChestQuickStack(false), () -> quickStack(player));

		assertStack(helper, new ItemStack(Items.COBBLESTONE, 10), chest.getItem(0), "chest");
		helper.assertTrue(chest.getItem(1).isEmpty(), "the chest took " + chest.getItem(1));
		assertStack(helper, new ItemStack(Items.COBBLESTONE, 20), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT),
				"what the chest holds");
		assertStack(helper, new ItemStack(Items.IRON_ORE, 5), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT + 1),
				"what is in its Category");
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	private static boolean quickStack(ServerPlayer player) {
		EchoChestMenu menu = menu(player);
		boolean handled = menu.clickMenuButton(player, EchoChestMenu.QUICK_STACK_BUTTON);
		menu.broadcastChanges();
		return handled;
	}

	/** How many iron ore the bundle holds, however its entries are split. */
	private static void assertBundleHolds(GameTestHelper helper, ItemStack bundle, int ironOre, String what) {
		helper.assertTrue(bundle.is(EchoItems.ECHO_BUNDLE), what + ": not a bundle, " + bundle);
		int held = 0;
		for (ItemStack inside : bundle.getOrDefault(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY).items()) {
			helper.assertTrue(inside.is(Items.IRON_ORE), what + ": the bundle holds " + inside);
			held += inside.getCount();
		}
		helper.assertValueEqual(held, ironOre, what + ": iron ore in the bundle");
	}
}
