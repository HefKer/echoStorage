package dev.hefker.echostorage.menu;

import java.util.Optional;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.QuickStack;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An open Echo Chest: its 27 slots over the player's inventory, laid out like a vanilla chest.
 *
 * <p>Shift-clicking moves stacks between slots and never looks inside bundles; writing into a
 * nested bundle is the quick-stack button's job alone (ADR-0007). A strict chest refuses a
 * shift-clicked stray, but a stack placed by hand always goes in: that is the player choosing to.
 *
 * <p>The chest's Category and strictness ride along as vanilla data slots, so an open screen
 * follows every change, and the screen changes them with vanilla menu-button clicks — which the
 * server already ignores for a menu the player no longer has open or could not still use.
 */
public class EchoChestMenu extends AbstractContainerMenu {
	private static final int SLOTS_PER_ROW = 9;
	private static final int ROWS = EchoChestBlockEntity.SLOTS / SLOTS_PER_ROW;

	// Buttons say what the chest should become, never "flip it", so a click from a screen that
	// had not yet seen someone else's change cannot undo it.
	public static final int PERMISSIVE_BUTTON = 0;
	public static final int STRICT_BUTTON = 1;
	/** An action, not a state: runs quick-stack once on the server, however often it arrives. */
	public static final int QUICK_STACK_BUTTON = 2;
	/** Followed by one button per preset, in {@link Categories#ALL} order. */
	public static final int CLEAR_CATEGORY_BUTTON = 3;

	private static final int CATEGORY_DATA = 0;
	private static final int STRICT_DATA = 1;
	private static final int DATA_COUNT = 2;

	private final Container container;
	private final ContainerData assignment;
	private final EchoChestMenuData data;

	/** Client-side constructor: the container is a stand-in the server syncs contents into. */
	public EchoChestMenu(int containerId, Inventory playerInventory, EchoChestMenuData data) {
		this(containerId, playerInventory, new SimpleContainer(EchoChestBlockEntity.SLOTS), new SimpleContainerData(DATA_COUNT), data);
	}

	public EchoChestMenu(int containerId, Inventory playerInventory, EchoChestBlockEntity chest, EchoChestMenuData data) {
		this(containerId, playerInventory, chest, assignmentOf(chest), data);
	}

	private EchoChestMenu(int containerId, Inventory playerInventory, Container container, ContainerData assignment,
			EchoChestMenuData data) {
		super(EchoMenus.ECHO_CHEST, containerId);
		checkContainerSize(container, EchoChestBlockEntity.SLOTS);
		checkContainerDataCount(assignment, DATA_COUNT);
		this.container = container;
		this.assignment = assignment;
		this.data = data;
		container.startOpen(playerInventory.player);
		addDataSlots(assignment);

		int playerInventoryTop = 103 + (ROWS - 4) * 18;

		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < SLOTS_PER_ROW; column++) {
				addSlot(new Slot(container, column + row * SLOTS_PER_ROW, 8 + column * 18, 18 + row * 18));
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

	/** The button that assigns the chest to {@code category}. */
	public static int assignButton(Category category) {
		return CategoryData.assignButton(CLEAR_CATEGORY_BUTTON, category);
	}

	/** The chest's Category as last synced: on the server the chest's own, on the client a copy. */
	public Optional<Category> category() {
		return CategoryData.decode(assignment.get(CATEGORY_DATA));
	}

	public boolean isStrict() {
		return assignment.get(STRICT_DATA) != 0;
	}

	/** Whether {@code stack} falls outside the chest's Category. An unassigned chest has no strays. */
	public boolean isStray(ItemStack stack) {
		return EchoChestBlockEntity.isStray(category(), stack);
	}

	/** The chest's own rule, read from the synced data so the client predicts what the server does. */
	private boolean refuses(ItemStack stack) {
		return EchoChestBlockEntity.refuses(category(), isStrict(), stack);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button == QUICK_STACK_BUTTON) {
			quickStack(player);
			return true;
		}
		if (button == PERMISSIVE_BUTTON || button == STRICT_BUTTON) {
			assignment.set(STRICT_DATA, button == STRICT_BUTTON ? 1 : 0);
			return true;
		}
		int category = button - CLEAR_CATEGORY_BUTTON;
		if (CategoryData.isValid(category)) {
			assignment.set(CATEGORY_DATA, category);
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
			QuickStack.run(container, this::refuses, player.getInventory(), Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		int containerSlots = EchoChestBlockEntity.SLOTS;
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
		container.stopOpen(player);
	}

	/** The server's data slots: read from the chest, and written straight back to it. */
	private static ContainerData assignmentOf(EchoChestBlockEntity chest) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case CATEGORY_DATA -> CategoryData.encode(chest.category());
					case STRICT_DATA -> chest.isStrict() ? 1 : 0;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case CATEGORY_DATA -> chest.assign(CategoryData.decode(value).orElse(null));
					case STRICT_DATA -> chest.setStrict(value != 0);
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
