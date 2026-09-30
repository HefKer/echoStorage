package dev.hefker.echostorage.gametest;

import static dev.hefker.echostorage.gametest.EchoChestTests.CHEST;
import static dev.hefker.echostorage.gametest.EchoChestTests.assertStack;
import static dev.hefker.echostorage.gametest.EchoChestTests.breakChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.bundleOf;
import static dev.hefker.echostorage.gametest.EchoChestTests.chestAt;
import static dev.hefker.echostorage.gametest.EchoChestTests.holding;
import static dev.hefker.echostorage.gametest.EchoChestTests.menu;
import static dev.hefker.echostorage.gametest.EchoChestTests.openedBy;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeChest;
import static dev.hefker.echostorage.gametest.EchoChestTests.placeFromItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * The Echo Shulker Box's own behaviour: what it keeps when broken, how it is crafted and dyed, and
 * the vanilla shulker box rules it follows. What it does as an Echo Chest is tested with the
 * other kinds, through {@link EchoChestTests.ChestKind#SHULKER}.
 */
public class EchoShulkerBoxGameTest implements FabricGameTest {
	private static final EchoChestAssignment ORES_STRICT = new EchoChestAssignment(Optional.of(Categories.ORES), true);

	// --- break and place ------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void placingABoxRestoresEverythingItCarriesAndBreakingItDropsOneItemCarryingItAll(GameTestHelper helper) {
		ItemStack carried = boxHolding(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIAMOND_SWORD));
		carried.set(DataComponents.CUSTOM_NAME, Component.literal("Ores"));
		carried.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, ORES_STRICT);
		carried.set(DataComponents.BASE_COLOR, DyeColor.LIME);
		ItemStack expected = carried.copy();

		placeFromItem(helper, carried, CHEST);
		EchoChestBlockEntity box = chestAt(helper, CHEST);
		helper.assertValueEqual(box.name(), "Ores", "placed name");
		helper.assertValueEqual(box.category(), Optional.of(Categories.ORES), "placed Category");
		helper.assertTrue(box.isStrict(), "the placed box should be strict");
		assertStack(helper, new ItemStack(Items.COBBLESTONE, 64), box.getItem(0), "slot 0");
		assertStack(helper, new ItemStack(Items.DIAMOND_SWORD), box.getItem(1), "slot 1");

		breakChest(helper, CHEST);

		List<ItemEntity> dropped = helper.getEntities(EntityType.ITEM);
		helper.assertValueEqual(dropped.size(), 1, "item entities dropped");
		assertStack(helper, expected, dropped.getFirst().getItem(), "the dropped box");
		helper.succeed();
	}

	/** Powered from above: power near the template's corner would re-trigger its structure block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPistonDestroysABoxAndItDropsWithItsContents(GameTestHelper helper) {
		BlockPos box = new BlockPos(4, 1, 4);
		BlockPos piston = box.west();
		helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
		placeChest(helper, box, EchoChestTests.ChestKind.SHULKER).setItem(3, new ItemStack(Items.DIAMOND, 5));

		helper.setBlock(piston.above(), Blocks.REDSTONE_BLOCK);

		helper.succeedWhen(() -> {
			List<ItemEntity> dropped = helper.getEntities(EntityType.ITEM);
			helper.assertValueEqual(dropped.size(), 1, "item entities dropped");
			ItemStack stack = dropped.getFirst().getItem();
			helper.assertTrue(stack.is(EchoItems.ECHO_SHULKER_BOX), "dropped " + stack);
			assertStack(helper, new ItemStack(Items.DIAMOND, 5), contents(stack).get(3), "the dropped box's slot 3");
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingABoxWithSomethingInItInCreativeDropsIt(GameTestHelper helper) {
		placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER).setItem(0, new ItemStack(Items.DIAMOND));
		breakInCreative(helper);

		ItemStack dropped = EchoChestTests.droppedChest(helper, EchoChestTests.ChestKind.SHULKER);
		assertStack(helper, new ItemStack(Items.DIAMOND), contents(dropped).getFirst(), "the dropped box's slot 0");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingAnEmptyBoxInCreativeDropsNothing(GameTestHelper helper) {
		placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER);
		breakInCreative(helper);

		helper.assertValueEqual(helper.getEntities(EntityType.ITEM).size(), 0, "item entities dropped");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDestroyedBoxItemSpillsItsContents(GameTestHelper helper) {
		ItemEntity box = new ItemEntity(helper.getLevel(), helper.absoluteVec(CHEST.getCenter()).x, helper.absoluteVec(CHEST.getCenter()).y,
				helper.absoluteVec(CHEST.getCenter()).z, boxHolding(new ItemStack(Items.DIAMOND, 7)));
		helper.getLevel().addFreshEntity(box);

		box.hurt(helper.getLevel().damageSources().lava(), 100);

		helper.assertItemEntityCountIs(Items.DIAMOND, CHEST, 2, 7);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBoxBreaksAsFastAsAVanillaOne(GameTestHelper helper) {
		helper.setBlock(CHEST, EchoBlocks.ECHO_SHULKER_BOX);
		helper.setBlock(CHEST.east(), Blocks.SHULKER_BOX);
		ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);

		helper.assertValueEqual(helper.getBlockState(CHEST).getDestroySpeed(helper.getLevel(), helper.absolutePos(CHEST)),
				helper.getBlockState(CHEST.east()).getDestroySpeed(helper.getLevel(), helper.absolutePos(CHEST.east())), "hardness");
		helper.assertValueEqual(pickaxe.getDestroySpeed(helper.getBlockState(CHEST)), pickaxe.getDestroySpeed(helper.getBlockState(CHEST.east())),
				"a pickaxe's speed on it");
		helper.succeed();
	}

	// --- opening --------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlockOnTheLidKeepsTheBoxShutToAPlayersHand(GameTestHelper helper) {
		placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER);
		helper.setBlock(CHEST.above(), Blocks.STONE);
		ServerPlayer player = playerBeside(helper);

		helper.useBlock(CHEST, player);
		helper.assertFalse(player.containerMenu instanceof EchoChestMenu, "a box with its lid blocked opened");

		helper.setBlock(CHEST.above(), Blocks.AIR);
		helper.useBlock(CHEST, player);
		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "a box with room above did not open");
		helper.succeed();
	}

	// --- what goes inside -----------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEchoBundleRefusesAnEchoShulkerBox(GameTestHelper helper) {
		EchoBundleContents.Mutable bundle = new EchoBundleContents.Mutable(EchoBundleContents.EMPTY);

		helper.assertValueEqual(bundle.tryInsert(new ItemStack(EchoItems.ECHO_SHULKER_BOX)), 0, "boxes the bundle took");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aHopperPutsNoShulkerBoxIntoABoxButDoesPutAnEchoBundle(GameTestHelper helper) {
		EchoChestBlockEntity box = placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER);

		for (ItemStack shulkerBox : shulkerBoxes()) {
			ItemStack left = HopperBlockEntity.addItem(null, box, shulkerBox.copy(), Direction.DOWN);
			assertStack(helper, shulkerBox, left, "what the hopper could not put in");
		}
		ItemStack bundle = bundleOf(new ItemStack(Items.DIAMOND));
		helper.assertTrue(HopperBlockEntity.addItem(null, box, bundle.copy(), Direction.DOWN).isEmpty(), "the hopper kept the Echo Bundle");
		assertStack(helper, bundle, box.getItem(0), "slot 0");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBoxsSlotsTakeNoShulkerBoxButDoTakeAnEchoBundle(GameTestHelper helper) {
		ServerPlayer player = openedBy(helper, placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER));
		EchoChestMenu menu = menu(player);

		for (ItemStack shulkerBox : shulkerBoxes()) {
			helper.assertFalse(menu.getSlot(0).mayPlace(shulkerBox), "the box's slot took " + shulkerBox);
		}
		helper.assertTrue(menu.getSlot(0).mayPlace(bundleOf()), "the box's slot refused an Echo Bundle");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shiftClickingAShulkerBoxIntoABoxLeavesItWithThePlayer(GameTestHelper helper) {
		ServerPlayer player = openedBy(helper, placeChest(helper, CHEST, EchoChestTests.ChestKind.SHULKER));
		EchoChestMenu menu = menu(player);
		player.getInventory().setItem(EchoChestTests.FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.SHULKER_BOX));

		menu.quickMoveStack(player, menu.chestSlots());

		helper.assertTrue(menu.container().isEmpty(), "the box took a shulker box from a shift-click");
		helper.succeed();
	}

	// --- crafting -------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void shardsAroundAVanillaBoxMakeAnEchoShulkerBoxKeepingItsContentsColourAndName(GameTestHelper helper) {
		ItemStack vanilla = new ItemStack(Items.LIME_SHULKER_BOX);
		vanilla.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3))));
		vanilla.set(DataComponents.CUSTOM_NAME, Component.literal("Gems"));

		ItemStack made = craft(helper, column(vanilla));

		helper.assertTrue(made.is(EchoItems.ECHO_SHULKER_BOX), "crafted " + made);
		helper.assertValueEqual(made.get(DataComponents.CONTAINER), vanilla.get(DataComponents.CONTAINER), "contents");
		helper.assertValueEqual(made.get(DataComponents.BASE_COLOR), DyeColor.LIME, "colour");
		helper.assertValueEqual(made.get(DataComponents.CUSTOM_NAME), Component.literal("Gems"), "name");
		helper.assertFalse(made.has(EchoComponents.ECHO_CHEST_ASSIGNMENT), "a new box should be unassigned and permissive");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUndyedVanillaBoxMakesAnUndyedEchoShulkerBox(GameTestHelper helper) {
		ItemStack made = craft(helper, column(new ItemStack(Items.SHULKER_BOX)));

		helper.assertTrue(made.is(EchoItems.ECHO_SHULKER_BOX), "crafted " + made);
		helper.assertFalse(made.has(DataComponents.BASE_COLOR), "an undyed box came out " + made.get(DataComponents.BASE_COLOR));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aVanillaBoxWhoseLootHasNotRolledIsNoInputForTheShardRecipe(GameTestHelper helper) {
		ItemStack unrolled = new ItemStack(Items.PURPLE_SHULKER_BOX);
		unrolled.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(BuiltInLootTables.END_CITY_TREASURE, 7L));

		helper.assertTrue(recipeFor(helper, column(unrolled)).isEmpty(), "a box with a loot table still crafted");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEchoShulkerBoxIsNoInputForTheShardRecipe(GameTestHelper helper) {
		helper.assertTrue(recipeFor(helper, column(new ItemStack(EchoItems.ECHO_SHULKER_BOX))).isEmpty(),
				"an Echo Shulker Box between shards still crafted");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void dyeingABoxRecoloursItAndKeepsTheRest(GameTestHelper helper) {
		ItemStack box = settledBox();
		box.set(DataComponents.BASE_COLOR, DyeColor.LIME);

		ItemStack dyed = craft(helper, grid(box, new ItemStack(Items.RED_DYE)));

		ItemStack expected = box.copy();
		expected.set(DataComponents.BASE_COLOR, DyeColor.RED);
		assertStack(helper, expected, dyed, "the dyed box");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void washingABoxInAWaterCauldronUndyesItAndKeepsTheRest(GameTestHelper helper) {
		ItemStack box = settledBox();
		box.set(DataComponents.BASE_COLOR, DyeColor.LIME);
		helper.setBlock(CHEST, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		Player player = holding(helper, box.copy());

		helper.useBlock(CHEST, player);

		ItemStack expected = box.copy();
		expected.remove(DataComponents.BASE_COLOR);
		assertStack(helper, expected, player.getItemInHand(InteractionHand.MAIN_HAND), "the washed box");
		helper.assertBlockProperty(CHEST, LayeredCauldronBlock.LEVEL, 2);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUndyedBoxIsNotWashed(GameTestHelper helper) {
		helper.setBlock(CHEST, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
		Player player = holding(helper, new ItemStack(EchoItems.ECHO_SHULKER_BOX));

		helper.useBlock(CHEST, player);

		helper.assertBlockProperty(CHEST, LayeredCauldronBlock.LEVEL, 3);
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	/** A box with something in it, a name, a Category and Strict: what dyeing and washing must keep. */
	private static ItemStack settledBox() {
		ItemStack box = boxHolding(new ItemStack(Items.DIAMOND, 3));
		box.set(DataComponents.CUSTOM_NAME, Component.literal("Gems"));
		box.set(EchoComponents.ECHO_CHEST_ASSIGNMENT, ORES_STRICT);
		return box;
	}

	private static ItemStack boxHolding(ItemStack... contents) {
		ItemStack box = new ItemStack(EchoItems.ECHO_SHULKER_BOX);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
		return box;
	}

	private static List<ItemStack> contents(ItemStack box) {
		NonNullList<ItemStack> slots = NonNullList.withSize(EchoChestTests.ChestKind.SHULKER.slots, ItemStack.EMPTY);
		box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);
		return slots;
	}

	/** Every kind of shulker box: vanilla's, dyed and undyed, and an Echo one. */
	private static List<ItemStack> shulkerBoxes() {
		return List.of(new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.RED_SHULKER_BOX), new ItemStack(EchoItems.ECHO_SHULKER_BOX));
	}

	private static void breakInCreative(GameTestHelper helper) {
		ServerPlayer player = playerBeside(helper);
		player.setGameMode(GameType.CREATIVE);
		player.gameMode.destroyBlock(helper.absolutePos(CHEST));
		helper.assertBlockNotPresent(EchoBlocks.ECHO_SHULKER_BOX, CHEST);
	}

	private static ServerPlayer playerBeside(GameTestHelper helper) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(CHEST.getCenter()).add(1, 0, 0));
		return player;
	}

	private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
		RecipeHolder<CraftingRecipe> recipe = recipeFor(helper, grid)
				.orElseThrow(() -> new GameTestAssertException("no recipe for " + grid));
		return recipe.value().assemble(CraftingInput.of(3, 3, grid), helper.getLevel().registryAccess());
	}

	private static Optional<RecipeHolder<CraftingRecipe>> recipeFor(GameTestHelper helper, List<ItemStack> grid) {
		return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), helper.getLevel());
	}

	/** An echo shard above and below {@code centre}. */
	private static List<ItemStack> column(ItemStack centre) {
		List<ItemStack> grid = grid(ItemStack.EMPTY);
		grid.set(1, new ItemStack(Items.ECHO_SHARD));
		grid.set(4, centre);
		grid.set(7, new ItemStack(Items.ECHO_SHARD));
		return grid;
	}

	/** A 3x3 grid starting with {@code first}, the rest empty. */
	private static List<ItemStack> grid(ItemStack... first) {
		List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
		for (int i = 0; i < first.length; i++) {
			grid.set(i, first[i]);
		}
		return grid;
	}
}
