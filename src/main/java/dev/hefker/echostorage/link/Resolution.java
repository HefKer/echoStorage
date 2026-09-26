package dev.hefker.echostorage.link;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * What one run of link resolution found.
 *
 * @param chests   the linked Echo Chests, nearest first, by how far the Link runs to reach each
 * @param path     the connectors reached, grouped by how many steps out from the interface they
 *                 are, nearest first; what the link visual traces
 * @param hops     each Echo Relay's hop through the air to a chest it linked; the link visual's
 *                 wireless legs
 * @param complete false if an unloaded chunk cut the traversal short, so a chest not found may
 *                 still be linked
 */
public record Resolution(List<LinkedChest> chests, List<List<BlockPos>> path, List<Hop> hops, boolean complete) {
	public Resolution {
		chests = List.copyOf(chests);
		path = path.stream().<List<BlockPos>>map(List::copyOf).toList();
		hops = List.copyOf(hops);
	}

	/** An Echo Relay at {@code relay} carrying a Link through the air to the Echo Chest at {@code chest}. */
	public record Hop(BlockPos relay, BlockPos chest) {
	}
}
