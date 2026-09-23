package dev.hefker.echostorage.gametest;

import java.util.Optional;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * An Echo Chest's Category and strictness: how they are saved, assigned, enforced and shown.
 * Assignment is unrestricted and never moves anything; strictness only refuses what is being
 * put in by shift-click or hopper.
 */
public class EchoChestCategoryGameTest implements FabricGameTest {
	// GameTestHelper.assertValueEqual takes (actual, expected, what).
	private static final BlockPos CHEST = new BlockPos(1, 1, 1);
	/** The first slot of the player's main inventory, above the hotbar. */
	private static final int FIRST_MAIN_INVENTORY_SLOT = 9;
	/** The same slot in the Echo Chest menu, which lists the chest's 27 first. */
	private static final int FIRST_PLAYER_MENU_SLOT = EchoChestBlockEntity.SLOTS;

	// --- persistence ----------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCategoryAndStrictnessSurviveASaveAndLoad(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.setStrict(true);

		EchoChestBlockEntity loaded = load(helper, chest, save(helper, chest));

		helper.assertValueEqual(loaded.category(), Optional.of(Categories.ORES), "category after load");
		helper.assertTrue(loaded.isStrict(), "strictness after load");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestNeverAssignedLoadsUnassignedAndPermissive(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);

		EchoChestBlockEntity loaded = load(helper, chest, save(helper, chest));

