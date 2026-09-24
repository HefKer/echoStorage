package dev.hefker.echostorage.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Keeps a container screen's text boxes from fighting its key bindings. While a box has focus,
 * every key but Escape goes to it, so typing never reaches the inventory key, a hotbar swap or
 * drop. Enter, or a click anywhere else, finishes the box and hands the keyboard back — otherwise
 * the box keeps it and hotbar keys stop working on the slots.
 *
 * <p>Make a new one in each {@code init}, since the boxes are made again there.
 */
final class TypingFocus {
	private record Field(EditBox box, Runnable onFinish) {
	}

	private final Screen screen;
	private final List<Field> fields = new ArrayList<>();

	TypingFocus(Screen screen) {
		this.screen = screen;
	}

	/** Routes keys to {@code box} while it has focus; {@code onFinish} runs when it gives focus up. */
	void add(EditBox box, Runnable onFinish) {
		fields.add(new Field(box, onFinish));
	}

	/** Call first from the screen's {@code keyPressed}; true means the key was the box's. */
	boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			return false;
		}
		for (Field field : fields) {
			if (field.box().isFocused()) {
				if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
					finish(field);
				} else {
					field.box().keyPressed(keyCode, scanCode, modifiers);
				}
				return true;
			}
		}
		return false;
	}

	/** Call first from the screen's {@code mouseClicked}. */
	void mouseClicked(double mouseX, double mouseY) {
		for (Field field : fields) {
			if (field.box().isFocused() && !field.box().isMouseOver(mouseX, mouseY)) {
				finish(field);
			}
		}
	}

	private void finish(Field field) {
		field.box().setFocused(false);
		screen.setFocused(null);
		field.onFinish().run();
	}
}
