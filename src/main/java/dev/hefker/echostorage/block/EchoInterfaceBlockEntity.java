package dev.hefker.echostorage.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.link.LinkedChests;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.link.Links;
import dev.hefker.echostorage.link.Resolution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Echo Interface: the Echo Chests its Links reach, as rows it keeps between visits (ADR-0004).
 * It lists chests and opens one; it never pools their contents (ADR-0002).
 *
 * <p>The rows are re-resolved when the interface is opened and, while anyone has it open, once a
 * second, which is what catches sculk changing around it. Nothing reads the rows while the screen
 * is closed, so nothing watches the sculk then either: a closed interface costs nothing.
 */
public class EchoInterfaceBlockEntity extends BlockEntity {
	/** How often an open interface re-resolves, so a Link cut or built while it is open shows up. */
	private static final int REFRESH_TICKS = 20;
	/** How many ticks the link visual spends on each step out along the path. */
	private static final int TRACE_TICKS_PER_STEP = 2;
	/** Ticks the link visual rests at the far end before it starts again from the interface. */
	private static final int TRACE_PAUSE_TICKS = 20;
	private static final String ROWS_TAG = "Rows";

	private final LinkedChests rows = new LinkedChests();
	private Resolution lastResolution = new Resolution(List.of(), List.of(), List.of(), true);
	private long resolvedAt = Long.MIN_VALUE;

	public EchoInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(EchoBlocks.ECHO_INTERFACE_ENTITY, pos, state);
	}

	public List<Row> rows() {
		return rows.rows();
	}

	/** Server-only: follows the Links out again now and brings the rows up to date. */
	public void resolve() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		Map<UUID, BlockPos> known = new HashMap<>();
		rows.rows().forEach(row -> known.put(row.id(), row.pos()));
		lastResolution = Links.resolve(getBlockPos(), new LevelLinks(serverLevel), known);
		resolvedAt = serverLevel.getGameTime();
		List<Row> before = rows.rows();
		rows.update(lastResolution, chest -> labelOf(serverLevel, chest.pos()), serverLevel::isLoaded);
		if (!rows.rows().equals(before)) {
			setChanged();
		}
	}

	/** Re-resolves unless that was done within the last second, however many players are looking. */
	public void refresh() {
		if (level != null && level.getGameTime() - resolvedAt >= REFRESH_TICKS) {
			resolve();
		}
	}

	/** Takes a greyed row off the list. */
	public void dismiss(UUID id) {
		if (rows.dismiss(id)) {
			setChanged();
		}
	}

	/**
	 * The chest a row names, if it is linked and standing now. Checked against the world, not
	 * just the row, so a chest broken since the last resolution is never handed out.
	 */
	public Optional<EchoChestBlockEntity> linkedChest(UUID id) {
		return rows.rows().stream()
				.filter(row -> row.id().equals(id) && row.state() == LinkedChests.State.LINKED)
				.findFirst()
				.flatMap(row -> chestAt(row.pos()))
				.filter(chest -> chest.id().equals(id));
	}

	/**
	 * Global quick-stack: {@link QuickStack} into every linked chest, from the player's main
	 * inventory, in two passes (ADR-0010): first into the chests that already hold an item, then
	 * into those whose Category matches it, so like items join each other before a Category
	 * chest starts a new pile, whatever order the list is in. It reaches exactly what the list
	 * reaches — there is no second, wider definition of nearby.
	 */
	public void quickStack(Player player) {
		resolve();
		List<EchoChestBlockEntity> chests = rows.rows().stream()
				.flatMap(row -> linkedChest(row.id()).stream())
				.toList();
		for (EchoChestBlockEntity chest : chests) {
			QuickStack.run(chest, QuickStack.holds(chest), chest::refuses,
					player.getInventory(), Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE);
		}
		for (EchoChestBlockEntity chest : chests) {
			QuickStack.run(chest, QuickStack.inCategory(chest.category()), chest::refuses,
					player.getInventory(), Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE);
		}
	}

	/** Whether {@code player} is close enough to go on using this interface. */
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	/**
	 * The link visual: sculk particles run out along the last resolved path, a step at a time,
	 * shown to {@code viewer} alone. As they reach an Echo Relay, a vibration goes out to each
	 * chest it linked and an echo comes back. Called each tick the viewer has the screen open.
	 */
	public void traceFor(ServerPlayer viewer) {
		List<List<BlockPos>> path = lastResolution.path();
		if (path.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		int tick = (int) (serverLevel.getGameTime() % (path.size() * TRACE_TICKS_PER_STEP + TRACE_PAUSE_TICKS));
		if (tick % TRACE_TICKS_PER_STEP == 0 && tick / TRACE_TICKS_PER_STEP < path.size()) {
			for (BlockPos pos : path.get(tick / TRACE_TICKS_PER_STEP)) {
				serverLevel.sendParticles(viewer, ParticleTypes.SCULK_CHARGE_POP, false,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.15, 0.15, 0.15, 0.0);
			}
		}
		for (Resolution.Hop hop : lastResolution.hops()) {
			int reached = stepOf(path, hop.relay()) * TRACE_TICKS_PER_STEP;
			if (tick == reached) {
				EchoRelayBlockEntity.vibrate(serverLevel, viewer, hop.relay(), hop.chest());
			} else if (tick == reached + EchoRelayBlockEntity.travelTicks(hop.relay(), hop.chest())) {
				EchoRelayBlockEntity.vibrate(serverLevel, viewer, hop.chest(), hop.relay());
			}
		}
	}

	/** How many steps out along {@code path} the connector at {@code pos} is. */
	private static int stepOf(List<List<BlockPos>> path, BlockPos pos) {
		for (int step = 0; step < path.size(); step++) {
			if (path.get(step).contains(pos)) {
				return step;
			}
		}
		throw new IllegalStateException("A hop from " + pos + ", which the path does not reach");
	}

	private Optional<EchoChestBlockEntity> chestAt(BlockPos pos) {
		return level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest
				? Optional.of(chest)
				: Optional.empty();
	}

	private static LinkedChests.Label labelOf(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EchoChestBlockEntity chest
				? new LinkedChests.Label(chest.name(), chest.category())
				: new LinkedChests.Label("", Optional.empty());
	}

	// --- persistence ----------------------------------------------------------------------

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		rows.replace(Row.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get(ROWS_TAG))
				.resultOrPartial(error -> EchoStorage.LOGGER.warn("Echo Interface at {} lost some rows: {}", getBlockPos(), error))
				.orElse(List.of()));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put(ROWS_TAG, Row.CODEC.listOf().encodeStart(NbtOps.INSTANCE, rows.rows()).getOrThrow());
	}
}
