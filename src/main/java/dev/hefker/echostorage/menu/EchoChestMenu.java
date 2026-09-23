package dev.hefker.echostorage.menu;

import java.util.Optional;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
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
 * nested bundle is quick-stack's job alone (ADR-0007).
 *
 * <p>The chest's Category and strictness ride along as vanilla data slots, so an open screen
 * follows every change, and the screen changes them with vanilla menu-button clicks — which the
 * server already ignores for a menu the player no longer has open or could not still use.
 */
public class EchoChestMenu extends AbstractContainerMenu {
	private static final int SLOTS_PER_ROW = 9;
	private static final int ROWS = EchoChestBlockEntity.SLOTS / SLOTS_PER_ROW;

	public static final int TOGGLE_STRICT_BUTTON = 0;
	public static final int CLEAR_CATEGORY_BUTTON = 1;
	private static final int FIRST_CATEGORY_BUTTON = 2;

	private static final int CATEGORY_DATA = 0;
	private static final int STRICT_DATA = 1;
	private static final int DATA_COUNT = 2;
	private static final int NO_CATEGORY = -1;

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
		int index = Categories.ALL.indexOf(category);
		if (index < 0) {
			throw new IllegalArgumentException("not a preset: " + category.name());
		}
		return FIRST_CATEGORY_BUTTON + index;
	}

	/** The chest's Category as last synced: on the server the chest's own, on the client a copy. */
	public Optional<Category> category() {
		int index = assignment.get(CATEGORY_DATA);
		return index >= 0 && index < Categories.ALL.size() ? Optional.of(Categories.ALL.get(index)) : Optional.empty();
	}

	public boolean isStrict() {
		return assignment.get(STRICT_DATA) != 0;
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button == TOGGLE_STRICT_BUTTON) {
			assignment.set(STRICT_DATA, isStrict() ? 0 : 1);
			return true;
		}
		if (button == CLEAR_CATEGORY_BUTTON) {
			assignment.set(CATEGORY_DATA, NO_CATEGORY);
			return true;
		}
		int index = button - FIRST_CATEGORY_BUTTON;
		if (index >= 0 && index < Categories.ALL.size()) {
			assignment.set(CATEGORY_DATA, index);
			return true;
		}
		return false;
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
		} else if (!moveItemStackTo(inSlot, 0, containerSlots, false)) {
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
					case CATEGORY_DATA -> chest.category().map(Categories.ALL::indexOf).orElse(NO_CATEGORY);
					case STRICT_DATA -> chest.isStrict() ? 1 : 0;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case CATEGORY_DATA -> chest.assign(value == NO_CATEGORY ? null : Categories.ALL.get(value));
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
