package dev.hefker.echostorage.gametest;

import java.util.List;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoInterfaceBlockEntity;
import dev.hefker.echostorage.block.EchoRelayBlockEntity;
import dev.hefker.echostorage.link.LinkedChest;
import dev.hefker.echostorage.link.LinkedChests.State;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.menu.EchoInterfaceMenuProvider;
import dev.hefker.echostorage.network.EchoInterfaces;
import dev.hefker.echostorage.network.OpenLinkedChestPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Echo Relay in a real world: it hears a chest being opened, and from then on the sculk that
 * reaches it carries the Link on through the air to that chest, until wool, a broken relay or a
 * replaced chest stops it.
 */
public class EchoRelayGameTest implements FabricGameTest {
	private static final BlockPos INTERFACE = new BlockPos(0, 1, 0);
	/** At the end of two blocks of sculk from the interface. */
	private static final BlockPos RELAY = new BlockPos(3, 1, 0);
	/** Seven blocks from the relay, touching no sculk. */
	private static final BlockPos CHEST = new BlockPos(3, 1, 7);
	/** Halfway between the relay and the chest. */
	private static final BlockPos BETWEEN = new BlockPos(3, 1, 4);

	@GameTest(template = EMPTY_STRUCTURE)
	public void openingAChestNearARelayTheSculkReachesLinksIt(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(CHEST);
		echoInterface.resolve();
		helper.assertTrue(echoInterface.rows().isEmpty(), "the chest was linked before the relay heard it");

		EchoChestTests.openedBy(helper, chest);
		echoInterface.resolve();

		assertRow(helper, echoInterface, chest, State.LINKED);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void woolBetweenTheRelayAndAChestItHeardGreysItsRow(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(CHEST);
		EchoChestTests.openedBy(helper, chest);
		echoInterface.resolve();
		woolWall(helper);

		echoInterface.resolve();

		assertRow(helper, echoInterface, chest, State.LOST);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRelayDoesNotHearAChestOpenedBehindWool(GameTestHelper helper) {
		build(helper);
		woolWall(helper);

		EchoChestTests.openedBy(helper, helper.getBlockEntity(CHEST));

		helper.assertTrue(relay(helper).heard().isEmpty(), "the relay heard through wool: " + relay(helper).heard());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aChestOpenedFromAnInterfaceIsHeardToo(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		// Sculk on to a second chest, so the interface can open it while the relay listens.
		BlockPos other = new BlockPos(5, 1, 3);
		for (int x = 4; x <= other.getX(); x++) {
			helper.setBlock(new BlockPos(x, 1, 0), Blocks.SCULK);
		}
		for (int z = 1; z < other.getZ(); z++) {
			helper.setBlock(new BlockPos(other.getX(), 1, z), Blocks.SCULK);
		}
		EchoChestBlockEntity otherChest = EchoChestTests.placeChest(helper, other);
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.moveTo(helper.absoluteVec(INTERFACE.getCenter()).add(0, 1, 0));
		echoInterface.resolve();
		player.openMenu(new EchoInterfaceMenuProvider(echoInterface));

		EchoInterfaces.onOpen(player, new OpenLinkedChestPayload(player.containerMenu.containerId, otherChest.id()));

		helper.assertTrue(player.containerMenu instanceof EchoChestMenu, "the chest did not open from the interface");
		helper.assertTrue(relay(helper).heard().equals(List.of(new LinkedChest(otherChest.id(), helper.absolutePos(other)))),
				"the relay heard " + relay(helper).heard());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakingTheRelayGreysTheRowAndANewRelayHasToHearTheChestAgain(GameTestHelper helper) {
		EchoInterfaceBlockEntity echoInterface = build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(CHEST);
		EchoChestTests.openedBy(helper, chest);
		echoInterface.resolve();

		helper.destroyBlock(RELAY);
		echoInterface.resolve();
		assertRow(helper, echoInterface, chest, State.LOST);

		helper.setBlock(RELAY, EchoBlocks.ECHO_RELAY);
		echoInterface.resolve();
		assertRow(helper, echoInterface, chest, State.LOST);

		EchoChestTests.openedBy(helper, chest);
		echoInterface.resolve();
		assertRow(helper, echoInterface, chest, State.LINKED);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRelayHearsAChestOnceHoweverOftenItIsOpened(GameTestHelper helper) {
		build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(CHEST);

		EchoChestTests.openedBy(helper, chest).closeContainer();
		EchoChestTests.openedBy(helper, chest);

		helper.assertValueEqual(relay(helper).heard().size(), 1, "chests heard");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatARelayHeardSurvivesASaveAndLoad(GameTestHelper helper) {
		build(helper);
		EchoChestBlockEntity chest = helper.getBlockEntity(CHEST);
		EchoChestTests.openedBy(helper, chest);
		EchoRelayBlockEntity relay = relay(helper);

		CompoundTag saved = relay.saveWithFullMetadata(helper.getLevel().registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(relay.getBlockPos(), relay.getBlockState(), saved, helper.getLevel().registryAccess());

		helper.assertTrue(loaded instanceof EchoRelayBlockEntity reloaded
						&& reloaded.heard().equals(List.of(new LinkedChest(chest.id(), helper.absolutePos(CHEST)))),
				"the relay came back having heard " + (loaded instanceof EchoRelayBlockEntity reloaded ? reloaded.heard() : loaded));
		helper.succeed();
	}

	// --- helpers --------------------------------------------------------------------------

	/** An interface, two blocks of sculk, a relay, and a chest seven blocks from the relay. */
	private static EchoInterfaceBlockEntity build(GameTestHelper helper) {
		helper.setBlock(INTERFACE, EchoBlocks.ECHO_INTERFACE);
		for (int x = 1; x < RELAY.getX(); x++) {
			helper.setBlock(new BlockPos(x, 1, 0), Blocks.SCULK);
		}
		helper.setBlock(RELAY, EchoBlocks.ECHO_RELAY);
		helper.setBlock(CHEST, EchoBlocks.ECHO_CHEST);
		return helper.getBlockEntity(INTERFACE);
	}

	/** A wall of wool across the line between the relay and the chest, wide enough that no side of it sees past. */
	private static void woolWall(GameTestHelper helper) {
		for (int x = BETWEEN.getX() - 1; x <= BETWEEN.getX() + 1; x++) {
			for (int y = BETWEEN.getY(); y <= BETWEEN.getY() + 1; y++) {
				helper.setBlock(new BlockPos(x, y, BETWEEN.getZ()), Blocks.WHITE_WOOL);
			}
		}
	}

	private static EchoRelayBlockEntity relay(GameTestHelper helper) {
		return helper.getBlockEntity(RELAY);
	}

	private static void assertRow(GameTestHelper helper, EchoInterfaceBlockEntity echoInterface, EchoChestBlockEntity chest, State state) {
		helper.assertTrue(echoInterface.rows().size() == 1
						&& echoInterface.rows().getFirst().id().equals(chest.id())
						&& echoInterface.rows().getFirst().state() == state,
				"expected the chest's row to be " + state + ", got " + echoInterface.rows());
	}
}
