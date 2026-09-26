package dev.hefker.echostorage.block;

import java.util.List;
import java.util.UUID;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.link.Connectors;
import dev.hefker.echostorage.link.LinkWorld;
import dev.hefker.echostorage.link.LinkedChest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The world as link resolution and an Echo Relay's hearing see it. Never loads a chunk: every
 * look is behind {@link #isLoaded}, which {@link dev.hefker.echostorage.link.Links} asks first.
 */
record LevelLinks(ServerLevel level) implements LinkWorld {
	@Override
	public boolean isLoaded(BlockPos pos) {
		return level.isLoaded(pos);
	}

	@Override
	public boolean isConnector(BlockPos pos) {
		return level.getBlockState(pos).is(Connectors.TAG);
	}

	@Nullable
	@Override
	public UUID chestAt(BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest ? chest.id() : null;
	}

	@Override
	public List<LinkedChest> heardBy(BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoRelayBlockEntity relay ? relay.heard() : List.of();
	}

	/**
	 * Vanilla's rule for a vibration reaching a sculk sensor: blocked only if a line from every
	 * side of the chest's centre to the relay's centre meets wool or another block tagged
	 * {@code occludes_vibration_signals}. The chest is where the vibration starts, as an opening
	 * chest is for a sculk sensor.
	 */
	@Override
	public boolean isOccluded(BlockPos relay, BlockPos chest) {
		Vec3 chestCentre = Vec3.atCenterOf(chest);
		Vec3 relayCentre = Vec3.atCenterOf(relay);
		for (Direction side : Direction.values()) {
			ClipBlockStateContext line = new ClipBlockStateContext(chestCentre.relative(side, 1.0E-5F), relayCentre,
					state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS));
			if (level.isBlockInLine(line).getType() != HitResult.Type.BLOCK) {
				return false;
			}
		}
		return true;
	}

	@Override
	public UUID giveNewId(BlockPos pos) {
		EchoChestBlockEntity chest = (EchoChestBlockEntity) level.getBlockEntity(pos);
		chest.assignNewId();
		EchoStorage.LOGGER.info("Echo Chest at {} shared its id with another chest and was given a new one", pos);
		return chest.id();
	}
}
