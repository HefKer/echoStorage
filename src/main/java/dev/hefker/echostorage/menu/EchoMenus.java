package dev.hefker.echostorage.menu;

import dev.hefker.echostorage.EchoStorage;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu registration, and the only place that names a loader's extended-menu API.
 *
 * <p>Because open-data is a record plus a {@link net.minecraft.network.codec.StreamCodec}
 * (ADR-0003 rule 4), the adapter is this class and the {@code *MenuProvider} records — nothing else.
 * On NeoForge the same records go to {@code IMenuTypeExtension.create} with the codec reading
 * from the buffer, and no menu or screen changes. {@code PortabilityRulesTest} keeps the
 * loader's extended-menu API confined to this package.
 */
public final class EchoMenus {
	public static final MenuType<ProbeMenu> PROBE =
			new ExtendedScreenHandlerType<>(ProbeMenu::new, ProbeMenuData.STREAM_CODEC);
	public static final MenuType<EchoChestMenu> ECHO_CHEST =
			new ExtendedScreenHandlerType<>(EchoChestMenu::new, EchoChestMenuData.STREAM_CODEC);
	/** Vanilla's own menu type: the bundle screen has no open-data, so there is nothing to adapt. */
	public static final MenuType<EchoBundleMenu> ECHO_BUNDLE = new MenuType<>(EchoBundleMenu::new, FeatureFlags.VANILLA_SET);

	private EchoMenus() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.MENU, EchoStorage.id("probe"), PROBE);
		Registry.register(BuiltInRegistries.MENU, EchoStorage.id("echo_chest"), ECHO_CHEST);
		Registry.register(BuiltInRegistries.MENU, EchoStorage.id("echo_bundle"), ECHO_BUNDLE);
	}
}
