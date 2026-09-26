package dev.hefker.echostorage.gametest;

import static dev.hefker.echostorage.gametest.EchoChestTests.assertStack;
import static dev.hefker.echostorage.gametest.EchoChestTests.bundleOf;

import java.util.Optional;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.item.EchoBundleSettings;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoBundleMenu;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Echo Bundle's three conveniences against a real player and world, each with its config
 * switch off as well as on, and the bundle's own screen that sets its Category and vacuum toggle.
 *
 * <p>Tests that change the config restore it before returning; they run on the server thread
 * start to finish, so no other test sees the change.
 */
public class EchoBundleConveniencesGameTest implements FabricGameTest {
	/** A block to click on; whatever is placed goes on its top face. */
	private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
	private static final BlockPos ABOVE_FLOOR = FLOOR.above();
	private static final int BUNDLE_SLOT = 9;

	// --- vacuum ----------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void aVacuumingBundlePicksUpWhatItHoldsBeforeTheInventoryDoes(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(BUNDLE_SLOT, bundle(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 10)));

		ItemEntity dropped = drop(helper, player, new ItemStack(Items.COBBLESTONE, 20));

		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.COBBLESTONE), 30, "cobblestone in the bundle");
		helper.assertFalse(inventoryHoldsLoose(player, Items.COBBLESTONE), "cobblestone reached the inventory");
		helper.assertTrue(dropped.isRemoved(), "the item entity was picked up");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBundleWithACategoryPicksUpWhatItMatchesAndLeavesTheRest(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(BUNDLE_SLOT, bundle(vacuuming(Optional.of(Categories.ORES))));

		drop(helper, player, new ItemStack(Items.IRON_ORE, 5));
		drop(helper, player, new ItemStack(Items.BREAD, 5));

		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.IRON_ORE), 5, "ore in the bundle");
		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.BREAD), 0, "bread in the bundle");
		helper.assertTrue(inventoryHoldsLoose(player, Items.BREAD), "the bread went to the inventory");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBundleIsNotHungryUntilItsToggleIsOn(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(BUNDLE_SLOT, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.DIAMOND, 1)));

		drop(helper, player, new ItemStack(Items.DIAMOND, 3));

		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.DIAMOND), 1, "diamonds in the bundle");
		helper.assertTrue(inventoryHoldsLoose(player, Items.DIAMOND), "the diamonds went to the inventory");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatTheBundleHasNoRoomForGoesToTheInventory(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(BUNDLE_SLOT, bundle(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 64),
				new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 60)));

		ItemEntity dropped = drop(helper, player, new ItemStack(Items.COBBLESTONE, 10));

		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.COBBLESTONE), 256, "cobblestone in the bundle");
		helper.assertValueEqual(player.getInventory().countItem(Items.COBBLESTONE), 6, "loose cobblestone");
		helper.assertTrue(dropped.isRemoved(), "the item entity was picked up");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noBundleVacuumsWithTheConfigSwitchOff(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(BUNDLE_SLOT, bundle(vacuuming(Optional.empty()), new ItemStack(Items.COBBLESTONE, 10)));

		withConfig(EchoConfig.DEFAULTS.withBundleVacuum(false), () -> drop(helper, player, new ItemStack(Items.COBBLESTONE, 20)));

		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.COBBLESTONE), 10, "cobblestone in the bundle");
		helper.assertTrue(inventoryHoldsLoose(player, Items.COBBLESTONE), "the cobblestone went to the inventory");
		helper.succeed();
	}

	// --- refill ----------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void placingTheLastBlockInHandPullsTheNextStackFromABundle(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 1));
		player.getInventory().setItem(BUNDLE_SLOT, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 64),
				new ItemStack(Items.STONE, 10)));

		useOnFloor(helper, player);

		helper.assertBlockPresent(Blocks.STONE, ABOVE_FLOOR);
		assertStack(helper, new ItemStack(Items.STONE, 64), player.getMainHandItem(), "the hand");
		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.STONE), 10, "stone left in the bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void placingABlockThatIsNotTheLastLeavesTheBundleAlone(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
		player.getInventory().setItem(BUNDLE_SLOT, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 10)));

		useOnFloor(helper, player);

		assertStack(helper, new ItemStack(Items.STONE, 4), player.getMainHandItem(), "the hand");
		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.STONE), 10, "stone in the bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noRefillWithTheConfigSwitchOff(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 1));
		player.getInventory().setItem(BUNDLE_SLOT, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 10)));

		withConfig(EchoConfig.DEFAULTS.withBundleRefill(false), () -> useOnFloor(helper, player));

		helper.assertBlockPresent(Blocks.STONE, ABOVE_FLOOR);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the hand was refilled: " + player.getMainHandItem());
		helper.assertValueEqual(count(player.getInventory().getItem(BUNDLE_SLOT), Items.STONE), 10, "stone in the bundle");
		helper.succeed();
	}

	// --- place from the bundle -------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void usingTheBundleOnABlockPlacesTheBlockMostRecentlyPutIn(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		// Bread first, so the most recent block is not simply the most recent entry.
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT,
				new ItemStack(Items.STONE, 3), new ItemStack(Items.OAK_PLANKS, 2), new ItemStack(Items.BREAD, 4)));

		useOnFloor(helper, player);

		helper.assertBlockPresent(Blocks.OAK_PLANKS, ABOVE_FLOOR);
		ItemStack bundle = player.getMainHandItem();
		helper.assertTrue(bundle.is(EchoItems.ECHO_BUNDLE), "the bundle left the hand: " + bundle);
		helper.assertValueEqual(count(bundle, Items.OAK_PLANKS), 1, "planks left in the bundle");
		helper.assertValueEqual(count(bundle, Items.STONE), 3, "stone left in the bundle");
		helper.assertValueEqual(count(bundle, Items.BREAD), 4, "bread left in the bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlockThatLeavesAContainerBehindIsNeverPlacedFromTheBundle(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		// A powder snow bucket places a block and hands back its bucket, which a bundle cannot.
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT,
				new ItemStack(Items.STONE, 3), new ItemStack(Items.POWDER_SNOW_BUCKET)));

		useOnFloor(helper, player);

		helper.assertBlockPresent(Blocks.STONE, ABOVE_FLOOR);
		helper.assertValueEqual(count(player.getMainHandItem(), Items.POWDER_SNOW_BUCKET), 1, "buckets left in the bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCreativePlayerPlacesFromTheBundleWithoutEmptyingIt(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setGameMode(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 3)));

		useOnFloor(helper, player);

		helper.assertBlockPresent(Blocks.STONE, ABOVE_FLOOR);
		helper.assertValueEqual(count(player.getMainHandItem(), Items.STONE), 3, "stone left in the bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void nothingIsPlacedFromTheBundleWithTheConfigSwitchOff(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.STONE, 3)));

		withConfig(EchoConfig.DEFAULTS.withBundlePlace(false), () -> useOnFloor(helper, player));

		helper.assertBlockNotPresent(Blocks.STONE, ABOVE_FLOOR);
		helper.succeed();
	}

	// --- the bundle's own screen -----------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void sneakUsingTheBundleOpensItsScreenWhoseButtonsSetItsSettings(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT, new ItemStack(Items.IRON_ORE, 2)));
		player.setShiftKeyDown(true);

		player.gameMode.useItem(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);

		helper.assertTrue(player.containerMenu instanceof EchoBundleMenu, "the bundle screen did not open: " + player.containerMenu);
		helper.assertValueEqual(count(player.getMainHandItem(), Items.IRON_ORE), 2, "sneak-use dropped the contents");
		EchoBundleMenu menu = (EchoBundleMenu) player.containerMenu;
		helper.assertTrue(menu.clickMenuButton(player, EchoBundleMenu.VACUUM_ON_BUTTON), "vacuum button handled");
		helper.assertTrue(menu.clickMenuButton(player, EchoBundleMenu.assignButton(Categories.ORES)), "Category button handled");

		EchoBundleSettings settings = EchoBundleItem.settingsOf(player.getMainHandItem());
		helper.assertValueEqual(settings, new EchoBundleSettings(Optional.of(Categories.ORES), true), "the bundle's settings");
		helper.assertValueEqual(menu.category(), Optional.of(Categories.ORES), "the menu's Category");
		helper.assertTrue(menu.vacuums(), "the menu shows vacuum on");

		menu.clickMenuButton(player, EchoBundleMenu.VACUUM_OFF_BUTTON);
		menu.clickMenuButton(player, EchoBundleMenu.CLEAR_CATEGORY_BUTTON);
		helper.assertValueEqual(EchoBundleItem.settingsOf(player.getMainHandItem()), EchoBundleSettings.DEFAULT, "the settings once cleared");
		helper.assertFalse(player.getMainHandItem().has(EchoComponents.ECHO_BUNDLE_SETTINGS), "cleared settings were left on the item");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBundleScreenClosesOnceTheBundleLeavesItsSlot(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bundle(EchoBundleSettings.DEFAULT));
		player.setShiftKeyDown(true);
		player.gameMode.useItem(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
		EchoBundleMenu menu = (EchoBundleMenu) player.containerMenu;

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));

		helper.assertFalse(menu.stillValid(player), "the screen outlived its bundle");
		helper.succeed();
	}

	// --- helpers ---------------------------------------------------------------------------

	private static ServerPlayer player(GameTestHelper helper) {
		helper.setBlock(FLOOR, Blocks.STONE);
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.moveTo(helper.absoluteVec(ABOVE_FLOOR.getCenter()).add(1, 0, 0));
		player.getInventory().selected = 0;
		return player;
	}

	/** An item entity at the player's feet, touched by them as a tick would. */
	private static ItemEntity drop(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		ItemEntity entity = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), stack);
		helper.getLevel().addFreshEntity(entity);
		entity.playerTouch(player);
		return entity;
	}

	/** Uses the main hand on the top of the floor block, down the same path as a player's click. */
	private static void useOnFloor(GameTestHelper helper, ServerPlayer player) {
		BlockHitResult hit = new BlockHitResult(helper.absoluteVec(Vec3.atCenterOf(FLOOR).add(0, 0.5, 0)), Direction.UP,
				helper.absolutePos(FLOOR), false);
		player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static void withConfig(EchoConfig config, Runnable action) {
		EchoConfig before = EchoConfig.get();
		EchoConfig.set(config);
		try {
			action.run();
		} finally {
			EchoConfig.set(before);
		}
	}

	private static EchoBundleSettings vacuuming(Optional<Category> category) {
		return new EchoBundleSettings(category, true);
	}

	private static ItemStack bundle(EchoBundleSettings settings, ItemStack... contents) {
		ItemStack bundle = bundleOf(contents);
		bundle.set(EchoComponents.ECHO_BUNDLE_SETTINGS, settings);
		return bundle;
	}

	private static int count(ItemStack bundle, Item item) {
		int held = 0;
		for (ItemStack inside : bundle.getOrDefault(EchoComponents.ECHO_BUNDLE_CONTENTS, EchoBundleContents.EMPTY).items()) {
			if (inside.is(item)) {
				held += inside.getCount();
			}
		}
		return held;
	}

	/** Whether {@code item} sits in a slot of its own, outside any bundle. */
	private static boolean inventoryHoldsLoose(ServerPlayer player, Item item) {
		return player.getInventory().countItem(item) > 0;
	}
}
