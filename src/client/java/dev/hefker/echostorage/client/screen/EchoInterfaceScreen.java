package dev.hefker.echostorage.client.screen;

import java.util.ArrayList;
import java.util.List;

import dev.hefker.echostorage.block.EchoChestBlockEntity;
import dev.hefker.echostorage.config.EchoConfig;
import dev.hefker.echostorage.link.LinkedChests;
import dev.hefker.echostorage.link.LinkedChests.Row;
import dev.hefker.echostorage.menu.EchoInterfaceMenu;
import dev.hefker.echostorage.network.DismissLinkedChestPayload;
import dev.hefker.echostorage.network.NetClient;
import dev.hefker.echostorage.network.OpenLinkedChestPayload;
import dev.hefker.echostorage.search.SearchQuery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * An open Echo Interface: one button per linked Echo Chest, which opens that chest. It lists
 * chests and never what is in them (ADR-0002).
 *
 * <p>A chest the Links no longer reach keeps a greyed row until the player dismisses it, since a
 * labelled chest silently dropping off the list is the problem the mod exists to solve. Its
 * tooltip says why it is greyed: in an unloaded chunk, or lost.
 *
 * <p>A row is labelled as its chest is — the typed name, else the Category in italics, else
 * "Echo Chest" — and the search box, unless the config turns it off, dims rows whose label does
 * not match. Those labels are names the player typed or chose, so searching them is allowed.
 */
public class EchoInterfaceScreen extends AbstractContainerScreen<EchoInterfaceMenu> {
	private static final int COLUMNS = 3;
	private static final int ROWS_PER_COLUMN = LinkedChests.MAX_ROWS / COLUMNS;
	private static final int PADDING = 8;
	private static final int TOP = 18;
	private static final int GAP = 2;
	private static final int ROW_WIDTH = 110;
	private static final int ROW_HEIGHT = 18;
	private static final int DISMISS_WIDTH = 14;
	private static final int FOOTER_HEIGHT = 20;
	private static final int FOOTER_WIDTH = 120;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_EDGE = 0xFF555555;
	private static final int LABEL_COLOR = 0x404040;
	private static final int SEARCH_MISS_DIM = 0xA0000000;

	private record RowButton(Row row, Button button) {
	}

	private final List<RowButton> rowButtons = new ArrayList<>();
	private final List<AbstractWidget> rowWidgets = new ArrayList<>();
	/** The rows the buttons were made from; the menu swaps in a new list when the server sends one. */
	private List<Row> shownRows;
	/** Null when the config turns search off. */
	private EditBox searchField;
	private SearchQuery query = SearchQuery.of("");
	private TypingFocus typing;

	public EchoInterfaceScreen(EchoInterfaceMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 2 * PADDING + COLUMNS * ROW_WIDTH + (COLUMNS - 1) * 2 * GAP;
		this.imageHeight = TOP + ROWS_PER_COLUMN * (ROW_HEIGHT + GAP) + GAP + FOOTER_HEIGHT + PADDING;
	}

	@Override
	protected void init() {
		super.init();
		String searched = searchField == null ? "" : searchField.getValue();
		typing = new TypingFocus(this);
		int footerY = topPos + TOP + ROWS_PER_COLUMN * (ROW_HEIGHT + GAP) + GAP;

		// The menu does nothing with this on the client; the player's inventory syncs back.
		addRenderableWidget(Button.builder(Component.translatable("container.echostorage.echo_interface.quick_stack"),
						button -> {
							menu.clickMenuButton(minecraft.player, EchoInterfaceMenu.QUICK_STACK_BUTTON);
							minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EchoInterfaceMenu.QUICK_STACK_BUTTON);
						})
				.tooltip(Tooltip.create(Component.translatable("container.echostorage.echo_interface.quick_stack.tooltip")))
				.bounds(leftPos + PADDING, footerY, FOOTER_WIDTH, FOOTER_HEIGHT)
				.build());

		searchField = null;
		if (EchoConfig.get().searchByChestName()) {
			Component searchHint = Component.translatable("container.echostorage.echo_interface.search");
			searchField = new EditBox(font, leftPos + imageWidth - PADDING - FOOTER_WIDTH, footerY, FOOTER_WIDTH, FOOTER_HEIGHT, searchHint);
			searchField.setHint(searchHint);
			searchField.setResponder(typed -> query = SearchQuery.of(typed));
			searchField.setValue(searched);
			addRenderableWidget(searchField);
			// Finishing leaves the query standing; only the keyboard is given back.
			typing.add(searchField, () -> {
			});
		}

		addRows();
	}

	/** Makes one button per row, and a dismiss button beside each greyed one. */
	private void addRows() {
		// init runs again on resize, after vanilla has already dropped every widget.
		rowWidgets.clear();
		rowButtons.clear();
		shownRows = menu.rows();
		for (int i = 0; i < shownRows.size(); i++) {
			Row row = shownRows.get(i);
			int x = leftPos + PADDING + i / ROWS_PER_COLUMN * (ROW_WIDTH + 2 * GAP);
			int y = topPos + TOP + i % ROWS_PER_COLUMN * (ROW_HEIGHT + GAP);
			boolean greyed = row.state() != LinkedChests.State.LINKED;
			Button button = Button.builder(label(row),
							pressed -> NetClient.sendToServer(new OpenLinkedChestPayload(menu.containerId, row.id())))
					.bounds(x, y, greyed ? ROW_WIDTH - DISMISS_WIDTH - GAP : ROW_WIDTH, ROW_HEIGHT)
					.build();
			button.active = !greyed;
			if (greyed) {
				button.setTooltip(Tooltip.create(Component.translatable(
						"container.echostorage.echo_interface." + row.state().getSerializedName())));
				rowWidgets.add(addRenderableWidget(Button.builder(Component.literal("×"),
								pressed -> NetClient.sendToServer(new DismissLinkedChestPayload(menu.containerId, row.id())))
						.tooltip(Tooltip.create(Component.translatable("container.echostorage.echo_interface.dismiss")))
						.bounds(x + ROW_WIDTH - DISMISS_WIDTH, y, DISMISS_WIDTH, ROW_HEIGHT)
						.build()));
			}
			rowWidgets.add(addRenderableWidget(button));
			rowButtons.add(new RowButton(row, button));
		}
	}

	/** What a row is called: the chest's typed name, else its Category in italics, else "Echo Chest". */
	static Component label(Row row) {
		return row.name().isEmpty() ? EchoChestBlockEntity.unnamedTitle(row.category()) : Component.literal(row.name());
	}

	/** Remakes the row buttons when the server sends new rows, leaving the search box and its focus alone. */
	@Override
	protected void containerTick() {
		super.containerTick();
		if (menu.rows() != shownRows) {
			rowWidgets.forEach(this::removeWidget);
			addRows();
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return typing.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		typing.mouseClicked(mouseX, mouseY);
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_EDGE);
		graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, PANEL);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// No player inventory here, so no inventory label.
		graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
		if (menu.rows().isEmpty()) {
			graphics.drawString(font, Component.translatable("container.echostorage.echo_interface.empty"),
					PADDING, TOP, LABEL_COLOR, false);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (!query.isBlank()) {
			for (RowButton shown : rowButtons) {
				if (!query.matches(label(shown.row()).getString())) {
					Button button = shown.button();
					graphics.fill(button.getX(), button.getY(), button.getRight(), button.getBottom(), SEARCH_MISS_DIM);
				}
			}
		}
		renderTooltip(graphics, mouseX, mouseY);
	}
}
