package dev.hefker.echostorage.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Link resolution over a world that is only connectors, Echo Chests and loaded chunks. */
class LinksTest {
	private static final BlockPos INTERFACE = BlockPos.ZERO;

	private final FakeWorld world = new FakeWorld();

	@Test
	void aChestBesideASculkPathFromTheInterfaceIsLinked() {
		world.connectors(INTERFACE.east(), INTERFACE.east(2), INTERFACE.east(3));
		UUID chest = world.chest(INTERFACE.east(3).north());

		assertEquals(List.of(new LinkedChest(chest, INTERFACE.east(3).north())), resolve().chests());
	}

	@Test
	void aChestTouchingTheInterfaceIsLinkedWithNoSculkAtAll() {
		UUID chest = world.chest(INTERFACE.above());

		assertEquals(List.of(new LinkedChest(chest, INTERFACE.above())), resolve().chests());
	}

	@Test
	void aBlockThatIsNotAConnectorBreaksTheLink() {
		world.connectors(INTERFACE.east());
		// east(2) is plain air.
		world.connectors(INTERFACE.east(3));
		world.chest(INTERFACE.east(4));

		assertEquals(List.of(), resolve().chests());
	}

	@Test
	void aChestDoesNotCarryALinkOnToTheChestBesideIt() {
		UUID linked = world.chest(INTERFACE.east());
		world.chest(INTERFACE.east(2));

		assertEquals(List.of(new LinkedChest(linked, INTERFACE.east())), resolve().chests());
	}

	@Test
	void theLinkReachesAChestAtTheEndOf128Connectors() {
		BlockPos end = line(Links.MAX_CONNECTORS);
		UUID chest = world.chest(end.east());

		assertEquals(List.of(new LinkedChest(chest, end.east())), resolve().chests());
	}

	@Test
	void theLinkStopsAfter128Connectors() {
		BlockPos end = line(Links.MAX_CONNECTORS + 1);
		world.chest(end.east());

		assertEquals(List.of(), resolve().chests());
	}

	@Test
	void theLinkNeverLooksIntoAnUnloadedChunkAndSaysItStoppedShort() {
		world.connectors(INTERFACE.east(), INTERFACE.east(2), INTERFACE.east(3));
		world.unload(INTERFACE.east(2));
		world.chest(INTERFACE.east(3).north());

		Resolution resolution = resolve();

		assertEquals(List.of(), resolution.chests());
		assertFalse(resolution.complete(), "an unloaded chunk cut the traversal short");
	}

	@Test
	void aTraversalThatMetOnlyLoadedChunksIsComplete() {
		world.connectors(INTERFACE.east());

		assertTrue(resolve().complete());
	}

	@Test
	void twoChestsSharingAnIdAreBothListedAndOneIsGivenANewId() {
		UUID shared = UUID.randomUUID();
		world.chest(INTERFACE.east(), shared);
		world.chest(INTERFACE.west(), shared);

		List<LinkedChest> chests = resolve().chests();

		assertEquals(2, chests.size());
		assertEquals(1, chests.stream().filter(chest -> chest.id().equals(shared)).count(), "one keeps the id");
		for (LinkedChest chest : chests) {
			assertEquals(world.chestAt(chest.pos()), chest.id(), "the listed id is the chest's own");
		}
	}

	@Test
	void ofTwoChestsSharingAnIdTheOneTheInterfaceAlreadyListedKeepsIt() {
		UUID shared = UUID.randomUUID();
		world.chest(INTERFACE.east(), shared);
		world.chest(INTERFACE.west(), shared);

		for (BlockPos listed : List.of(INTERFACE.east(), INTERFACE.west())) {
			List<LinkedChest> chests = Links.resolve(INTERFACE, world, Map.of(shared, listed)).chests();

			assertTrue(chests.contains(new LinkedChest(shared, listed)), "the listed chest kept its id, " + chests);
			world.chest(INTERFACE.east(), shared);
			world.chest(INTERFACE.west(), shared);
		}
	}

	@Test
	void thePathGroupsTheConnectorsReachedByHowManyStepsOutTheyAre() {
		world.connectors(INTERFACE.east(), INTERFACE.west(), INTERFACE.east(2), INTERFACE.east(3).above());

		assertEquals(List.of(Set.of(INTERFACE.east(), INTERFACE.west()), Set.of(INTERFACE.east(2))), resolve().path()
				.stream().map(Set::copyOf).toList(), "east(3).above() touches east(2) only at an edge");
	}

