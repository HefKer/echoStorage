package dev.hefker.echostorage.client.tooltip;

import dev.hefker.echostorage.item.EchoBundleContents;
import dev.hefker.echostorage.item.SelectedItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.math.Fraction;

/**
 * Vanilla's {@code ClientBundleTooltip}, drawn over {@link EchoBundleTooltipLayout} so four
 * bundles' worth of entries cannot grow it off the screen. When capped, the last cell shows
 * how many entries were left out.
 *
 * <p>Where vanilla highlights the entry it would take out next, this highlights every shown entry
 * of the Selected item, or the hidden-count cell when the cap left all of them out.
 */
public final class ClientEchoBundleTooltip implements ClientTooltipComponent {
	private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/bundle/background");
	private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/bundle/slot");
	private static final ResourceLocation BLOCKED_SLOT = ResourceLocation.withDefaultNamespace("container/bundle/blocked_slot");
	private static final int SLOT_WIDTH = 18;
	private static final int SLOT_HEIGHT = 20;
	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int LABEL_WIDTH = SLOT_WIDTH - 2;

	private final EchoBundleContents contents;
	private final SelectedItem selected;
	private final EchoBundleTooltipLayout layout;
	private final boolean selectedHidden;

	public ClientEchoBundleTooltip(EchoBundleContents contents, SelectedItem selected) {
		this.contents = contents;
		this.selected = selected;
		this.layout = EchoBundleTooltipLayout.forEntries(contents.size());
		this.selectedHidden = !selected.isEmpty() && layout.capped() && !selectedShown();
	}

	private boolean selectedShown() {
		for (int index = 0; index < layout.shownEntries(); index++) {
			if (isSelected(index)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSelected(int index) {
		return selected.matches(contents.getItemUnsafe(index));
	}

	@Override
	public int getHeight() {
		return backgroundHeight() + 4;
	}

	@Override
	public int getWidth(Font font) {
		return backgroundWidth();
	}

	private int backgroundWidth() {
		return layout.columns() * SLOT_WIDTH + 2;
	}

	private int backgroundHeight() {
		return layout.rows() * SLOT_HEIGHT + 2;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
		graphics.blitSprite(BACKGROUND, x, y, backgroundWidth(), backgroundHeight());
		boolean full = contents.weight().compareTo(Fraction.ONE) >= 0;
		int index = 0;

		for (int row = 0; row < layout.rows(); row++) {
			for (int column = 0; column < layout.columns(); column++) {
				renderSlot(x + column * SLOT_WIDTH + 1, y + row * SLOT_HEIGHT + 1, index++, full, graphics, font);
			}
		}
	}

	private void renderSlot(int x, int y, int index, boolean full, GuiGraphics graphics, Font font) {
		if (index < layout.shownEntries()) {
			ItemStack stack = contents.getItemUnsafe(index);
			blit(graphics, x, y, SLOT);
			graphics.renderItem(stack, x + 1, y + 1, index);
			graphics.renderItemDecorations(font, stack, x + 1, y + 1);
			if (isSelected(index)) {
				AbstractContainerScreen.renderSlotHighlight(graphics, x + 1, y + 1, 0);
			}
		} else if (index == layout.shownEntries() && layout.capped()) {
			blit(graphics, x, y, SLOT);
			renderHiddenCount(x, y, graphics, font);
			if (selectedHidden) {
				AbstractContainerScreen.renderSlotHighlight(graphics, x + 1, y + 1, 0);
			}
		} else {
			blit(graphics, x, y, full ? BLOCKED_SLOT : SLOT);
		}
	}

	/** "+185" is wider than a cell, so the label shrinks to fit rather than spill over. */
	private void renderHiddenCount(int x, int y, GuiGraphics graphics, Font font) {
		String label = "+" + layout.hiddenEntries();
		float scale = Math.min(1F, (float) LABEL_WIDTH / font.width(label));
		graphics.pose().pushPose();
		graphics.pose().translate(x + SLOT_WIDTH / 2F, y + SLOT_HEIGHT / 2F, 0);
		graphics.pose().scale(scale, scale, 1F);
		graphics.drawString(font, label, -font.width(label) / 2, -font.lineHeight / 2, TEXT_COLOR);
		graphics.pose().popPose();
	}

	private static void blit(GuiGraphics graphics, int x, int y, ResourceLocation sprite) {
		graphics.blitSprite(sprite, x, y, 0, SLOT_WIDTH, SLOT_HEIGHT);
	}
}
