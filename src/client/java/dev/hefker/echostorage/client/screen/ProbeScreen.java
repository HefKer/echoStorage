package dev.hefker.echostorage.client.screen;

import dev.hefker.echostorage.menu.ProbeMenu;
import dev.hefker.echostorage.menu.ProbeMenuData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screen for {@link ProbeMenu}, drawn on the vanilla chest texture.
 *
 * <p>It reads its height from the menu's {@link ProbeMenuData} rather than from a buffer it
 * has to parse itself — the payoff of ADR-0003 rule 4 on the client side.
 */
public class ProbeScreen extends AbstractContainerScreen<ProbeMenu> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int TEXTURE_WIDTH = 256;
	private static final int TEXTURE_HEIGHT = 256;
	/** Where the player-inventory strip starts in the vanilla texture. */
	private static final int PLAYER_INVENTORY_V = 126;
	private static final int PLAYER_INVENTORY_HEIGHT = 96;

	private final int rows;

	public ProbeScreen(ProbeMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.rows = menu.data().rows();
		this.imageHeight = 114 + rows * 18;
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		int chestHeight = rows * 18 + 17;

		graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, chestHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		graphics.blit(TEXTURE, x, y + chestHeight, 0, PLAYER_INVENTORY_V,
				imageWidth, PLAYER_INVENTORY_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
