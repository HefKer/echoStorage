package dev.hefker.echostorage.link;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Link resolution (ADR-0004): a flood-fill from an Echo Interface through connector blocks. An
 * Echo Chest touching the interface or any connector reached is linked, and so is each chest an
 * Echo Relay reached has heard, while it can still reach it through the air. That hop is the
 * last: a chest carries no Link on, and relays only ever hear chests.
 *
 * <p>The fill never looks into an unloaded chunk. A chest the interface already lists is then
 * greyed ({@link LinkedChests}); one it has never reached simply has no row until the chunk loads,
 * since learning it exists would mean loading the chunk.
 */
public final class Links {
	/** How many connector blocks one interface's traversal may enter. */
	public static final int MAX_CONNECTORS = 128;
	/** How far an Echo Relay hears and reaches: a sculk sensor's range (ADR-0004). */
	public static final int RELAY_RANGE = 8;

	private Links() {
	}

	/**
	 * Resolves the Links from the interface at {@code origin}.
	 *
	 * @param known where each chest the interface already lists was last seen
	 */
	public static Resolution resolve(BlockPos origin, LinkWorld world, Map<UUID, BlockPos> known) {
		Map<BlockPos, Found> found = new LinkedHashMap<>();
		List<List<BlockPos>> path = new ArrayList<>();
		List<Resolution.Hop> hops = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(origin);
		List<BlockPos> wave = List.of(origin);
		int steps = 0;
		int connectors = 0;
		boolean complete = true;
		while (!wave.isEmpty()) {
			steps++;
			List<BlockPos> next = new ArrayList<>();
			for (BlockPos from : wave) {
				for (Direction direction : Direction.values()) {
					BlockPos to = from.relative(direction);
					if (!seen.add(to)) {
						continue;
					}
					// Never force-load a chunk (ADR-0004); a chest behind one is simply not reached now.
					if (!world.isLoaded(to)) {
						complete = false;
						continue;
					}
					UUID chest = world.chestAt(to);
					if (chest != null) {
						reached(found, new LinkedChest(chest, to), steps);
					} else if (connectors < MAX_CONNECTORS && world.isConnector(to)) {
						connectors++;
						next.add(to);
						for (LinkedChest heard : world.heardBy(to)) {
							switch (reach(world, to, heard.pos())) {
								case CLEAR -> {
									// Only the chest heard: one broken and replaced since has a new id.
									if (heard.id().equals(world.chestAt(heard.pos()))) {
										reached(found, heard, steps + Math.sqrt(to.distSqr(heard.pos())));
										hops.add(new Resolution.Hop(to, heard.pos()));
									}
								}
								case UNLOADED -> complete = false;
								case BLOCKED -> {
								}
							}
						}
					}
				}
			}
			if (!next.isEmpty()) {
				path.add(next);
			}
			wave = next;
		}
		List<LinkedChest> chests = found.values().stream()
				.sorted(Comparator.comparingDouble(Found::distance))
				.map(Found::chest)
				.toList();
		return new Resolution(withOwnIds(chests, world, known), path, hops, complete);
	}

	/** Keeps the shorter way to a chest reached both over sculk and through the air, or by two relays. */
	private static void reached(Map<BlockPos, Found> found, LinkedChest chest, double distance) {
		found.merge(chest.pos(), new Found(chest, distance), (kept, other) -> other.distance() < kept.distance() ? other : kept);
	}

	/**
	 * A chest reached, and how far the Link runs from the interface to reach it: a block a step
	 * over connectors, then a relay's hop measured straight through the air.
	 */
	private record Found(LinkedChest chest, double distance) {
	}

	/**
	 * Whether the Echo Relay at {@code relay} can hear, or reach, the block at {@code chest}: within
	 * {@link #RELAY_RANGE}, measured as a sculk sensor measures, with nothing between them that
	 * stops a vibration. Never looks into an unloaded chunk.
	 */
	public static Reach reach(LinkWorld world, BlockPos relay, BlockPos chest) {
		if (relay.distSqr(chest) > RELAY_RANGE * RELAY_RANGE) {
			return Reach.BLOCKED;
		}
		// Within range the box between the two spans at most two chunks each way, so its
		// corners and the chest cover every chunk the occlusion check could look into.
		for (int x : new int[] {relay.getX(), chest.getX()}) {
			for (int z : new int[] {relay.getZ(), chest.getZ()}) {
				if (!world.isLoaded(new BlockPos(x, relay.getY(), z))) {
					return Reach.UNLOADED;
				}
			}
		}
		if (!world.isLoaded(chest)) {
			return Reach.UNLOADED;
		}
		return world.isOccluded(relay, chest) ? Reach.BLOCKED : Reach.CLEAR;
	}

	/** Whether an Echo Relay hears or reaches a block; see {@link #reach}. */
	public enum Reach {
		CLEAR,
		/** Out of range, or a vibration would be stopped on the way. */
		BLOCKED,
		/** A chunk between them is unloaded, so it cannot be told. */
		UNLOADED
	}

	/**
	 * Gives every chest but one a new id wherever two share an id, which {@code /clone} and
	 * structure blocks can cause by copying a chest's saved data. The chest the interface already
	 * lists at that position keeps the id, so its row stays its own; otherwise the nearest does.
	 */
	private static List<LinkedChest> withOwnIds(List<LinkedChest> found, LinkWorld world, Map<UUID, BlockPos> known) {
		Map<UUID, BlockPos> keeper = new HashMap<>();
		for (LinkedChest chest : found) {
			BlockPos listed = known.get(chest.id());
			if (listed != null && listed.equals(chest.pos())) {
				keeper.put(chest.id(), chest.pos());
			}
		}
		for (LinkedChest chest : found) {
			keeper.putIfAbsent(chest.id(), chest.pos());
		}
		List<LinkedChest> chests = new ArrayList<>(found.size());
		for (LinkedChest chest : found) {
			chests.add(keeper.get(chest.id()).equals(chest.pos())
					? chest
					: new LinkedChest(world.giveNewId(chest.pos()), chest.pos()));
		}
		return chests;
	}
}
