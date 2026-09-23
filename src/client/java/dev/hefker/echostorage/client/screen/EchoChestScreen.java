package dev.hefker.echostorage.client.screen;

import dev.hefker.echostorage.block.EchoBlocks;
import dev.hefker.echostorage.block.EchoChestName;
import dev.hefker.echostorage.menu.EchoChestMenu;
import dev.hefker.echostorage.network.NetClient;
import dev.hefker.echostorage.network.RenameEchoChestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/**
 * An open Echo Chest, drawn on the vanilla chest texture, whose title is a text field: click
 * the name, type, press enter. No anvil, no name tag.
 *
 * <p>The screen only reports what was typed. The server decides what the name becomes, so the
 * field may show untrimmed text until the chest is next opened.
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

	private EditBox nameField;
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
		nameField.setHint(EchoBlocks.ECHO_CHEST.getName());
		nameField.setTooltip(Tooltip.create(Component.translatable("container.echostorage.echo_chest.rename")));
		addRenderableWidget(nameField);
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
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
