package dev.hefker.echostorage.menu;

import java.util.Optional;
import java.util.function.Predicate;

import dev.hefker.echostorage.block.AbstractEchoChestBlock;
import dev.hefker.echostorage.block.EchoChestAssignment;
import dev.hefker.echostorage.block.EchoChestBlock;
import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoShulkerBoxBlock;
import dev.hefker.echostorage.block.EchoShulkerBoxBlockEntity;
import dev.hefker.echostorage.block.QuickStack;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.config.EchoConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * An open Echo Chest: its slots over the player's inventory, laid out like a vanilla chest of as
 * many rows. One menu type serves every size; the open-data says how many rows to lay out.
 *
 * <p>Shift-clicking moves stacks between slots and never looks inside bundles; writing into a
 * nested bundle is the quick-stack button's job alone (ADR-0007). A strict chest refuses a
 * stray from shift-click and quick-stack alike (ADR-0009), but a stack placed by hand always goes
 * in: that is the player choosing to.
 *
 * <p>An Echo Shulker Box's slots take no shulker box of any kind, even by hand, as vanilla's
 * shulker box slots do; the open-data's kind says which chest this is, so the client predicts it.
 *
 * <p>The chest's Category and strictness, and an Echo Shulker Box's Vacuum toggle, ride along as
 * vanilla data slots, so an open screen follows every change, and the screen changes them with
 * vanilla menu-button clicks — which the server already ignores for a menu the player no longer has
 * open or could not still use. Only an Echo Shulker Box's menu takes the Vacuum buttons.
 */
public class EchoChestMenu extends AbstractContainerMenu {
	// Buttons say what the chest should become, never "flip it", so a click from a screen that
	// had not yet seen someone else's change cannot undo it.
	public static final int PERMISSIVE_BUTTON = 0;
	public static final int STRICT_BUTTON = 1;
	/** An action, not a state: runs quick-stack once on the server, however often it arrives. */
	public static final int QUICK_STACK_BUTTON = 2;
	public static final int VACUUM_OFF_BUTTON = 3;
	public static final int VACUUM_ON_BUTTON = 4;
	/** Followed by one button per preset, in {@link Categories#ALL} order. */
	public static final int CLEAR_CATEGORY_BUTTON = 5;

	private static final int CATEGORY_DATA = 0;
	private static final int STRICT_DATA = 1;
	private static final int VACUUM_DATA = 2;
	private static final int DATA_COUNT = 3;

	private final Container container;
	private final ContainerData settings;
	private final EchoChestMenuData data;
	/** The chest's kind, from the open-data, so the client knows it as well as the server. */
	private final Block block;
	/** Who may go on using the menu when it was opened from an Echo Interface; null when opened in person. */
	@Nullable
	private final Predicate<Player> remoteReach;

	/** Client-side constructor: the container is a stand-in the server syncs contents into. */
	public EchoChestMenu(int containerId, Inventory playerInventory, EchoChestMenuData data) {
		this(containerId, playerInventory, new SimpleContainer(data.rows() * EchoChestBlock.SLOTS_PER_ROW),
				new SimpleContainerData(DATA_COUNT), data, null);
	}

	/**
	 * Server-side. With a {@code remoteReach} the chest was opened from an Echo Interface: the
	 * player stays in the menu while that says so rather than while they stand at the chest, and
	 * the lid stays shut, since vanilla's lid counts only players standing near.
	 */
	public EchoChestMenu(int containerId, Inventory playerInventory, EchoChestBlockEntity chest, EchoChestMenuData data,
			@Nullable Predicate<Player> remoteReach) {
		this(containerId, playerInventory, chest, settingsOf(chest), data, remoteReach);
	}

