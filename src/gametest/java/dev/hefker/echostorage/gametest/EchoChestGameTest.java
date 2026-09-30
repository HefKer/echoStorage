package dev.hefker.echostorage.gametest;

import static dev.hefker.echostorage.gametest.EchoChestTests.CHEST;
import static dev.hefker.echostorage.gametest.EchoChestTests.NEIGHBOUR;
import static dev.hefker.echostorage.gametest.EchoChestTests.breakChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.chestAt;
import static dev.hefker.echostorage.gametest.EchoChestTests.droppedChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.holding;
import static dev.hefker.echostorage.gametest.EchoChestTests.menu;
import static dev.hefker.echostorage.gametest.EchoChestTests.openedBy;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeFromItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoChestName;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.gametest.EchoChestTests.ChestKind;
import dev.hefker.echostorage.gametest.EchoChestTests.EveryChestKind;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.network.EchoChestRenames;
import dev.hefker.echostorage.network.RenameEchoChestPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Echo Chest's break/drop path and identity lifecycle, in a real world. These are where
 * storage mods lose items, so they are tested against vanilla's own loot and placement code
 * rather than against a stand-in. Most run once for each kind of chest, since a Deep Echo Chest
 * is an Echo Chest in every respect but its size.
 */
public class EchoChestGameTest implements FabricGameTest {
	@GameTestGenerator
	public Collection<TestFunction> everyChestKind() {
		return EchoChestTests.forEveryKind(EchoChestGameTest.class);
	}

	// --- break and drop -------------------------------------------------------------------

	@EveryChestKind(placedOnly = true)
	public void breakingSpillsEveryItemAndDropsTheChest(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		chest.setItem(kind.slots - 1, new ItemStack(Items.DIAMOND_SWORD));

		breakChest(helper, CHEST);

		helper.assertItemEntityCountIs(Items.COBBLESTONE, CHEST, 2, 64);
		helper.assertItemEntityCountIs(Items.DIAMOND_SWORD, CHEST, 2, 1);
		helper.assertItemEntityCountIs(kind.item, CHEST, 2, 1);
		helper.succeed();
	}

	@EveryChestKind(placedOnly = true)
	public void aBlankChestDropsAsAPlainItemThatStacks(GameTestHelper helper, ChestKind kind) {
		placeChest(helper, CHEST, kind);

		breakChest(helper, CHEST);

		ItemStack dropped = droppedChest(helper, kind);
		helper.assertTrue(ItemStack.isSameItemSameComponents(dropped, new ItemStack(kind.item)),
				"a blank chest should drop an item that stacks with a freshly crafted one, got " + dropped);
		helper.succeed();
	}

	@EveryChestKind
	public void aNamedChestDropsCarryingItsNameAndDoesNotStackWithABlankOne(GameTestHelper helper, ChestKind kind) {
		placeChest(helper, CHEST, kind).rename("Ores");

		breakChest(helper, CHEST);

		ItemStack dropped = droppedChest(helper, kind);
		helper.assertValueEqual(dropped.get(DataComponents.CUSTOM_NAME), Component.literal("Ores"), "dropped name");
		helper.assertFalse(ItemStack.isSameItemSameComponents(dropped, new ItemStack(kind.item)),
				"a named chest must not stack with a blank one");
		helper.succeed();
	}

	@EveryChestKind
	public void placingANamedChestRestoresItsName(GameTestHelper helper, ChestKind kind) {
		ItemStack named = new ItemStack(kind.item);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Ores"));

		placeFromItem(helper, named, CHEST);

		helper.assertValueEqual(chestAt(helper, CHEST).name(), "Ores", "name after placing");
		helper.succeed();
	}

	// --- identity -------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyChestHasItsOwnId(GameTestHelper helper) {
		UUID first = placeChest(helper, CHEST).id();
		UUID second = placeChest(helper, NEIGHBOUR).id();

