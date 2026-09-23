package dev.hefker.echostorage.block;

import java.util.Optional;
import java.util.UUID;

import dev.hefker.echostorage.EchoStorage;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.EchoComponents;
import dev.hefker.echostorage.menu.EchoChestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
	private static final String CATEGORY_TAG = "Category";
	private static final String STRICT_TAG = "Strict";
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
	@Nullable
	private Category category;
	private boolean strict;

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

	/**
	 * The name as the player typed it, empty if the chest has none. Held to the rename rules
	 * even when it came from an item named some other way, such as by a command.
	 */
	public String name() {
		return name == null ? "" : EchoChestName.sanitize(name.getString());
	}

	/** Renames the chest to what {@code typed} sanitises to; blank clears the name. */
	public void rename(String typed) {
		String accepted = EchoChestName.sanitize(typed);
		name = accepted.isEmpty() ? null : Component.literal(accepted);
		setChanged();
	}

	/** The Category this chest is assigned to hold, if any. */
	public Optional<Category> category() {
		return Optional.ofNullable(category);
	}

	/**
	 * Assigns the chest to {@code category}, or clears it with null. Allowed at any time, on any
	 * chest, and never moves or refuses what is already inside.
	 */
	public void assign(@Nullable Category category) {
		this.category = category;
		setChanged();
	}

	/** Whether this chest refuses items outside its Category on shift-click and hopper insert. */
	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
		setChanged();
	}

	/** Whether {@code stack} is kept out: only by a strict chest, and only if it is not in the Category. */
	public boolean refuses(ItemStack stack) {
		return refuses(category(), strict, stack);
	}

	/** The rule itself, shared with the menu so a client screen can predict it. */
	public static boolean refuses(Optional<Category> category, boolean strict, ItemStack stack) {
		return strict && isStray(category, stack);
	}

	/**
	 * Whether {@code stack} falls outside {@code category}. With no Category nothing is a stray.
	 * An Echo Bundle is judged by what it holds, one level deep and read-only (ADR-0007), so an
	 * empty one — capacity waiting to be filled — belongs in any chest.
	 */
	public static boolean isStray(Optional<Category> category, ItemStack stack) {
		return category.filter(assigned -> !belongs(assigned, stack)).isPresent();
	}

	private static boolean belongs(Category category, ItemStack stack) {
		EchoBundleContents contents = stack.get(EchoComponents.ECHO_BUNDLE_CONTENTS);
		if (contents == null) {
			return category.matches(stack);
		}
		for (ItemStack inside : contents.items()) {
			if (!category.matches(inside)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * What the chest is called on screen: its typed name, else its Category's name in italics,
	 * else "Echo Chest". The Category fallback is display-only — never stored as the custom name,
	 * so clearing the Category strands no name the player never typed.
	 */
	@Override
	public Component getName() {
		return name != null ? name : unnamedTitle(category());
	}

	/** What an Echo Chest with no typed name is called: its Category in italics, else "Echo Chest". */
	public static Component unnamedTitle(Optional<Category> category) {
		return category.<Component>map(assigned -> assigned.displayName().copy().withStyle(ChatFormatting.ITALIC))
				.orElseGet(EchoBlocks.ECHO_CHEST::getName);
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
		category = tag.contains(CATEGORY_TAG) ? loadCategory(tag) : null;
		strict = tag.getBoolean(STRICT_TAG);
		items.clear();
		ContainerHelper.loadAllItems(tag, items, registries);
	}

	/**
	 * A Category saved under a name that no longer ships loads as none, so the chest comes back
	 * unassigned rather than failing to load its contents and name with it.
	 */
	@Nullable
	private Category loadCategory(CompoundTag tag) {
		return Categories.CODEC.parse(NbtOps.INSTANCE, tag.get(CATEGORY_TAG))
				.resultOrPartial(error -> EchoStorage.LOGGER.warn("Echo Chest at {} lost its Category: {}", getBlockPos(), error))
				.orElse(null);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putUUID(ID_TAG, id);
		if (name != null) {
			tag.putString(NAME_TAG, Component.Serializer.toJson(name, registries));
		}
		if (category != null) {
			tag.put(CATEGORY_TAG, Categories.CODEC.encodeStart(NbtOps.INSTANCE, category).getOrThrow());
		}
		if (strict) {
			tag.putBoolean(STRICT_TAG, true);
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

	/** What hoppers and droppers ask before inserting. A player placing by hand is never refused. */
	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !refuses(stack);
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