	private EchoChestMenu(int containerId, Inventory playerInventory, Container container, ContainerData settings,
			EchoChestMenuData data, @Nullable Predicate<Player> remoteReach) {
		super(EchoMenus.ECHO_CHEST, containerId);
		checkContainerSize(container, data.rows() * EchoChestBlock.SLOTS_PER_ROW);
		checkContainerDataCount(settings, DATA_COUNT);
		this.container = container;
		this.settings = settings;
		this.data = data;
		this.remoteReach = remoteReach;
		this.block = Block.byItem(BuiltInRegistries.ITEM.get(data.kind()));
		if (remoteReach == null) {
			container.startOpen(playerInventory.player);
		}
		addDataSlots(settings);

		int playerInventoryTop = 103 + (data.rows() - 4) * 18;

		for (int row = 0; row < data.rows(); row++) {
			for (int column = 0; column < EchoChestBlock.SLOTS_PER_ROW; column++) {
				int slot = column + row * EchoChestBlock.SLOTS_PER_ROW;
				int x = 8 + column * 18;
				int y = 18 + row * 18;
				addSlot(refusesShulkerBoxes() ? new ShulkerBoxSlot(container, slot, x, y) : new Slot(container, slot, x, y));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, playerInventoryTop + row * 18));
			}
		}

		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(playerInventory, column, 8 + column * 18, playerInventoryTop + 58));
		}
	}

	/** What this menu shows: on the server the chest itself, on the client a stand-in. */
	public Container container() {
		return container;
	}

	public EchoChestMenuData data() {
		return data;
	}

	/** How many of the menu's slots are the chest's; the player's inventory follows them. */
	public int chestSlots() {
		return data.rows() * EchoChestBlock.SLOTS_PER_ROW;
	}

	/** The button that assigns the chest to {@code category}. */
	public static int assignButton(Category category) {
		return CategoryData.assignButton(CLEAR_CATEGORY_BUTTON, category);
	}

	/** The chest's Category as last synced: on the server the chest's own, on the client a copy. */
	public Optional<Category> category() {
		return CategoryData.decode(settings.get(CATEGORY_DATA));
	}

	public boolean isStrict() {
		return settings.get(STRICT_DATA) != 0;
	}

	/**
	 * Whether this is an Echo Shulker Box's menu, whose screen shows the Vacuum button. Asked of
	 * the kind rather than of {@link #refusesShulkerBoxes}: having a Vacuum toggle and refusing
	 * shulker boxes are separate rules that today pick out the same kind.
	 */
	public boolean isShulkerBox() {
		return block instanceof EchoShulkerBoxBlock;
	}

	/** Whether the chest holds no shulker boxes, even placed by hand: an Echo Shulker Box. */
	private boolean refusesShulkerBoxes() {
		return AbstractEchoChestBlock.refusesShulkerBoxes(block);
	}

	/** An Echo Shulker Box's Vacuum toggle as last synced; always off for any other chest. */
	public boolean vacuums() {
		return settings.get(VACUUM_DATA) != 0;
	}

	/** Whether {@code stack} falls outside the chest's Category. An unassigned chest has no strays. */
	public boolean isStray(ItemStack stack) {
		return EchoChestBlockEntity.isStray(category(), stack);
	}

	/** The chest's own rule, read from the synced data so the client predicts what the server does. */
	private boolean refuses(ItemStack stack) {
		return EchoChestBlockEntity.refuses(
				refusesShulkerBoxes(), new EchoChestAssignment(category(), isStrict()), stack);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button == QUICK_STACK_BUTTON) {
			// A switched-off button is refused here, not only hidden, so no client can press it (ADR-0010).
			if (!EchoConfig.get().chestQuickStack()) {
				return false;
			}
			quickStack(player);
			return true;
		}
		if (button == PERMISSIVE_BUTTON || button == STRICT_BUTTON) {
			settings.set(STRICT_DATA, button == STRICT_BUTTON ? 1 : 0);
			return true;
		}
		if (button == VACUUM_OFF_BUTTON || button == VACUUM_ON_BUTTON) {
			// No other kind of chest has the toggle, so no client can set one there.
			if (!isShulkerBox()) {
				return false;
			}
			settings.set(VACUUM_DATA, button == VACUUM_ON_BUTTON ? 1 : 0);
			return true;
		}
		int category = button - CLEAR_CATEGORY_BUTTON;
		if (CategoryData.isValid(category)) {
			settings.set(CATEGORY_DATA, category);
			return true;
		}
		return false;
	}

	/**
	 * Server-only: the whole transfer is worked out and written here, and the client learns the
	 * result from the slot sync that follows every button click. The hotbar is left alone, so
	 * what the player is holding stays at hand.
	 */
	private void quickStack(Player player) {
		if (!player.level().isClientSide()) {
			QuickStack.run(container, QuickStack.wanted(container, category()), this::refuses,
					player.getInventory(), Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return remoteReach == null ? container.stillValid(player) : remoteReach.test(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		int containerSlots = chestSlots();
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack inSlot = slot.getItem();
		ItemStack original = inSlot.copy();

		if (index < containerSlots) {
			if (!moveItemStackTo(inSlot, containerSlots, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (refuses(inSlot) || !moveItemStackTo(inSlot, 0, containerSlots, false)) {
			return ItemStack.EMPTY;
		}

		if (inSlot.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (remoteReach == null) {
			container.stopOpen(player);
		}
	}

	/** The server's data slots: read from the chest, and written straight back to it. */
	private static ContainerData settingsOf(EchoChestBlockEntity chest) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case CATEGORY_DATA -> CategoryData.encode(chest.category());
					case STRICT_DATA -> chest.isStrict() ? 1 : 0;
					case VACUUM_DATA -> chest instanceof EchoShulkerBoxBlockEntity box && box.vacuums() ? 1 : 0;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case CATEGORY_DATA -> chest.assign(CategoryData.decode(value).orElse(null));
					case STRICT_DATA -> chest.setStrict(value != 0);
					case VACUUM_DATA -> {
						if (chest instanceof EchoShulkerBoxBlockEntity box) {
							box.setVacuum(value != 0);
						}
					}
					default -> {
					}
				}
			}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		};
	}
}
