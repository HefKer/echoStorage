package dev.hefker.echostorage.link;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * What link resolution asks of the world. {@link Links} never asks about a block in an unloaded
 * chunk, so an implementation never has to load one to answer.
 */
public interface LinkWorld {
	/** Whether the chunk holding {@code pos} is loaded. */
	boolean isLoaded(BlockPos pos);

	/** Whether the block at {@code pos} can carry a Link. */
	boolean isConnector(BlockPos pos);

	/** The id of the Echo Chest at {@code pos}, or null if there is none. */
	@Nullable
	UUID chestAt(BlockPos pos);

	/**
	 * The Echo Chests the Echo Relay at {@code pos} has heard, each where it stood when heard;
	 * empty if there is no relay there.
	 */
	List<LinkedChest> heardBy(BlockPos pos);

	/**
	 * Whether a block that stops vibrations, such as wool, stands between the Echo Relay at
	 * {@code relay} and the block at {@code chest}. Only asked once every chunk between them is
	 * known to be loaded.
	 */
	boolean isOccluded(BlockPos relay, BlockPos chest);

	/** Gives the Echo Chest at {@code pos} a fresh id, because another chest already has its id. */
	UUID giveNewId(BlockPos pos);
}
