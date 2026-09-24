package dev.hefker.echostorage.menu;

import java.util.Optional;

import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.item.EchoBundleItem;
import dev.hefker.echostorage.item.EchoBundleSettings;
import dev.hefker.echostorage.item.EchoComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * An Echo Bundle's own screen: its Category and its vacuum toggle, for the bundle in the hand
 * that opened it. No slots, so nothing can move the bundle while it is open.
 *
 * <p>Built the way {@link EchoChestMenu} is: the settings ride along as data slots read from and
 * written to the bundle's component, and the screen changes them with menu-button clicks that say
 * what the bundle should become.
 */
public class EchoBundleMenu extends AbstractContainerMenu {
	public static final int VACUUM_OFF_BUTTON = 0;
	public static final int VACUUM_ON_BUTTON = 1;
	/** Followed by one button per preset, in {@link Categories#ALL} order. */
	public static final int CLEAR_CATEGORY_BUTTON = 2;

	private static final int CATEGORY_DATA = 0;
	private static final int VACUUM_DATA = 1;
	private static final int DATA_COUNT = 2;
	/** The client's menu has no bundle of its own; the server's decides when it closes. */
	private static final int NO_SLOT = -1;

	private final Inventory inventory;
	private final int slot;
	private final ContainerData settings;

	/** Client-side constructor: the settings are a stand-in the server syncs into. */
	public EchoBundleMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, NO_SLOT, new SimpleContainerData(DATA_COUNT));
	}

	/** Server-side constructor, for the bundle in {@code inventory}'s {@code slot}. */
	public EchoBundleMenu(int containerId, Inventory inventory, int slot) {
		this(containerId, inventory, slot, settingsOf(inventory, slot));
	}

	private EchoBundleMenu(int containerId, Inventory inventory, int slot, ContainerData settings) {
		super(EchoMenus.ECHO_BUNDLE, containerId);
		checkContainerDataCount(settings, DATA_COUNT);
		this.inventory = inventory;
		this.slot = slot;
		this.settings = settings;
		addDataSlots(settings);
	}

	/** The button that assigns the bundle to {@code category}. */
	public static int assignButton(Category category) {
		return CategoryData.assignButton(CLEAR_CATEGORY_BUTTON, category);
	}

	public Optional<Category> category() {
		return CategoryData.decode(settings.get(CATEGORY_DATA));
	}

	public boolean vacuums() {
		return settings.get(VACUUM_DATA) != 0;
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (button == VACUUM_OFF_BUTTON || button == VACUUM_ON_BUTTON) {
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

	/** Open only while an Echo Bundle is still in the slot it was opened from. */
	@Override
	public boolean stillValid(Player player) {
		return slot == NO_SLOT || inventory.getItem(slot).has(EchoComponents.ECHO_BUNDLE_CONTENTS);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	/** The server's data slots: read from the bundle, and written straight back to it. */
	private static ContainerData settingsOf(Inventory inventory, int slot) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				EchoBundleSettings current = EchoBundleItem.settingsOf(inventory.getItem(slot));
				return switch (index) {
					case CATEGORY_DATA -> CategoryData.encode(current.category());
					case VACUUM_DATA -> current.vacuum() ? 1 : 0;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				ItemStack bundle = inventory.getItem(slot);
				EchoBundleSettings current = EchoBundleItem.settingsOf(bundle);
				switch (index) {
					case CATEGORY_DATA -> EchoBundleItem.setSettings(bundle, current.withCategory(CategoryData.decode(value)));
					case VACUUM_DATA -> EchoBundleItem.setSettings(bundle, current.withVacuum(value != 0));
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
