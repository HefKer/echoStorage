package dev.hefker.echostorage.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.block.EchoChestName;
import dev.hefker.echostorage.category.Categories;
import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.network.NetClient;
import dev.hefker.echostorage.network.RenameEchoChestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * An open Echo Chest, drawn on the vanilla chest texture, whose title is a text field: click
 * the name, type, press enter. No anvil, no name tag.
 *
 * <p>The screen only reports what was typed. The server decides what the name becomes, so the
 * field may show untrimmed text until the chest is next opened.
 *
 * <p>Beside the chest sit its Category and strictness. Either can change at any time and neither
 * moves anything: slots holding items outside the Category are tinted, not emptied. An unnamed
 * chest with a Category shows the Category's name, in italics, where its name would be.
 */
public class EchoChestScreen extends AbstractContainerScreen<EchoChestMenu> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int ROWS = 3;
	private static final int CHEST_HEIGHT = ROWS * 18 + 17;
	/** Where the player-inventory strip starts in the vanilla texture. */
	private static final int PLAYER_INVENTORY_V = 126;
	private static final int PLAYER_INVENTORY_HEIGHT = 96;
	private static final int LABEL_COLOR = 0x404040;
	private static final int STRAY_TINT = 0x60D08040;
	private static final int BUTTON_GAP = 4;
	private static final int BUTTON_WIDTH = 90;
	private static final int BUTTON_HEIGHT = 20;

	private EditBox nameField;
	private CycleButton<Optional<Category>> categoryButton;
	private CycleButton<Boolean> strictButton;
	/** The name as the server last heard it from this screen. */
	private String sentName;

	public EchoChestScreen(EchoChestMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageHeight = 114 + ROWS * 18;
		this.inventoryLabelY = imageHeight - 94;
		this.sentName = menu.data().name();
	}

	@Override
	protected void init() {
		super.init();
		// init runs again on resize; keep whatever is being typed.
		String value = nameField == null ? sentName : nameField.getValue();

		nameField = new EditBox(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX, 10, title);
		nameField.setBordered(false);
		nameField.setMaxLength(EchoChestName.MAX_LENGTH);
		nameField.setTextColor(LABEL_COLOR);
		nameField.setValue(value);
		nameField.setHint(EchoChestBlockEntity.unnamedTitle(menu.category()));
		nameField.setTooltip(Tooltip.create(Component.translatable("container.echostorage.echo_chest.rename")));
		addRenderableWidget(nameField);

		int buttonX = leftPos + imageWidth + BUTTON_GAP;
		categoryButton = CycleButton.<Optional<Category>>builder(EchoChestScreen::categoryLabel)
				.withValues(categoryChoices())
				.withInitialValue(menu.category())
				.withTooltip(choice -> Tooltip.create(Component.translatable("container.echostorage.echo_chest.category.tooltip")))
				.displayOnlyValue()
				.create(buttonX, topPos, BUTTON_WIDTH, BUTTON_HEIGHT,
						Component.translatable("container.echostorage.echo_chest.category"),
						(button, choice) -> clickButton(choice.map(EchoChestMenu::assignButton).orElse(EchoChestMenu.CLEAR_CATEGORY_BUTTON)));
		addRenderableWidget(categoryButton);

		strictButton = CycleButton.onOffBuilder(menu.isStrict())
				.withTooltip(strict -> Tooltip.create(Component.translatable("container.echostorage.echo_chest.strict.tooltip")))
				.create(buttonX, topPos + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT,
						Component.translatable("container.echostorage.echo_chest.strict"),
						(button, strict) -> clickButton(strict ? EchoChestMenu.STRICT_BUTTON : EchoChestMenu.PERMISSIVE_BUTTON));
		addRenderableWidget(strictButton);
	}

	/** Follows changes that did not come from this screen: another player's, or the server's answer. */
	@Override
	protected void containerTick() {
		super.containerTick();
		Optional<Category> category = menu.category();
		if (!categoryButton.getValue().equals(category)) {
			categoryButton.setValue(category);
		}
		nameField.setHint(EchoChestBlockEntity.unnamedTitle(category));
		if (strictButton.getValue() != menu.isStrict()) {
			strictButton.setValue(menu.isStrict());
		}
	}

	/** Applies the click here at once, as vanilla's stonecutter does, and asks the server to agree. */
	private void clickButton(int button) {
		menu.clickMenuButton(minecraft.player, button);
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
	}

	private static List<Optional<Category>> categoryChoices() {
		List<Optional<Category>> choices = new ArrayList<>();
		choices.add(Optional.empty());
		Categories.ALL.forEach(category -> choices.add(Optional.of(category)));
		return choices;
	}

	private static Component categoryLabel(Optional<Category> category) {
		return category.map(Category::displayName)
				.orElseGet(() -> Component.translatable("container.echostorage.echo_chest.category.none"));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode != GLFW.GLFW_KEY_ESCAPE && nameField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				finishRenaming();
			} else {
				nameField.keyPressed(keyCode, scanCode, modifiers);
			}
			// Typing must not reach the inventory key, hotbar swaps or drop.
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (nameField.isFocused() && !nameField.isMouseOver(mouseX, mouseY)) {
			finishRenaming();
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** Sends before the close, so the server still has this chest open when the rename lands. */
	@Override
	public void onClose() {
		sendName();
		super.onClose();
	}

	private void finishRenaming() {
		nameField.setFocused(false);
		setFocused(null);
		sendName();
	}

	private void sendName() {
		String typed = nameField.getValue();
		if (!typed.equals(sentName)) {
			sentName = typed;
			NetClient.sendToServer(new RenameEchoChestPayload(menu.containerId, typed));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// The name field stands in for the title.
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, CHEST_HEIGHT);
		graphics.blit(TEXTURE, leftPos, topPos + CHEST_HEIGHT, 0, PLAYER_INVENTORY_V, imageWidth, PLAYER_INVENTORY_HEIGHT);
		// Under the items, so a stray is marked without being hidden.
		for (Slot slot : menu.slots.subList(0, EchoChestBlockEntity.SLOTS)) {
			if (slot.hasItem() && menu.isStray(slot.getItem())) {
				int x = leftPos + slot.x;
				int y = topPos + slot.y;
				graphics.fill(x, y, x + 16, y + 16, STRAY_TINT);
			}
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
