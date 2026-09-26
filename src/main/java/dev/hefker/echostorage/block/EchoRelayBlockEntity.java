package dev.hefker.echostorage.block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.link.LinkWorld;
import dev.hefker.echostorage.link.LinkedChest;
import dev.hefker.echostorage.link.Links;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import org.jetbrains.annotations.Nullable;

/**
 * An Echo Relay: the Echo Chests it has heard being opened, which it carries a Link to while they
 * stay in reach (ADR-0004). Whether each is still in reach is decided by {@link Links} on every
 * resolution, never here; this only remembers.
 *
 * <p>What it heard is saved with it and dies with the block, so a relay broken and placed again
 * has to hear each chest again.
 */
public class EchoRelayBlockEntity extends BlockEntity {
	private static final String HEARD_TAG = "Heard";

	private final List<LinkedChest> heard = new ArrayList<>();
	/** Echoes still on their way back from a chest just heard. Not saved: they are only particles. */
	private final List<Echo> echoes = new ArrayList<>();

	public EchoRelayBlockEntity(BlockPos pos, BlockState state) {
		super(EchoBlocks.ECHO_RELAY_ENTITY, pos, state);
	}

	/** The chests this relay has heard, each where it stood when heard. */
	public List<LinkedChest> heard() {
		return List.copyOf(heard);
	}

	/**
	 * A player opened {@code chest}, at it or through an Echo Interface: every relay that can hear
	 * it remembers it. Only relays in loaded chunks are asked, and none is loaded to ask it.
	 */
	public static void hearOpening(ServerLevel level, EchoChestBlockEntity chest) {
		BlockPos at = chest.getBlockPos();
		LinkedChest opened = new LinkedChest(chest.id(), at);
		LinkWorld world = new LevelLinks(level);
		int range = Links.RELAY_RANGE;
		for (int chunkX = SectionPos.blockToSectionCoord(at.getX() - range); chunkX <= SectionPos.blockToSectionCoord(at.getX() + range); chunkX++) {
			for (int chunkZ = SectionPos.blockToSectionCoord(at.getZ() - range); chunkZ <= SectionPos.blockToSectionCoord(at.getZ() + range); chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : List.copyOf(chunk.getBlockEntities().values())) {
					if (entity instanceof EchoRelayBlockEntity relay
							&& Links.reach(world, relay.getBlockPos(), at) == Links.Reach.CLEAR) {
						relay.hear(opened, level);
					}
				}
			}
		}
	}

	/**
	 * Remembers {@code chest}, and the first time shows it: a vibration out to the chest and an
	 * echo back. A chest heard where another used to stand replaces it, since that one is gone.
	 */
	private void hear(LinkedChest chest, ServerLevel level) {
		if (heard.contains(chest)) {
			return;
		}
		heard.removeIf(old -> old.pos().equals(chest.pos()));
		heard.add(chest);
		setChanged();
		int travel = vibrate(level, null, getBlockPos(), chest.pos());
		echoes.add(new Echo(chest.pos(), level.getGameTime() + travel));
		level.scheduleTick(getBlockPos(), getBlockState().getBlock(), travel);
	}

	/** Sends the echoes whose ping has reached its chest by now. */
	void echo(ServerLevel level) {
		for (Iterator<Echo> pending = echoes.iterator(); pending.hasNext(); ) {
			Echo echo = pending.next();
			if (echo.due() <= level.getGameTime()) {
				vibrate(level, null, echo.chest(), getBlockPos());
				pending.remove();
			}
		}
	}

	/**
	 * A vanilla vibration particle from {@code from} to {@code to}, shown to {@code viewer} alone,
	 * or to everyone nearby if null. Returns how many ticks it takes to arrive, as a sculk
	 * sensor's vibration would.
	 */
	static int vibrate(ServerLevel level, @Nullable ServerPlayer viewer, BlockPos from, BlockPos to) {
		int travel = travelTicks(from, to);
		VibrationParticleOption particle = new VibrationParticleOption(new BlockPositionSource(to), travel);
		double x = from.getX() + 0.5;
		double y = from.getY() + 0.5;
		double z = from.getZ() + 0.5;
		if (viewer == null) {
			level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		} else {
			level.sendParticles(viewer, particle, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		return travel;
	}

	/** A vibration's travel time: a tick a block, as vanilla's. */
	static int travelTicks(BlockPos from, BlockPos to) {
		return Math.max(1, Mth.floor(Math.sqrt(from.distSqr(to))));
	}

	// --- persistence ----------------------------------------------------------------------

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		heard.clear();
		heard.addAll(LinkedChest.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get(HEARD_TAG))
				.resultOrPartial(error -> EchoStorage.LOGGER.warn("Echo Relay at {} forgot some chests: {}", getBlockPos(), error))
				.orElse(List.of()));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put(HEARD_TAG, LinkedChest.CODEC.listOf().encodeStart(NbtOps.INSTANCE, heard).getOrThrow());
	}

	private record Echo(BlockPos chest, long due) {
	}
}