	@Test
	void aChestARelayHeardWithinEightBlocksIsLinked() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(8);
		UUID chest = world.chest(far);
		world.relay(relay, far);

		assertEquals(List.of(new LinkedChest(chest, far)), resolve().chests());
	}

	@Test
	void aChestARelayHeardIsNotLinkedOnceItIsMoreThanEightBlocksAway() {
		BlockPos relay = INTERFACE.east();
		BlockPos diagonal = relay.north(6).above(6);
		world.chest(diagonal);
		world.relay(relay, diagonal);

		assertEquals(List.of(), resolve().chests(), "6 up and 6 across is 8.5 blocks away");
	}

	@Test
	void aChestARelayHeardIsNotLinkedWithWoolBetweenThem() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(4);
		world.chest(far);
		world.relay(relay, far);
		world.wool(relay.north(2));

		assertEquals(List.of(), resolve().chests());
	}

	@Test
	void aChestARelayHeardThatWasSinceReplacedIsNotLinked() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(4);
		world.chest(far);
		world.relay(relay, far);
		world.chest(far);

		assertEquals(List.of(), resolve().chests(), "the chest standing there now was never heard");
	}

	@Test
	void aChestARelayHeardInAnUnloadedChunkIsNotLookedAtAndTheResolutionSaysItStoppedShort() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(4);
		world.chest(far);
		world.relay(relay, far);
		world.unload(far);

		Resolution resolution = resolve();

		assertEquals(List.of(), resolution.chests());
		assertFalse(resolution.complete(), "an unloaded chunk hid the chest");
	}

	@Test
	void aRelaysHopAcrossAnUnloadedChunkIsNotCheckedAndTheResolutionSaysItStoppedShort() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(4).east(4);
		world.chest(far);
		world.relay(relay, far);
		world.unload(relay.east(4));

		Resolution resolution = resolve();

		assertEquals(List.of(), resolution.chests());
		assertFalse(resolution.complete(), "the chunk the hop crosses is unloaded");
	}

	@Test
	void aChestNearARelayThatNeverHeardItIsNotLinked() {
		world.relay(INTERFACE.east());
		world.chest(INTERFACE.east().north(3));

		assertEquals(List.of(), resolve().chests());
	}

	@Test
	void aRelayDoesNotCarryTheLinkOnThroughARelayNearIt() {
		BlockPos reached = INTERFACE.east();
		BlockPos other = reached.north(8);
		BlockPos far = other.north(4);
		world.chest(far);
		world.relay(other, far);
		world.relay(reached);

		assertEquals(List.of(), resolve().chests(), "only the relay the sculk reaches hops, and only to a chest it heard");
	}

	@Test
	void aRelayAtTheEndOf128ConnectorsLinksWhatItHeard() {
		BlockPos relay = line(Links.MAX_CONNECTORS);
		BlockPos far = relay.north(4);
		UUID chest = world.chest(far);
		world.relay(relay, far);

		assertEquals(List.of(new LinkedChest(chest, far)), resolve().chests());
	}

	@Test
	void aRelayCountsAsOneOfThe128Connectors() {
		BlockPos relay = line(Links.MAX_CONNECTORS + 1);
		BlockPos far = relay.north(4);
		world.chest(far);
		world.relay(relay, far);

		assertEquals(List.of(), resolve().chests());
	}

	@Test
	void aChestARelayHeardIsListedByHowFarTheLinkRunsToReachIt() {
		world.connectors(INTERFACE.east(2), INTERFACE.east(3));
		UUID nearBySculk = world.chest(INTERFACE.east(3).north());
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.south(4).above(4);
		UUID farByAir = world.chest(far);
		world.relay(relay, far);

		assertEquals(List.of(new LinkedChest(nearBySculk, INTERFACE.east(3).north()), new LinkedChest(farByAir, far)),
				resolve().chests(), "4 steps of sculk is nearer than 1 step and a hop of 8");
	}

	@Test
	void aChestBothTheSculkAndARelayReachIsListedOnce() {
		BlockPos relay = INTERFACE.east();
		BlockPos chestPos = relay.east();
		UUID chest = world.chest(chestPos);
		world.relay(relay, chestPos);

		assertEquals(List.of(new LinkedChest(chest, chestPos)), resolve().chests());
	}

	@Test
	void theHopARelayMakesIsThereForTheLinkVisual() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(5);
		world.chest(far);
		world.relay(relay, far);

		assertEquals(List.of(new Resolution.Hop(relay, far)), resolve().hops());
	}

	@Test
	void aHopThatDoesNotLinkIsNotShown() {
		BlockPos relay = INTERFACE.east();
		BlockPos far = relay.north(5);
		world.chest(far);
		world.relay(relay, far);
		world.wool(relay.north(3));

		assertEquals(List.of(), resolve().hops());
	}

	/** A straight line of {@code length} connectors running east from the interface; returns its end. */
	private BlockPos line(int length) {
		for (int i = 1; i <= length; i++) {
			world.connectors(INTERFACE.east(i));
		}
		return INTERFACE.east(length);
	}

	private Resolution resolve() {
		return Links.resolve(INTERFACE, world, Map.of());
	}

	/** Everything not set is loaded air. Asking about an unloaded block is a test failure. */
	static final class FakeWorld implements LinkWorld {
		private final Set<BlockPos> connectors = new HashSet<>();
		private final Map<BlockPos, UUID> chests = new HashMap<>();
		private final Set<BlockPos> unloaded = new HashSet<>();
		private final Map<BlockPos, List<LinkedChest>> relays = new HashMap<>();
		private final Set<BlockPos> wool = new HashSet<>();

		void connectors(BlockPos... positions) {
			connectors.addAll(List.of(positions));
		}

		UUID chest(BlockPos pos) {
			return chest(pos, UUID.randomUUID());
		}

		UUID chest(BlockPos pos, UUID id) {
			chests.put(pos, id);
			return id;
		}

		/** An Echo Relay at {@code pos} that has heard the chests now standing at {@code heard}. */
		void relay(BlockPos pos, BlockPos... heard) {
			connectors(pos);
			relays.put(pos, List.of(heard).stream().map(chest -> new LinkedChest(chests.get(chest), chest)).toList());
		}

		void wool(BlockPos pos) {
			wool.add(pos);
		}

		void unload(BlockPos pos) {
			unloaded.add(pos);
		}

		@Override
		public boolean isLoaded(BlockPos pos) {
			return !unloaded.contains(pos);
		}

		@Override
		public boolean isConnector(BlockPos pos) {
			requireLoaded(pos);
			return connectors.contains(pos);
		}

		@Nullable
		@Override
		public UUID chestAt(BlockPos pos) {
			requireLoaded(pos);
			return chests.get(pos);
		}

		@Override
		public List<LinkedChest> heardBy(BlockPos pos) {
			requireLoaded(pos);
			return relays.getOrDefault(pos, List.of());
		}

		/** Also fails the test if a corner of the box between the two is unloaded, as a chunk there would be. */
		@Override
		public boolean isOccluded(BlockPos from, BlockPos to) {
			requireLoaded(from);
			requireLoaded(to);
			requireLoaded(new BlockPos(from.getX(), from.getY(), to.getZ()));
			requireLoaded(new BlockPos(to.getX(), from.getY(), from.getZ()));
			return wool.stream().anyMatch(block -> between(from, to, block));
		}

		/** Strictly inside the box {@code from} and {@code to} span, which for a straight line is on it. */
		private static boolean between(BlockPos from, BlockPos to, BlockPos block) {
			return !block.equals(from) && !block.equals(to)
					&& within(from.getX(), to.getX(), block.getX())
					&& within(from.getY(), to.getY(), block.getY())
					&& within(from.getZ(), to.getZ(), block.getZ());
		}

		private static boolean within(int a, int b, int value) {
			return Math.min(a, b) <= value && value <= Math.max(a, b);
		}

		@Override
		public UUID giveNewId(BlockPos pos) {
			return chest(pos, UUID.randomUUID());
		}

		private void requireLoaded(BlockPos pos) {
			if (unloaded.contains(pos)) {
				throw new AssertionError("looked into an unloaded chunk at " + pos);
			}
		}
	}
}
