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
