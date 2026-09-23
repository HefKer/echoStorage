package dev.hefker.echostorage.block;

import java.util.UUID;

import dev.hefker.echostorage.menu.EchoChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An Echo Chest: 27 vanilla slots (ADR-0005), an id, and a name the player typed.
 *
 * <p>The id is assigned when the block entity is made and again when a player places the
 * chest, and it is never written to the dropped item — so it dies with the block, and a
 * chest broken and replaced is a different chest. The name is a vanilla custom name and
 * does travel with the item, which is what stops a named chest stacking with a blank one.
 *
 * <p>This is a plain {@link BlockEntity} rather than a {@code BaseContainerBlockEntity}
 * because that class keeps its custom name private with no way to change it after placement,
 * and renaming in place is the point.
 */
public class EchoChestBlockEntity extends BlockEntity implements Container, Nameable, LidBlockEntity {
	public static final int SLOTS = 27;

	private static final String ID_TAG = "EchoChestId";
	private static final String NAME_TAG = "CustomName";
	private static final int EVENT_SET_OPEN_COUNT = 1;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final ChestLidController lid = new ChestLidController();
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, SoundEvents.CHEST_OPEN);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, SoundEvents.CHEST_CLOSE);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int after) {
			level.blockEvent(pos, state.getBlock(), EVENT_SET_OPEN_COUNT, after);
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof EchoChestMenu menu && menu.container() == EchoChestBlockEntity.this;
		}
	};

	private UUID id = UUID.randomUUID();
	@Nullable
	private Component name;

	public EchoChestBlockEntity(BlockPos pos, BlockState state) {
		super(EchoBlocks.ECHO_CHEST_ENTITY, pos, state);
	}

	/** This chest's identity: stable while it stands, never shared, never carried by the item. */
	public UUID id() {
		return id;
	}

	/** Called when a player places the chest, so data copied from another chest cannot clone its id. */
	void assignNewId() {
		id = UUID.randomUUID();
		setChanged();
	}

	/** The name as the player typed it, empty if the chest has none. */
	public String name() {
		return name == null ? "" : name.getString();
	}

	/** Renames the chest to what {@code typed} sanitises to; blank clears the name. */
	public void rename(String typed) {
		String accepted = EchoChestName.sanitize(typed);
		name = accepted.isEmpty() ? null : Component.literal(accepted);
		setChanged();
	}

	@Override
	public Component getName() {
		return name != null ? name : getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public Component getCustomName() {
		return name;
	}

	// --- persistence ----------------------------------------------------------------------

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.hasUUID(ID_TAG)) {
			id = tag.getUUID(ID_TAG);
		}
		name = tag.contains(NAME_TAG, CompoundTag.TAG_STRING)
				? parseCustomNameSafe(tag.getString(NAME_TAG), registries)
				: null;
		items.clear();
		ContainerHelper.loadAllItems(tag, items, registries);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putUUID(ID_TAG, id);
		if (name != null) {
			tag.putString(NAME_TAG, Component.Serializer.toJson(name, registries));
		}
		ContainerHelper.saveAllItems(tag, items, registries);
	}

	/** The item side of the name: read on placement, written when the block drops. */
	@Override
	protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
		super.applyImplicitComponents(components);
		name = components.get(DataComponents.CUSTOM_NAME);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (name != null) {
			components.set(DataComponents.CUSTOM_NAME, name);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove(NAME_TAG);
	}

	// --- container ------------------------------------------------------------------------

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
		if (!removed.isEmpty()) {
			setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	// --- opening and the lid --------------------------------------------------------------

	@Override
	public void startOpen(Player player) {
		if (!remove && !player.isSpectator()) {
			openers.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
		}
	}

	@Override
	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator()) {
			openers.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
		}
	}

	/** Catches players who stopped viewing without closing, as vanilla chests do. */
	void recheckOpen() {
		if (!remove) {
			openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
		}
	}

	@Override
	public boolean triggerEvent(int event, int value) {
		if (event == EVENT_SET_OPEN_COUNT) {
			lid.shouldBeOpen(value > 0);
			return true;
		}
		return super.triggerEvent(event, value);
	}

	static void lidAnimateTick(Level level, BlockPos pos, BlockState state, EchoChestBlockEntity chest) {
		chest.lid.tickLid();
	}

	@Override
	public float getOpenNess(float partialTick) {
		return lid.getOpenness(partialTick);
	}

	private static void playSound(Level level, BlockPos pos, SoundEvent sound) {
		level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
	}
}
