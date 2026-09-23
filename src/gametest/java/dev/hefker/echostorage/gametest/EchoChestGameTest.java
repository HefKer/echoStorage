package dev.hefker.echostorage.gametest;

import java.util.List;
import java.util.UUID;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoChestName;
import dev.hefker.echostorage.item.EchoItems;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoChestMenuProvider;
import dev.hefker.echostorage.network.EchoChestRenames;
import dev.hefker.echostorage.network.RenameEchoChestPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Echo Chest's break/drop path and identity lifecycle, in a real world. These are where
 * storage mods lose items, so they are tested against vanilla's own loot and placement code
 * rather than against a stand-in.
 */
public class EchoChestGameTest implements FabricGameTest {
	// GameTestHelper.assertValueEqual takes (actual, expected, what).
	private static final BlockPos CHEST = new BlockPos(1, 1, 1);
	private static final BlockPos NEIGHBOUR = new BlockPos(2, 1, 1);

	// --- break and drop -------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingSpillsEveryItemAndDropsTheChest(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		chest.setItem(26, new ItemStack(Items.DIAMOND_SWORD));

		breakChest(helper, CHEST);

		helper.assertItemEntityCountIs(Items.COBBLESTONE, CHEST, 2, 64);
		helper.assertItemEntityCountIs(Items.DIAMOND_SWORD, CHEST, 2, 1);
		helper.assertItemEntityCountIs(EchoItems.ECHO_CHEST, CHEST, 2, 1);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlankChestDropsAsAPlainItemThatStacks(GameTestHelper helper) {
		placeChest(helper, CHEST);

		breakChest(helper, CHEST);

		ItemStack dropped = droppedChest(helper);
		helper.assertTrue(ItemStack.isSameItemSameComponents(dropped, new ItemStack(EchoItems.ECHO_CHEST)),
				"a blank chest should drop an item that stacks with a freshly crafted one, got " + dropped);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNamedChestDropsCarryingItsNameAndDoesNotStackWithABlankOne(GameTestHelper helper) {
		placeChest(helper, CHEST).rename("Ores");

		breakChest(helper, CHEST);

		ItemStack dropped = droppedChest(helper);
		helper.assertValueEqual(dropped.get(DataComponents.CUSTOM_NAME), Component.literal("Ores"), "dropped name");
		helper.assertFalse(ItemStack.isSameItemSameComponents(dropped, new ItemStack(EchoItems.ECHO_CHEST)),
				"a named chest must not stack with a blank one");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void placingANamedChestRestoresItsName(GameTestHelper helper) {
		ItemStack named = new ItemStack(EchoItems.ECHO_CHEST);
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

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingAndReplacingAChestGivesItANewId(GameTestHelper helper) {
		ItemStack named = new ItemStack(EchoItems.ECHO_CHEST);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Ores"));
		placeFromItem(helper, named, CHEST);
		UUID before = chestAt(helper, CHEST).id();

		breakChest(helper, CHEST);
		placeFromItem(helper, droppedChest(helper), CHEST);

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

	@GameTest(template = EMPTY_STRUCTURE)
	public void theIdAndNameSurviveASaveAndLoad(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
		chest.rename("Ores");
		chest.setItem(4, new ItemStack(Items.DIAMOND, 3));

		CompoundTag saved = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
		EchoChestBlockEntity loaded = new EchoChestBlockEntity(chest.getBlockPos(), chest.getBlockState());
		loaded.loadWithComponents(saved, helper.getLevel().registryAccess());

		helper.assertValueEqual(loaded.id(), chest.id(), "id after load");
		helper.assertValueEqual(loaded.name(), "Ores", "name after load");
		helper.assertTrue(ItemStack.matches(new ItemStack(Items.DIAMOND, 3), loaded.getItem(4)), "contents after load");
		helper.succeed();
	}

	// --- no pairing -----------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void adjacentChestsStayTwoChestsOfTwentySevenSlots(GameTestHelper helper) {
		placeFromItem(helper, new ItemStack(EchoItems.ECHO_CHEST), CHEST);
		// Sneak-placing against a chest's side is how vanilla asks for a double chest.
		Player sneaking = holding(helper, new ItemStack(EchoItems.ECHO_CHEST));
		sneaking.setShiftKeyDown(true);
		helper.placeAt(sneaking, sneaking.getMainHandItem(), CHEST, Direction.EAST);

		helper.assertBlockPresent(EchoBlocks.ECHO_CHEST, NEIGHBOUR);
		for (BlockPos pos : List.of(CHEST, NEIGHBOUR)) {
			helper.assertFalse(helper.getBlockState(pos).hasProperty(BlockStateProperties.CHEST_TYPE),
					"an Echo Chest has no double-chest half to be");
			helper.assertValueEqual(chestAt(helper, pos).getContainerSize(), 27, "slots");
		}
		helper.succeed();
	}

	// --- rename ---------------------------------------------------------------------------

	@GameTest(template = EMPTY_STRUCTURE)
	public void renamingFromTheOpenScreenNamesTheChest(GameTestHelper helper) {
		EchoChestBlockEntity chest = placeChest(helper, CHEST);
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

	// --- helpers --------------------------------------------------------------------------

	private static EchoChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, EchoBlocks.ECHO_CHEST);
		return chestAt(helper, pos);
	}

	private static EchoChestBlockEntity chestAt(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos);
	}

	/** Places {@code stack} at {@code pos} the way a player would, through the block item. */
	private static void placeFromItem(GameTestHelper helper, ItemStack stack, BlockPos pos) {
		Player player = holding(helper, stack);
		helper.placeAt(player, player.getMainHandItem(), pos.below(), Direction.UP);
		helper.assertBlockPresent(EchoBlocks.ECHO_CHEST, pos);
	}

	/** Placement reads the stack from the player's hand, not from the stack it is handed. */
	private static Player holding(GameTestHelper helper, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	/** Breaks the block as a player's tool would: loot table drops and all. */
	private static void breakChest(GameTestHelper helper, BlockPos pos) {
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
	}

	private static ItemStack droppedChest(GameTestHelper helper) {
		List<ItemStack> chests = helper.getEntities(EntityType.ITEM).stream()
				.map(ItemEntity::getItem)
				.filter(stack -> stack.is(EchoItems.ECHO_CHEST))
				.toList();
		helper.assertValueEqual(chests.size(), 1, "dropped Echo Chests");
		return chests.getFirst();
	}

	private static ServerPlayer openedBy(GameTestHelper helper, EchoChestBlockEntity chest) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(CHEST.getCenter()).add(0, 1, 0));
		player.openMenu(new EchoChestMenuProvider(chest));
		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "the Echo Chest menu did not open");
		return player;
	}
}
