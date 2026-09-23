package dev.hefker.echostorage.client.screen;

import dev.hefker.echostorage.menu.EchoMenus;
import net.minecraft.client.gui.screens.MenuScreens;

/** Binds menus to their screens. Vanilla {@code MenuScreens}, so this ports unchanged. */
public final class EchoScreens {
	private EchoScreens() {
	}

	public static void register() {
		MenuScreens.register(EchoMenus.PROBE, ProbeScreen::new);
		MenuScreens.register(EchoMenus.ECHO_CHEST, EchoChestScreen::new);
	}
}
