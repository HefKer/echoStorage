package dev.hefker.echostorage.client.screen;

import java.util.Optional;

import dev.hefker.echostorage.category.Category;
import dev.hefker.echostorage.menu.EchoBundleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * An Echo Bundle's own screen, opened by sneak-using it: its Category and whether it picks up
 * matching items. Two buttons under the bundle's name, over the dimmed world; there are no slots.
 */
public class EchoBundleScreen extends AbstractContainerScreen<EchoBundleMenu> {
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int TITLE_HEIGHT = 12;
	private static final int TITLE_COLOR = 0xFFFFFF;

	private CycleButton<Optional<Category>> categoryButton;
	private CycleButton<Boolean> vacuumButton;

	public EchoBundleScreen(EchoBundleMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = BUTTON_WIDTH;
		this.imageHeight = TITLE_HEIGHT + 2 * BUTTON_HEIGHT + GAP;
	}

	@Override
	protected void init() {
		super.init();
		categoryButton = CycleButton.<Optional<Category>>builder(CategoryChoices::label)
				.withValues(CategoryChoices.all())
				.withInitialValue(menu.category())
				.withTooltip(choice -> Tooltip.create(Component.translatable("container.echostorage.echo_bundle.category.tooltip")))
				.create(leftPos, topPos + TITLE_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, CategoryChoices.BUTTON_LABEL,
						(button, choice) -> clickButton(choice.map(EchoBundleMenu::assignButton).orElse(EchoBundleMenu.CLEAR_CATEGORY_BUTTON)));
		addRenderableWidget(categoryButton);

		vacuumButton = CycleButton.onOffBuilder(menu.vacuums())
				.withTooltip(vacuum -> Tooltip.create(Component.translatable("container.echostorage.echo_bundle.vacuum.tooltip")))
				.create(leftPos, topPos + TITLE_HEIGHT + BUTTON_HEIGHT + GAP, BUTTON_WIDTH, BUTTON_HEIGHT,
						Component.translatable("container.echostorage.echo_bundle.vacuum"),
						(button, vacuum) -> clickButton(vacuum ? EchoBundleMenu.VACUUM_ON_BUTTON : EchoBundleMenu.VACUUM_OFF_BUTTON));
		addRenderableWidget(vacuumButton);
	}

	/** Follows the server: the menu opens before its data slots arrive, and they may disagree. */
	@Override
	protected void containerTick() {
		super.containerTick();
		if (!categoryButton.getValue().equals(menu.category())) {
			categoryButton.setValue(menu.category());
		}
		if (vacuumButton.getValue() != menu.vacuums()) {
			vacuumButton.setValue(menu.vacuums());
		}
	}

	/** Applies the click here at once and asks the server to agree, as the Echo Chest screen does. */
	private void clickButton(int button) {
		menu.clickMenuButton(minecraft.player, button);
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawCenteredString(font, title, imageWidth / 2, 0, TITLE_COLOR);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		// Nothing behind the buttons but the dimmed world.
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
