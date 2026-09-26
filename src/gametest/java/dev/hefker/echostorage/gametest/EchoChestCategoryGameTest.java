package dev.hefker.echostorage.gametest;

import static dev.hefker.echostorage.gametest.EchoChestTests.CHEST;
import static dev.hefker.echostorage.gametest.EchoChestTests.FIRST_MAIN_INVENTORY_SLOT;
import static dev.hefker.echostorage.gametest.EchoChestTests.FIRST_PLAYER_MENU_SLOT;
import static dev.hefker.echostorage.gametest.EchoChestTests.NEIGHBOUR;
import static dev.hefker.echostorage.gametest.EchoChestTests.breakChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.bundleOf;
import static dev.hefker.echostorage.gametest.EchoChestTests.chestAt;
import static dev.hefker.echostorage.gametest.EchoChestTests.droppedChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.menu;
import static dev.hefker.echostorage.gametest.EchoChestTests.openedBy;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeFromItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoChestMenuData;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * An Echo Chest's Category and strictness: how they are saved, assigned, enforced and shown.
 * Assignment is unrestricted and never moves anything; strictness only refuses what is being
 * put in by shift-click or hopper.
 */
public class EchoChestCategoryGameTest implements FabricGameTest {
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

	@GameTest(template = EMPTY_STRUCTURE)
	public void aScreenOpenedBeforeItsFirstSyncShowsNoCategory(GameTestHelper helper) {
		// The client's menu, as it stands between the open packet and the first data sync.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EchoChestMenu unsynced = new EchoChestMenu(1, player.getInventory(), new EchoChestMenuData(""));

		helper.assertValueEqual(unsynced.category(), Optional.empty(), "category before sync");
		helper.assertFalse(unsynced.isStray(new ItemStack(Items.BREAD)), "an unsynced screen marks strays");
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

	// --- through bundles (ADR-0007, read-only) --------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBundleIsJudgedByWhatItHolds(GameTestHelper helper) {
		Optional<Category> ores = Optional.of(Categories.ORES);

		helper.assertFalse(EchoChestBlockEntity.isStray(ores, bundleOf(new ItemStack(Items.IRON_ORE, 10))), "a bundle of ore is a stray");
		helper.assertTrue(EchoChestBlockEntity.isStray(ores, bundleOf(new ItemStack(Items.IRON_ORE, 10), new ItemStack(Items.BREAD, 1))),
				"a bundle with bread in it is not a stray");
		helper.assertFalse(EchoChestBlockEntity.isStray(ores, bundleOf()), "an empty bundle is a stray");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestTakesAShiftClickedBundleOfItsCategory(GameTestHelper helper) {
		EchoChestBlockEntity chest = strictChestOf(helper, Categories.ORES);
		ServerPlayer player = openedBy(helper, chest);
		ItemStack bundle = bundleOf(new ItemStack(Items.IRON_ORE, 10));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, bundle.copy());

		menu(player).quickMoveStack(player, FIRST_PLAYER_MENU_SLOT);

		helper.assertTrue(ItemStack.matches(bundle, chest.getItem(0)), "the bundle went in, got " + chest.getItem(0));
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

	// --- on the item (ADR-0008) -----------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUnnamedChestWithACategoryDropsCarryingItButNoName(GameTestHelper helper) {
		placeChest(helper, CHEST).assign(Categories.ORES);

		breakChest(helper, CHEST);

		ItemStack dropped = droppedChest(helper);
		helper.assertTrue(dropped.get(DataComponents.CUSTOM_NAME) == null,
				"the Category name must not be written onto the item as its name, got " + dropped);
		helper.assertValueEqual(dropped.get(EchoComponents.ECHO_CHEST_ASSIGNMENT),
				new EchoChestAssignment(Optional.of(Categories.ORES), false), "assignment on the item");
		helper.assertFalse(ItemStack.isSameItemSameComponents(dropped, new ItemStack(EchoItems.ECHO_CHEST)),
				"an assigned chest must not stack with a blank one");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anAssignedStrictChestKeepsBothWhenBrokenAndReplaced(GameTestHelper helper) {
		strictChestOf(helper, Categories.ORES);

		breakChest(helper, CHEST);
		placeFromItem(helper, droppedChest(helper), CHEST);

		EchoChestBlockEntity replaced = chestAt(helper, CHEST);
		helper.assertValueEqual(replaced.category(), Optional.of(Categories.ORES), "category after replacing");
		helper.assertTrue(replaced.isStrict(), "strictness after replacing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anAssignedPermissiveChestStaysPermissiveWhenBrokenAndReplaced(GameTestHelper helper) {
		placeChest(helper, CHEST).assign(Categories.FOOD);

		breakChest(helper, CHEST);
		placeFromItem(helper, droppedChest(helper), CHEST);

		EchoChestBlockEntity replaced = chestAt(helper, CHEST);
		helper.assertValueEqual(replaced.category(), Optional.of(Categories.FOOD), "category after replacing");
		helper.assertFalse(replaced.isStrict(), "a permissive chest comes back permissive");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aStrictChestWithNoCategoryKeepsItsStrictness(GameTestHelper helper) {
		placeChest(helper, CHEST).setStrict(true);

		breakChest(helper, CHEST);
		placeFromItem(helper, droppedChest(helper), CHEST);

		EchoChestBlockEntity replaced = chestAt(helper, CHEST);
		helper.assertValueEqual(replaced.category(), Optional.empty(), "category after replacing");
		helper.assertTrue(replaced.isStrict(), "strictness after replacing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anItemNamingACategoryThatNoLongerShipsPlacesUnassigned(GameTestHelper helper) throws CommandSyntaxException {
		// As if carried over from a version that shipped a "wood" preset this one has since dropped.
		CompoundTag saved = TagParser.parseTag("""
				{id: "echostorage:echo_chest", count: 1, components: {
					"echostorage:echo_chest_assignment": {category: "wood", strict: 1b},
					"minecraft:custom_name": '"Logs"'}}""");
		ItemStack stale = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), saved)
				.getOrThrow();

		placeFromItem(helper, stale, CHEST);

		EchoChestBlockEntity placed = chestAt(helper, CHEST);
		helper.assertValueEqual(placed.category(), Optional.empty(), "category after placing");
		helper.assertTrue(placed.isStrict(), "strictness after placing");
		helper.assertValueEqual(placed.name(), "Logs", "name after placing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pickBlockWithDataCarriesTheAssignmentAsAComponentOnly(GameTestHelper helper) {
		EchoChestBlockEntity original = strictChestOf(helper, Categories.ORES);
		original.rename("Mine haul");
		// Creative pick-block with ctrl copies the block entity's data onto the item.
		ItemStack copy = new ItemStack(EchoItems.ECHO_CHEST);
		original.saveToItem(copy, helper.getLevel().registryAccess());

		CompoundTag data = copy.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		helper.assertFalse(data.contains("Category") || data.contains("Strict") || data.contains("CustomName"),
				"implicit components must be stripped from the copied block data, got " + data);
		helper.assertValueEqual(copy.get(EchoComponents.ECHO_CHEST_ASSIGNMENT),
				new EchoChestAssignment(Optional.of(Categories.ORES), true), "assignment on the copy");

		placeFromItem(helper, copy, NEIGHBOUR);

		EchoChestBlockEntity placed = chestAt(helper, NEIGHBOUR);
		helper.assertValueEqual(placed.category(), Optional.of(Categories.ORES), "category after placing");
		helper.assertTrue(placed.isStrict(), "strictness after placing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theItemTooltipShowsTheCategoryAndStrictness(GameTestHelper helper) {
		ItemStack assigned = new ItemStack(EchoItems.ECHO_CHEST);
		assigned.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, new EchoChestAssignment(Optional.of(Categories.ORES), true));

		helper.assertValueEqual(tooltipOf(assigned), List.of(
				Component.translatable("item.echostorage.echo_chest.category", Categories.ORES.displayName()).withStyle(ChatFormatting.GRAY),
				Component.translatable("item.echostorage.echo_chest.strict").withStyle(ChatFormatting.GRAY)), "tooltip");
		helper.assertValueEqual(tooltipOf(new ItemStack(EchoItems.ECHO_CHEST)), List.of(), "a blank chest's tooltip");
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	/** The lines the Echo Chest adds under the item's name. */
	private static List<Component> tooltipOf(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
		return lines;
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

	private static CompoundTag save(GameTestHelper helper, EchoChestBlockEntity chest) {
		return chest.saveWithFullMetadata(helper.getLevel().registryAccess());
	}

	private static EchoChestBlockEntity load(GameTestHelper helper, EchoChestBlockEntity chest, CompoundTag saved) {
		EchoChestBlockEntity loaded = new EchoChestBlockEntity(chest.getBlockPos(), chest.getBlockState());
		loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
		return loaded;
	}
}
