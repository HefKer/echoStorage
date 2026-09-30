package dev.hefker.echostorage.block;

import java.util.List;
import java.util.Optional;

import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An Echo Shulker Box, placed: an Echo Chest in every respect, which also keeps its contents and
 * its colour on the item it drops as, the way a vanilla shulker box does. Its id still dies with
 * the block (ADR-0008), so placing it again makes a new chest.
 *
 * <p>It also keeps its Vacuum toggle, set from its screen while it is placed and acted on only
 * while it is carried: see {@link dev.hefker.echostorage.item.Vacuum}.
 *
 * <p>No shulker box of any kind goes in, from any source: hoppers and quick-stack ask
 * {@link #refuses}, Vacuum asks {@link #putIntoItem}, and the menu's slots refuse them as vanilla's
 * shulker box slots do, all because an Echo Shulker Box
 * {@link EchoShulkerBoxBlock#REFUSES_SHULKER_BOXES refuses shulker boxes}.
 *
 * <p>The lid is vanilla's shulker lid: it rises and turns over ten ticks on both sides, pushing
 * whatever is in the way, and while it is open the block's shape follows it.
 */
public class EchoShulkerBoxBlockEntity extends EchoChestBlockEntity {
	private static final int ROWS = 3;
	/** How many slots a box has, placed or carried. */
	public static final int SLOTS = ROWS * EchoChestBlock.SLOTS_PER_ROW;
	private static final String COLOR_TAG = "Color";
	private static final String VACUUM_TAG = "Vacuum";
	/** How far the lid moves each tick: open in ten. */
	private static final float LID_STEP = 0.1F;
	/** How far the lid rises when fully open, in blocks. */
	private static final float MAX_LID_HEIGHT = 0.5F;

	@Nullable
	private DyeColor color;
	private boolean vacuum;
	private AnimationStatus animationStatus = AnimationStatus.CLOSED;
	private float progress;
	private float progressOld;

	public EchoShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
		super(EchoBlocks.ECHO_SHULKER_BOX_ENTITY, pos, state, ROWS);
	}

	/** The dye this box was coloured with; empty when it is undyed. */
	@Override
	public Optional<DyeColor> color() {
		return Optional.ofNullable(color);
	}

	/** Whether the box vacuums what the player picks up while they carry it; off until they turn it on. */
	public boolean vacuums() {
		return vacuum;
	}

	public void setVacuum(boolean vacuum) {
		this.vacuum = vacuum;
		setChanged();
	}

	/**
	 * Puts as much of {@code moving} into a carried {@code box} as Quick-stacking it into the box,
	 * placed, would take, strictness and the bundles inside included, shrinking {@code moving} by
	 * what went in. Returns the box as written, or null if it took nothing.
	 */
	@Nullable
	public static ItemStack putIntoItem(ItemStack box, ItemStack moving) {
		SimpleContainer slots = new SimpleContainer(SLOTS);
		box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots.getItems());
		EchoChestAssignment assignment = box.getOrDefault(EchoComponents.ECHO_CHEST_ASSIGNMENT, EchoChestAssignment.DEFAULT);
		int before = moving.getCount();
		QuickStack.put(slots, QuickStack.wanted(slots, assignment.category()),
				stack -> refuses(EchoShulkerBoxBlock.REFUSES_SHULKER_BOXES, assignment.category(), assignment.strict(), stack), moving);
		if (moving.getCount() == before) {
			return null;
		}
		ItemStack written = box.copy();
		written.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots.getItems()));
		return written;
	}

	// --- the item side --------------------------------------------------------------------

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		color = tag.contains(COLOR_TAG, CompoundTag.TAG_STRING) ? DyeColor.byName(tag.getString(COLOR_TAG), null) : null;
		vacuum = tag.getBoolean(VACUUM_TAG);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		saveColor(tag);
		if (vacuum) {
			tag.putBoolean(VACUUM_TAG, true);
		}
	}

	private void saveColor(CompoundTag tag) {
		if (color != null) {
			tag.putString(COLOR_TAG, color.getName());
		}
	}

	/** Clients draw the box in its colour, so it is sent along with whatever an Echo Chest sends. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		saveColor(tag);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The contents, colour and Vacuum toggle travel with the item as well as the name and assignment. */
	@Override
	protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
		super.applyImplicitComponents(components);
		components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items());
		color = components.get(DataComponents.BASE_COLOR);
		vacuum = components.get(EchoComponents.ECHO_SHULKER_BOX_VACUUM) != null;
	}

	/** Always writes the contents, as vanilla's shulker box does; the item stacks to one anyway. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items()));
		if (color != null) {
			components.set(DataComponents.BASE_COLOR, color);
		}
		if (vacuum) {
			components.set(EchoComponents.ECHO_SHULKER_BOX_VACUUM, Unit.INSTANCE);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("Items");
		tag.remove(COLOR_TAG);
		tag.remove(VACUUM_TAG);
	}

	// --- the lid --------------------------------------------------------------------------

	@Override
	public boolean triggerEvent(int event, int value) {
		if (event == EVENT_SET_OPEN_COUNT) {
			if (value == 0) {
				animationStatus = AnimationStatus.CLOSING;
			} else if (value == 1) {
				animationStatus = AnimationStatus.OPENING;
			}
			return true;
		}
		return super.triggerEvent(event, value);
	}

	/** Vanilla's shulker box animation, on both sides: the server needs the shape, the client the look. */
	static void tick(Level level, BlockPos pos, BlockState state, EchoShulkerBoxBlockEntity box) {
		box.updateAnimation(level, pos, state);
	}

	private void updateAnimation(Level level, BlockPos pos, BlockState state) {
		progressOld = progress;
		switch (animationStatus) {
			case CLOSED -> progress = 0.0F;
			case OPENING -> {
				progress += LID_STEP;
				if (progressOld == 0.0F) {
					doNeighborUpdates(level, pos, state);
				}
				if (progress >= 1.0F) {
					animationStatus = AnimationStatus.OPENED;
					progress = 1.0F;
					doNeighborUpdates(level, pos, state);
				}
				moveCollidedEntities(level, pos, state);
			}
			case OPENED -> progress = 1.0F;
			case CLOSING -> {
				progress -= LID_STEP;
				if (progressOld == 1.0F) {
					doNeighborUpdates(level, pos, state);
				}
				if (progress <= 0.0F) {
					animationStatus = AnimationStatus.CLOSED;
					progress = 0.0F;
					doNeighborUpdates(level, pos, state);
				}
			}
		}
	}

	/** Its shape changes as the lid moves, so the blocks around it are told. */
	private static void doNeighborUpdates(Level level, BlockPos pos, BlockState state) {
		state.updateNeighbourShapes(level, pos, 3);
		level.updateNeighborsAt(pos, state.getBlock());
	}

	/** An opening lid lifts whatever sits on it, as vanilla's does. */
	private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof EchoShulkerBoxBlock)) {
			return;
		}
		Direction facing = state.getValue(EchoShulkerBoxBlock.FACING);
		AABB swept = Shulker.getProgressDeltaAabb(1.0F, facing, progressOld, progress).move(pos);
		List<Entity> entities = level.getEntities(null, swept);
		for (Entity entity : entities) {
			if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
				entity.move(MoverType.SHULKER_BOX, new Vec3(
						(swept.getXsize() + 0.01) * facing.getStepX(),
						(swept.getYsize() + 0.01) * facing.getStepY(),
						(swept.getZsize() + 0.01) * facing.getStepZ()));
			}
		}
	}

	/** Whether the lid is fully shut, which is when the box is a full block and blocks sight. */
	public boolean isClosed() {
		return animationStatus == AnimationStatus.CLOSED;
	}

	/** The box's shape as the lid stands now. */
	AABB boundingBox(BlockState state) {
		return Shulker.getProgressAabb(1.0F, state.getValue(EchoShulkerBoxBlock.FACING), MAX_LID_HEIGHT * getOpenNess(1.0F));
	}

	@Override
	public float getOpenNess(float partialTick) {
		return Mth.lerp(partialTick, progressOld, progress);
	}

	@Override
	protected SoundEvent openSound() {
		return SoundEvents.SHULKER_BOX_OPEN;
	}

	@Override
	protected SoundEvent closeSound() {
		return SoundEvents.SHULKER_BOX_CLOSE;
	}

	private enum AnimationStatus {
		CLOSED,
		OPENING,
		OPENED,
		CLOSING
	}
}
