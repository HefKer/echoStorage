package dev.hefker.echostorage.gametest;

import java.util.List;
import java.util.UUID;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoInterfaceBlockEntity;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.link.LinkedChests.State;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoInterfaceMenu;
import dev.hefker.echostorage.menu.EchoInterfaceMenuProvider;
import dev.hefker.echostorage.network.DismissLinkedChestPayload;
import dev.hefker.echostorage.network.EchoInterfaces;
import dev.hefker.echostorage.network.OpenLinkedChestPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The Echo Interface in a real world: sculk links it to a chest out of arm's reach, a row opens
 * that chest from where the player stands, and global quick-stack reaches linked chests only,
 * filling those that already hold an item before those whose Category merely matches it.
 */
public class EchoInterfaceGameTest implements FabricGameTest {
	private static final BlockPos INTERFACE = new BlockPos(0, 1, 0);
	/** The far corner of the template, well beyond arm's reach of a player standing on the interface. */
	private static final BlockPos FAR_CHEST = new BlockPos(7, 1, 7);
	/** A chest no sculk touches. */
	private static final BlockPos LONE_CHEST = new BlockPos(3, 1, 5);
	/** One block of the path, for cutting it. */
	private static final BlockPos PATH_BLOCK = new BlockPos(4, 1, 0);
	/** A second linked chest, beside the path and so nearer the interface than the far one. */
	private static final BlockPos NEAR_CHEST = new BlockPos(2, 1, 1);
	private static final int FIRST_MAIN_INVENTORY_SLOT = 9;

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSculkPathLinksAChestAndTheInterfaceListsIt(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(FAR_CHEST);
		chest.rename("Ores");

		echoInterface.resolve();

		assertRows(helper, echoInterface, List.of(new Row(chest.id(), helper.absolutePos(FAR_CHEST), "Ores",
				chest.category(), State.LINKED)));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestCutOffKeepsAGreyedRowUntilItIsDismissed(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		UUID chest = helper.<EchoChestBlockEntity>getBlockEntity(FAR_CHEST).id();
		ServerPlayer player = openedBy(helper, echoInterface);
		helper.setBlock(PATH_BLOCK, Blocks.STONE);

		echoInterface.resolve();
		helper.assertValueEqual(echoInterface.rows().getFirst().state(), State.LOST, "the cut-off chest's row");

		EchoInterfaces.onDismiss(player, new DismissLinkedChestPayload(player.containerMenu.containerId, chest));
		helper.assertTrue(echoInterface.rows().isEmpty(), "the dismissed row stayed: " + echoInterface.rows());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pickingARowOpensThatChestFromBeyondArmsReach(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(FAR_CHEST);
		ServerPlayer player = openedBy(helper, echoInterface);
		helper.assertFalse(chest.stillValid(player), "the chest should be out of reach for this test to mean anything");

		EchoInterfaces.onOpen(player, new OpenLinkedChestPayload(player.containerMenu.containerId, chest.id()));

		helper.assertTrue(player.containerMenu instanceof EchoChestMenu menu && menu.container() == chest,
				"the linked chest did not open, got " + player.containerMenu);
		helper.assertTrue(player.containerMenu.stillValid(player), "the chest closed as soon as it opened");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestCutOffSinceTheListWasSentDoesNotOpen(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(FAR_CHEST);
		ServerPlayer player = openedBy(helper, echoInterface);
		helper.setBlock(PATH_BLOCK, Blocks.STONE);

		EchoInterfaces.onOpen(player, new OpenLinkedChestPayload(player.containerMenu.containerId, chest.id()));

		helper.assertTrue(player.containerMenu instanceof EchoInterfaceMenu, "a cut-off chest opened");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestOpenedFromTheInterfaceClosesWhenThePlayerWalksAwayFromTheInterface(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(FAR_CHEST);
		ServerPlayer player = openedBy(helper, echoInterface);
		EchoInterfaces.onOpen(player, new OpenLinkedChestPayload(player.containerMenu.containerId, chest.id()));

		player.moveTo(helper.absoluteVec(INTERFACE.getCenter()).add(0, 20, 0));

		helper.assertFalse(player.containerMenu.stillValid(player), "the chest stayed usable from far away");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void globalQuickStackPutsAwayIntoLinkedChestsOnly(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity linked = helper.getBlockEntity(FAR_CHEST);
		linked.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
		helper.setBlock(LONE_CHEST, EchoBlocks.ECHO_CHEST);
		EchoChestBlockEntity lone = helper.getBlockEntity(LONE_CHEST);
		lone.setItem(0, new ItemStack(Items.BREAD, 1));
		ServerPlayer player = openedBy(helper, echoInterface);
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.COBBLESTONE, 20));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.BREAD, 5));

		helper.assertTrue(player.containerMenu.clickMenuButton(player, EchoInterfaceMenu.QUICK_STACK_BUTTON), "button handled");

		helper.assertTrue(ItemStack.matches(linked.getItem(0), new ItemStack(Items.COBBLESTONE, 21)),
				"the linked chest has " + linked.getItem(0));
		helper.assertTrue(ItemStack.matches(lone.getItem(0), new ItemStack(Items.BREAD, 1)),
				"the unlinked chest has " + lone.getItem(0));
		helper.assertTrue(ItemStack.matches(player.getInventory().getItem(FIRST_MAIN_INVENTORY_SLOT + 1), new ItemStack(Items.BREAD, 5)),
				"the bread should have stayed with the player");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void globalQuickStackPrefersTheFarChestHoldingAnItemOverANearChestOfItsCategory(GameTestHelper helper) {
		assertHolderFilledBeforeCategory(helper, FAR_CHEST, NEAR_CHEST);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void globalQuickStackPrefersTheNearChestHoldingAnItemOverAFarChestOfItsCategory(GameTestHelper helper) {
		assertHolderFilledBeforeCategory(helper, NEAR_CHEST, FAR_CHEST);
	}

	// --- helpers --------------------------------------------------------------------------

	/**
	 * Both orders are tested because the list's order is the interface's, not the test's: in one
	 * of them the Category chest is listed first, which is the case the first pass exists for.
	 */
	private static void assertHolderFilledBeforeCategory(GameTestHelper helper, BlockPos holding, BlockPos ofCategory) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		helper.setBlock(NEAR_CHEST, EchoBlocks.ECHO_CHEST);
		EchoChestBlockEntity holder = helper.getBlockEntity(holding);
		holder.setItem(0, new ItemStack(Items.IRON_ORE, 1));
		EchoChestBlockEntity categoryChest = helper.getBlockEntity(ofCategory);
		categoryChest.assign(Categories.ORES);
		ServerPlayer player = openedBy(helper, echoInterface);
		helper.assertValueEqual(echoInterface.rows().size(), 2, "linked chests");
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT, new ItemStack(Items.IRON_ORE, 20));
		player.getInventory().setItem(FIRST_MAIN_INVENTORY_SLOT + 1, new ItemStack(Items.COAL_ORE, 5));

		player.containerMenu.clickMenuButton(player, EchoInterfaceMenu.QUICK_STACK_BUTTON);

		helper.assertTrue(ItemStack.matches(holder.getItem(0), new ItemStack(Items.IRON_ORE, 21)),
				"the chest already holding iron ore has " + holder.getItem(0));
		helper.assertTrue(ItemStack.matches(categoryChest.getItem(0), new ItemStack(Items.COAL_ORE, 5)),
				"the ores chest should have taken only the coal ore, has " + categoryChest.getItem(0));
		helper.assertTrue(categoryChest.getItem(1).isEmpty(), "the ores chest also has " + categoryChest.getItem(1));
		helper.succeed();
	}

	/** An interface in one corner, a chest in the far one, and sculk running along two edges between them. */
	private static EchoInterfaceBlockEntity build(GameTestHelper helper) {
		helper.setBlock(INTERFACE, EchoBlocks.ECHO_INTERFACE);
		for (int x = 1; x <= FAR_CHEST.getX(); x++) {
			helper.setBlock(new BlockPos(x, 1, 0), Blocks.SCULK);
		}
		for (int z = 1; z < FAR_CHEST.getZ(); z++) {
			helper.setBlock(new BlockPos(FAR_CHEST.getX(), 1, z), Blocks.SCULK);
		}
		helper.setBlock(FAR_CHEST, EchoBlocks.ECHO_CHEST);
		return helper.getBlockEntity(INTERFACE);
	}

	private static ServerPlayer openedBy(GameTestHelper helper, EchoInterfaceBlockEntity echoInterface) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(INTERFACE.getCenter()).add(0, 1, 0));
		echoInterface.resolve();
		player.openMenu(new EchoInterfaceMenuProvider(echoInterface));
		helper.assertTrue(player.containerMenu instanceof EchoInterfaceMenu, "the Echo Interface menu did not open");
		return player;
	}

	private static void assertRows(GameTestHelper helper, EchoInterfaceBlockEntity echoInterface, List<Row> expected) {
		helper.assertTrue(echoInterface.rows().equals(expected), "expected rows " + expected + ", got " + echoInterface.rows());
	}
}