		helper.assertValueEqual(loaded.category(), Optional.empty(), "category after load");
		helper.assertFalse(loaded.isStrict(), "a chest is permissive by default");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCategoryThatNoLongerShipsLoadsUnassignedAndKeepsTheRest(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.rename("Logs");
		chest.assign(Categories.ORES);
		chest.setStrict(true);
		chest.setItem(4, new ItemStack(Items.OAK_LOG, 32));
		CompoundTag saved = save(helper, chest);
		// As if saved by a version that shipped a "wood" preset this one has since dropped.
		saved.putString("Category", "wood");

		EchoChestBlockEntity loaded = load(helper, chest, saved);

		helper.assertValueEqual(loaded.category(), Optional.empty(), "category after load");
		helper.assertValueEqual(loaded.name(), "Logs", "name after load");
		helper.assertTrue(loaded.isStrict(), "strictness after load");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.OAK_LOG, 32), loaded.getItem(4)), "contents after load");
		helper.succeed();
	}

	// --- assignment -----------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void assigningFromTheOpenScreenSetsTheCategory(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		ServerPlayer player = openedBy(helper, chest);

		helper.assertTrue(menu(player).clickMenuButton(player, EchoChestMenu.assignButton(Categories.ORES)), "button handled");

		helper.assertValueEqual(chest.category(), Optional.of(Categories.ORES), "category");
		helper.assertValueEqual(menu(player).category(), Optional.of(Categories.ORES), "category the screen sees");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void changingTheCategoryOfAFullChestMovesNothing(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.setItem(0, new ItemStack(Items.IRON_ORE, 16));
		chest.setItem(1, new ItemStack(Items.BREAD, 5));
		ServerPlayer player = openedBy(helper, chest);

		menu(player).clickMenuButton(player, EchoChestMenu.assignButton(Categories.FOOD));

		helper.assertValueEqual(chest.category(), Optional.of(Categories.FOOD), "category");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.IRON_ORE, 16), chest.getItem(0)), "the stray stays put");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.BREAD, 5), chest.getItem(1)), "the match stays put");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void clearingTheCategoryLeavesTheChestUnassigned(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);

		menu(player).clickMenuButton(player, EchoChestMenu.CLEAR_CATEGORY_BUTTON);

		helper.assertValueEqual(chest.category(), Optional.empty(), "category");
		helper.assertValueEqual(menu(player).category(), Optional.empty(), "category the screen sees");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theStrictAndPermissiveButtonsSetStrictness(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		ServerPlayer player = openedBy(helper, chest);

		menu(player).clickMenuButton(player, EchoChestMenu.STRICT_BUTTON);
		menu(player).clickMenuButton(player, EchoChestMenu.STRICT_BUTTON);
		helper.assertTrue(chest.isStrict(), "strict after asking twice");
		helper.assertTrue(menu(player).isStrict(), "strictness the screen sees");

		menu(player).clickMenuButton(player, EchoChestMenu.PERMISSIVE_BUTTON);
		helper.assertFalse(chest.isStrict(), "strict after asking for permissive");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aButtonTheScreenDoesNotHaveChangesNothing(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);

		for (int button : new int[] {-1, EchoChestMenu.assignButton(Categories.ALL.getLast()) + 1, 999}) {
			helper.assertFalse(menu(player).clickMenuButton(player, button), "button " + button + " handled");
		}

		helper.assertValueEqual(chest.category(), Optional.of(Categories.ORES), "category");
		helper.assertFalse(chest.isStrict(), "strictness");
		helper.succeed();
	}

	// --- strictness -----------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestRefusesAStrayFromAHopper(GameTestHelper helper) {
		EchoChestBlockEntity chest = strictChestOf(helper, Categories.ORES);

		ItemStack left = hopperInto(chest, new ItemStack(Items.BREAD, 5));

		helper.assertTrue(ItemStack.matches(new ItemStack(Items.BREAD, 5), left), "the hopper keeps the stray, got " + left);
		helper.assertTrue(chest.isEmpty(), "the chest took a stray");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestTakesItsCategoryFromAHopper(GameTestHelper helper) {
		EchoChestBlockEntity chest = strictChestOf(helper, Categories.ORES);

		ItemStack left = hopperInto(chest, new ItemStack(Items.IRON_ORE, 5));

		helper.assertTrue(left.isEmpty(), "the hopper kept a match, " + left);
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.IRON_ORE, 5), chest.getItem(0)), "the match went in");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPermissiveChestTakesStraysFromAHopper(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);

		ItemStack left = hopperInto(chest, new ItemStack(Items.BREAD, 5));

		helper.assertTrue(left.isEmpty(), "a permissive chest refused a stray, " + left);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void strictWithNoCategoryRefusesNothing(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.setStrict(true);

		ItemStack left = hopperInto(chest, new ItemStack(Items.BREAD, 5));

		helper.assertTrue(left.isEmpty(), "an unassigned chest has nothing to be strict about, " + left);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestRefusesAShiftClickedStray(GameTestHelper helper) {
		EchoChestBlockEntity chest = strictChestOf(helper, Categories.ORES);
		// A stray already inside must not become a way in for more of it.
		chest.setItem(0, new ItemStack(Items.BREAD, 1));
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.BREAD, 5));

		menu(player).quickMoveStack(player, FIRST_PLAYER_MENU_SLOT);

		helper.assertTrue(ItemStack.matches(new ItemStack(Items.BREAD, 5), player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT)),
				"the stray stays with the player");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.BREAD, 1), chest.getItem(0)), "the chest took a stray");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestTakesAShiftClickedMatch(GameTestHelper helper) {
		EchoChestBlockEntity chest = strictChestOf(helper, Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.IRON_ORE, 5));

		menu(player).quickMoveStack(player, FIRST_PLAYER_MENU_SLOT);

		helper.assertTrue(ItemStack.matches(new ItemStack(Items.IRON_ORE, 5), chest.getItem(0)), "the match went in");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPermissiveChestTakesAShiftClickedStray(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.BREAD, 5));

		menu(player).quickMoveStack(player, FIRST_PLAYER_MENU_SLOT);

		helper.assertTrue(ItemStack.matches(new ItemStack(Items.BREAD, 5), chest.getItem(0)), "the stray went in");
		helper.succeed();
	}

	// --- display fallback -----------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUnnamedChestIsShownByItsCategoryInItalics(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);

		helper.assertValueEqual(chest.getName(), Component.translatable("category.echostorage.ores").withStyle(ChatFormatting.ITALIC),
				"shown name");
		helper.assertValueEqual(chest.name(), "", "typed name");
		helper.assertTrue(chest.getCustomName() == null, "the Category name is never stored as the chest's name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTypedNameBeatsTheCategory(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.rename("Mine haul");

		helper.assertValueEqual(chest.getName(), Component.literal("Mine haul"), "shown name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void clearingTheCategoryOfAnUnnamedChestStrandsNoName(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(Categories.ORES);
		chest.assign(null);

		helper.assertValueEqual(chest.getName(), EchoBlocks.ECHO_CHEST.getName(), "shown name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUnnamedChestWithACategoryDropsAsAPlainItem(GameTestHelper helper) {
		placeChest(helper, CHEST).assign(Categories.ORES);

		helper.getLevel().destroyBlock(helper.absolutePos(CHEST), true);

		ItemStack dropped = helper.getEntities(EntityType.ITEM).stream()
				.map(ItemEntity::getItem)
				.filter(stack -> stack.is(EchoItems.ECHO_CHEST))
				.findFirst()
				.orElseThrow();
		helper.assertTrue(ItemStack.isSameItemSameComponents(dropped, new ItemStack(EchoItems.ECHO_CHEST)),
				"the Category name must not be written onto the item, got " + dropped);
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	private static EchoChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, EchoBlocks.ECHO_CHEST);
		return helper.getBlockEntity(pos);
	}

	private static EchoChestBlockEntity strictChestOf(GameTestHelper helper, Category category) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.assign(category);
		chest.setStrict(true);
		return chest;
	}

	/** Vanilla's hopper insert, which hoppers and droppers both use; returns what did not fit. */
	private static ItemStack hopperInto(EchoChestBlockEntity chest, ItemStack stack) {
		return HopperBlockEntity.addItem(null, chest, stack, Direction.DOWN);
	}

	private static ServerPlayer openedBy(GameTestHelper helper, EchoChestBlockEntity chest) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(CHEST.getCenter()).add(0, 1, 0));
		player.openMenu(new EchoChestMenuProvider(chest));
		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "the Echo Chest menu did not open");
		return player;
	}

	private static EchoChestMenu menu(ServerPlayer player) {
		return (EchoChestMenu) player.containerMenu;
	}

	private static CompoundTag save(GameTestHelper helper, EchoChestBlockEntity chest) {
		return chest.saveWithFullMetadata(helper.getLevel().registryAccess());
	}

	private static EchoChestBlockEntity load(GameTestHelper helper, EchoChestBlockEntity chest, CompoundTag saved) {
		EchoChestBlockEntity loaded = new EchoChestBlockEntity(chest.getBlockPos(), chest.getBlockState());
		loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
		return loaded;
	}
}
