package dev.hefker.echostorage.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Link resolution (ADR-0004): a flood-fill from an Echo Interface through connector blocks. An
 * Echo Chest touching the interface or any connector reached is linked.
 *
 * <p>The fill never looks into an unloaded chunk. A chest the interface already lists is then
 * greyed ({@link LinkedChests}); one it has never reached simply has no row until the chunk loads,
 * since learning it exists would mean loading the chunk.
 */
public final class Links {
	/** How many connector blocks one interface's traversal may enter. */
	public static final int MAX_CONNECTORS = 128;

	private Links() {
	}

	/**
	 * Resolves the Links from the interface at {@code origin}.
	 *
	 * @param known where each chest the interface already lists was last seen
	 */
	public static Resolution resolve(BlockPos origin, LinkWorld world, Map<UUID, BlockPos> known) {
		List<LinkedChest> chests = new ArrayList<>();
		List<List<BlockPos>> path = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(origin);
		List<BlockPos> wave = List.of(origin);
		int connectors = 0;
		boolean complete = true;
		while (!wave.isEmpty()) {
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
						chests.add(new LinkedChest(chest, to));
					} else if (connectors < MAX_CONNECTORS && world.isConnector(to)) {
						connectors++;
						next.add(to);
					}
				}
			}
			if (!next.isEmpty()) {
				path.add(next);
			}
			wave = next;
		}
		return new Resolution(withOwnIds(chests, world, known), path, complete);
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