		helper.assertFalse(first.equals(second), "two chests share an id");
		helper.succeed();
	}

	@EveryChestKind
	public void breakingAndReplacingAChestGivesItANewId(GameTestHelper helper, ChestKind kind) {
		ItemStack named = new ItemStack(kind.item);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Ores"));
		placeFromItem(helper, named, CHEST);
		UUID before = chestAt(helper, CHEST).id();

		breakChest(helper, CHEST);
		placeFromItem(helper, droppedChest(helper, kind), CHEST);

		helper.assertValueEqual(chestAt(helper, CHEST).name(), "Ores", "the name travelling with the item");
		helper.assertFalse(before.equals(chestAt(helper, CHEST).id()), "the id must not survive a break");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestPlacedFromCopiedBlockDataStillGetsItsOwnId(GameTestHelper helper) {
		EchoChestBlockEntity original = placeChest(helper, CHEST);
		// Creative pick-block with ctrl copies the block entity's data onto the item.
		ItemStack copy = new ItemStack(EchoItems.ECHO_CHEST);
		original.saveToItem(copy, helper.getLevel().registryAccess());

		placeFromItem(helper, copy, NEIGHBOUR);

		helper.assertFalse(original.id().equals(chestAt(helper, NEIGHBOUR).id()), "copied data duplicated an id");
		helper.succeed();
	}

	@EveryChestKind
	public void theIdAndNameSurviveASaveAndLoad(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		chest.rename("Ores");
		chest.setItem(kind.slots - 1, new ItemStack(Items.DIAMOND, 3));

		CompoundTag saved = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
		EchoChestBlockEntity loaded = (EchoChestBlockEntity) BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), saved,
				helper.getLevel().registryAccess());

		helper.assertValueEqual(loaded.id(), chest.id(), "id after load");
		helper.assertValueEqual(loaded.name(), "Ores", "name after load");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.DIAMOND, 3), loaded.getItem(kind.slots - 1)), "contents after load");
		helper.succeed();
	}

	// --- size and no pairing -------------------------------------------------------------

	@EveryChestKind
	public void theMenuListsEveryChestSlotAndThenThePlayersInventory(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		ServerPlayer player = openedBy(helper, chest);

		helper.assertValueEqual(chest.getContainerSize(), kind.slots, "chest slots");
		List<Slot> slots = menu(player).slots;
		helper.assertValueEqual(slots.size(), kind.slots + Inventory.INVENTORY_SIZE, "menu slots");
		for (int i = 0; i < slots.size(); i++) {
			boolean chestSlot = i < kind.slots;
			helper.assertTrue(slots.get(i).container == (chestSlot ? chest : player.getInventory()),
					"menu slot " + i + " should be the " + (chestSlot ? "chest's" : "player's"));
		}
		helper.succeed();
	}

	/** Against the other kind as well as its own: no two Echo Chests of any kind pair. */
	@EveryChestKind
	public void adjacentChestsStaySeparateChestsOfTheirOwnSize(GameTestHelper helper, ChestKind kind) {
		placeFromItem(helper, new ItemStack(kind.item), CHEST);
		// Sneak-placing against a chest's side is how vanilla asks for a double chest.
		Map<BlockPos, ChestKind> placed = new LinkedHashMap<>(Map.of(CHEST, kind));
		Iterator<Direction> sides = Direction.Plane.HORIZONTAL.iterator();
		for (ChestKind neighbour : ChestKind.values()) {
			Direction side = sides.next();
			Player sneaking = holding(helper, new ItemStack(neighbour.item));
			sneaking.setShiftKeyDown(true);
			helper.placeAt(sneaking, sneaking.getMainHandItem(), CHEST, side);
			placed.put(CHEST.relative(side), neighbour);
		}

		placed.forEach((pos, placedKind) -> {
			helper.assertBlockPresent(placedKind.block, pos);
			helper.assertFalse(helper.getBlockState(pos).hasProperty(BlockStateProperties.CHEST_TYPE),
					"an Echo Chest has no double-chest half to be");
			helper.assertValueEqual(chestAt(helper, pos).getContainerSize(), placedKind.slots, "slots at " + pos);
		});
		helper.succeed();
	}

	// --- crafting a Deep Echo Chest -------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void upgradingAnEchoChestKeepsItsNameCategoryAndStrictness(GameTestHelper helper) {
		ItemStack chest = new ItemStack(EchoItems.ECHO_CHEST);
		chest.set(DataComponents.CUSTOM_NAME, Component.literal("Ores"));
		EchoChestAssignment assignment = new EchoChestAssignment(Optional.of(Categories.ORES), true);
		chest.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, assignment);

		ItemStack deep = craft(helper, column(chest));

		helper.assertTrue(deep.is(EchoItems.DEEP_ECHO_CHEST), "crafted " + deep);
		helper.assertValueEqual(deep.get(DataComponents.CUSTOM_NAME), Component.literal("Ores"), "name");
		helper.assertValueEqual(deep.get(EchoComponents.ECHO_CHEST_ASSIGNMENT), assignment, "assignment");
		placeFromItem(helper, deep, CHEST);
		EchoChestBlockEntity placed = chestAt(helper, CHEST);
		helper.assertValueEqual(placed.name(), "Ores", "placed name");
		helper.assertValueEqual(placed.category(), Optional.of(Categories.ORES), "placed Category");
		helper.assertTrue(placed.isStrict(), "the placed chest should still be strict");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void upgradingABlankEchoChestGivesABlankDeepOneThatStacks(GameTestHelper helper) {
		ItemStack deep = craft(helper, column(new ItemStack(EchoItems.ECHO_CHEST)));

		helper.assertTrue(ItemStack.isSameItemSameComponents(deep, new ItemStack(EchoItems.DEEP_ECHO_CHEST)),
				"a blank chest should upgrade to a plain Deep Echo Chest, got " + deep);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestInAPlusOfFourShardsMakesABlankDeepEchoChest(GameTestHelper helper) {
		ItemStack deep = craft(helper, plus(new ItemStack(Items.CHEST)));

		helper.assertTrue(ItemStack.isSameItemSameComponents(deep, new ItemStack(EchoItems.DEEP_ECHO_CHEST)),
				"a chest should make a plain Deep Echo Chest, got " + deep);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theChestRecipeNeedsTheCornersEmpty(GameTestHelper helper) {
		List<ItemStack> grid = plus(new ItemStack(Items.CHEST));
		grid.set(0, new ItemStack(Items.ECHO_SHARD));

		helper.assertTrue(recipeFor(helper, grid).isEmpty(), "a shard in a corner still crafted");
		helper.succeed();
	}

	/** {@code grid} crafted as a crafting table would, failing the test if nothing matches. */
	private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
		RecipeHolder<CraftingRecipe> recipe = recipeFor(helper, grid)
				.orElseThrow(() -> new GameTestAssertException("no recipe for " + grid));
		return recipe.value().assemble(CraftingInput.of(3, 3, grid), helper.getLevel().registryAccess());
	}

	/** An echo shard above and below {@code centre}: the upgrade from an Echo Chest. */
	private static List<ItemStack> column(ItemStack centre) {
		return shardsAround(centre, 1, 7);
	}

	/** An echo shard on each side of {@code centre}: the recipe from a plain chest. */
	private static List<ItemStack> plus(ItemStack centre) {
		return shardsAround(centre, 1, 3, 5, 7);
	}

	private static List<ItemStack> shardsAround(ItemStack centre, int... shardSlots) {
		List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
		for (int slot : shardSlots) {
			grid.set(slot, new ItemStack(Items.ECHO_SHARD));
		}
		grid.set(4, centre);
		return grid;
	}

	private static Optional<RecipeHolder<CraftingRecipe>> recipeFor(GameTestHelper helper, List<ItemStack> grid) {
		return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), helper.getLevel());
	}

	// --- rename ---------------------------------------------------------------------------

	@EveryChestKind
	public void renamingFromTheOpenScreenNamesTheChest(GameTestHelper helper, ChestKind kind) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST, kind);
		ServerPlayer player = openedBy(helper, chest);

		EchoChestRenames.onRename(player, new RenameEchoChestPayload(player.containerMenu.containerId, "  Ores "));

		helper.assertValueEqual(chest.name(), "Ores", "name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestNamedLongerThanTheAnvilAllowsStillOpens(GameTestHelper helper) {
		// Commands and other mods can name an item past the anvil's limit.
		ItemStack named = new ItemStack(EchoItems.ECHO_CHEST);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("x".repeat(EchoChestName.MAX_LENGTH + 10)));
		placeFromItem(helper, named, CHEST);

		openedBy(helper, chestAt(helper, CHEST));

		helper.assertValueEqual(chestAt(helper, CHEST).name(), "x".repeat(EchoChestName.MAX_LENGTH), "name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void renamingToBlankClearsTheName(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.rename("Ores");
		ServerPlayer player = openedBy(helper, chest);

		EchoChestRenames.onRename(player, new RenameEchoChestPayload(player.containerMenu.containerId, " "));

		helper.assertValueEqual(chest.name(), "", "name");
		helper.assertTrue(chest.getCustomName() == null, "a cleared chest has no custom name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRenameForAScreenThePlayerNoLongerHasOpenIsIgnored(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		ServerPlayer player = openedBy(helper, chest);
		int stale = player.containerMenu.containerId;
		player.closeContainer();

		EchoChestRenames.onRename(player, new RenameEchoChestPayload(stale, "Ores"));

		helper.assertValueEqual(chest.name(), "", "name");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRenameFromTooFarAwayIsIgnored(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		ServerPlayer player = openedBy(helper, chest);
		player.teleportTo(player.getX() + 100, player.getY(), player.getZ());

		EchoChestRenames.onRename(player, new RenameEchoChestPayload(player.containerMenu.containerId, "Ores"));

		helper.assertValueEqual(chest.name(), "", "name");
		helper.succeed();
	}
}
